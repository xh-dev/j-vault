package me.xethh.tools.jVault.cmds.deen.sub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import me.xethh.libs.encryptDecryptLib.encryption.RsaEncryption;
import me.xethh.libs.encryptDecryptLib.op.deen.DeEnCryptor;
import me.xethh.libs.encryptDecryptLib.op.deen.DeEnCryptorImpl;
import me.xethh.utils.dateManipulation.BaseTimeZone;
import picocli.CommandLine;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CommandLine.Command(
        name = "auth-server",
        description = "auth-server"
)
public class AuthServer implements Callable<Integer> {

    public static class Request{
        private String key;
        private String code;
        private String date;
        private int expiresInM;

        public int getExpiresInM() {
            return expiresInM;
        }

        public void setExpiresInM(int expiresInM) {
            this.expiresInM = expiresInM;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }
    }

    public static class TempCert{
        private String date;
        private String to;
        private int expiresInM;

        public int getExpiresInM() {
            return expiresInM;
        }

        public void setExpiresInM(int expiresInM) {
            this.expiresInM = expiresInM;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }
    }

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested;
    @CommandLine.Option(names = {"-k", "--key"}, description = "The key")
    private String key;

    @CommandLine.Option(names = {"-s", "--secret"}, description = "The secret")
    private String secrete;

    @CommandLine.Option(names = {"-v", "--value"}, description = "The value")
    private String value;



    @Override
    public Integer call() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8001), 0);

        var timePeriod=30;
        var timeProvider = new SystemTimeProvider();
        var codeGen=new DefaultCodeGenerator(HashingAlgorithm.SHA512, 8);
        var codeNow=codeGen.generate(secrete, Math.floorDiv(timeProvider.getTime(),timePeriod));
        var verifier = new DefaultCodeVerifier(codeGen,timeProvider);
        verifier.setTimePeriod(timePeriod);
        verifier.setAllowedTimePeriodDiscrepancy(2);
        var result = verifier.isValidCode(secrete, codeNow);

        KeyPair kp = RsaEncryption.keyPair();
        KeyPair kp2 = RsaEncryption.keyPair();

        DeEnCryptorImpl deenClient = DeEnCryptor.instance(kp2.getPublic(), kp2.getPrivate());
        DeEnCryptorImpl deenServer = DeEnCryptor.instance(kp.getPublic(), kp.getPrivate());

        var senderKey = Base64.getEncoder().encodeToString(kp2.getPublic().getEncoded());

        var req = new Request();
        Instant now = Instant.now();
        req.setKey("xeth");
        req.setCode(codeNow);
        req.setExpiresInM(60);
        req.setDate(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now.atZone(BaseTimeZone.Asia_Hong_Kong.timeZone().toZoneId())));
        String data = Base64.getEncoder().encodeToString(deenClient.encryptToJsonContainer(kp.getPublic(),new ObjectMapper().writeValueAsString(req)).getBytes());

        System.out.println(String.format("curl -X POST -H \"sender: %s\" -d %s http://localhost:8001/b",senderKey, data));


        server.createContext("/a", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                var response = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.length());
                var os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        });

        server.createContext("/b", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if(!exchange.getRequestMethod().equalsIgnoreCase("post")){
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                var sender = exchange.getRequestHeaders().get("sender");
                if(sender == null) {
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                if(sender.isEmpty()){
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                    return;
                }

                try{
                    PublicKey publicKey = RsaEncryption.getPublicKey(Base64.getDecoder().decode(sender.get(0).getBytes()));
                    byte[] d = exchange.getRequestBody().readAllBytes();
                    String dd = new String(Base64.getDecoder().decode(d), StandardCharsets.UTF_8);
                    var ddd=deenServer.decryptJsonContainer(publicKey,dd);
                    if(dd.isEmpty()){
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }
                    var os = exchange.getResponseBody();
                    if(ddd.isEmpty()){
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    var req = new ObjectMapper().readValue(ddd.get(), Request.class);
                    // Test timing
                    var now=Instant.now();
                    var upper = now.plus(1, ChronoUnit.MINUTES);
                    var lower = now.plus(-1, ChronoUnit.MINUTES);
                    var dateSent=Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(req.getDate()));
                    if(dateSent.isBefore(lower) || dateSent.isAfter(upper)){
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    if(!verifier.isValidCode(secrete, req.getCode())){
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    if(req.getExpiresInM()<=0 || req.getExpiresInM() > 43200){
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    var tempCert = new TempCert();
                    tempCert.setExpiresInM(req.getExpiresInM());
                    tempCert.setDate(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(BaseTimeZone.Asia_Hong_Kong.timeZone().toZoneId())));
                    tempCert.setTo(sender.get(0));
                    var fff = deenServer.encryptToJsonContainer(kp.getPublic(), new ObjectMapper().writeValueAsString(tempCert));
                    var ffff = Base64.getEncoder().encodeToString(fff.getBytes(StandardCharsets.UTF_8));

                    System.out.println(String.format("\ncurl -X POST -H \"sender: %s\" -d %s http://localhost:8001/c",senderKey, Base64.getEncoder().encodeToString(deenClient.encryptToJsonContainer(kp.getPublic(), ffff).getBytes(StandardCharsets.UTF_8))));
                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(200, ffff.length());
                    os.write(ffff.getBytes());
                    os.close();
                }
                catch(Exception e){
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                    return;
                }
            }
        });

        server.createContext("/c", new HttpHandler() {

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if(!exchange.getRequestMethod().equalsIgnoreCase("post")){
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                var sender = exchange.getRequestHeaders().get("sender");
                if(sender == null) {
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                if(sender.isEmpty()){
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                    return;
                }

                try{
                    PublicKey publicKey = RsaEncryption.getPublicKey(Base64.getDecoder().decode(sender.get(0).getBytes()));
                    byte[] d = exchange.getRequestBody().readAllBytes();
                    String dd = new String(Base64.getDecoder().decode(d), StandardCharsets.UTF_8);

                    var ddd=deenServer.decryptJsonContainer(publicKey,dd);
                    if(ddd.isEmpty()){
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }
                    var certStrOpt = deenServer.decryptJsonContainer(kp.getPublic(),new String(Base64.getDecoder().decode(ddd.get()), StandardCharsets.UTF_8));
                    if(certStrOpt.isEmpty()){
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }
                    var cert = new ObjectMapper().readValue(certStrOpt.get(), TempCert.class);

                    // Test timing
                    var now=Instant.now();
                    var dateCert=Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(cert.getDate()));
                    var timeLimit = dateCert.plus(cert.getExpiresInM(), ChronoUnit.MINUTES);
                    if(!now.isBefore(timeLimit)){
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    var os = exchange.getResponseBody();

                    var fff = deenServer.encryptToJsonContainer(publicKey, value);
                    var ffff = Base64.getEncoder().encodeToString(fff.getBytes(StandardCharsets.UTF_8));

                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
                    exchange.sendResponseHeaders(200, ffff.length());
                    os.write(ffff.getBytes());
                    os.close();
                }
                catch(Exception e){
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                    return;
                }
            }
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        server.setExecutor(executor);
        server.start();
        Scanner scanner = new Scanner(System.in);
        System.out.print("[Auth Server Started] \nenter to exit: ");
        scanner.nextLine();
        return 0;
    }
}
