package me.xethh.tools.jvault.cmds.deen.sub;

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
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;
import java.util.function.Function;

import static me.xethh.tools.jvault.cmds.deen.sub.SimpleAuthServer.Const.CONTENT_TYPE;
import static me.xethh.tools.jvault.cmds.deen.sub.SimpleAuthServer.Const.TEXT_PLAIN;

@CommandLine.Command(
        name = "auth-server",
        description = "auth-server"
)
public class SimpleAuthServer implements Callable<Integer> {

    public static class Const {
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String TEXT_PLAIN = "text/plain";
        public static final String APPLICATION_JSON = "application/json";
    }
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
    @CommandLine.Option(names = {"-k", "--key"}, required = false, description = "The key")
    private String key;

    @CommandLine.Option(names = {"-s", "--secret"}, required = false, description = "The secret")
    private String secrete;

    @CommandLine.Option(names = {"-v", "--value"}, required = false, description = "The value")
    private String value;

    @CommandLine.Option(names = {"--port"}, required = false, defaultValue = "8001", description = "The tcp port to open for the server")
    private int port;

    @CommandLine.Option(names = {"--use-env"}, required = false, arity = "1", description = "Enable to get config from ENV")
    private boolean useEnv = false;



    @Override
    public Integer call() throws Exception {
        final var debugging = Optional.ofNullable(System.getenv("DEV")).isPresent();
        System.out.println("Launching server with port: " + port);
        if(debugging){
            System.out.println("Debugging enabled");
        }

        if(useEnv) {
            secrete = Optional.ofNullable(System.getenv("J_VAULT_SECRET"))
                    .orElseThrow(()->new RuntimeException("SECRET not set"));
            key = Optional.ofNullable(System.getenv("J_VAULT_KEY"))
                    .orElseThrow(()->new RuntimeException("KEY not set"));
            value = Optional.ofNullable(System.getenv("J_VAULT_VALUE"))
                    .orElseThrow(()->new RuntimeException("VALUE not set"));
            port = Optional.ofNullable(System.getenv("J_VAULT_PORT"))
                    .map(Integer::parseInt)
                    .orElseThrow(()->new RuntimeException("PORT not set"));

            if(debugging){
                System.out.println("KEY: " + key);
                //System.out.println("VAULT: " + value);
                System.out.println("PORT: " + port);
            }
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);


        var timePeriod=30;
        var timeProvider = new SystemTimeProvider();
        var codeGen=new DefaultCodeGenerator(HashingAlgorithm.SHA512, 8);
        var verifier = new DefaultCodeVerifier(codeGen,timeProvider);
        verifier.setTimePeriod(timePeriod);
        verifier.setAllowedTimePeriodDiscrepancy(2);

        KeyPair kp = RsaEncryption.keyPair();

        DeEnCryptorImpl deenServer = DeEnCryptor.instance(kp.getPublic(), kp.getPrivate());

        server.createContext("/a", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                System.out.println("received request: "+exchange.getRequestURI());
                var response = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
                exchange.getResponseHeaders().set(CONTENT_TYPE, TEXT_PLAIN);
                exchange.sendResponseHeaders(200, response.length());
                var os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        });

        Function<HttpExchange, Optional<PublicKey>> getSender = exchange->{
            if(debugging){
                System.out.println("getSender: "+exchange.getRequestURI());
            }
            if(!exchange.getRequestMethod().equalsIgnoreCase("post")){
                if(debugging){
                    System.out.println("[getSender] rejected due to not post request");
                }
                return Optional.empty();
            }
            var sender = exchange.getRequestHeaders().get("sender");
            if(sender == null) {
                if(debugging){
                    System.out.println("[getSender] rejected due to sender is null");
                }
                return Optional.empty();
            }
            if(sender.isEmpty()){
                if(debugging){
                    System.out.println("[getSender] rejected due to sender is empty");
                }
                return Optional.empty();
            }

            try{
                final var senderText = sender.get(0);
                if(debugging){
                    System.out.println("[getSender] sender: "+senderText);
                }
                return Optional.of(RsaEncryption.getPublicKey(Base64.getDecoder().decode(senderText.getBytes())));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        BiFunction<HttpExchange, PublicKey, Optional<String>> decryptBody = (exchange,key) -> {
            try{
                byte[] d = exchange.getRequestBody().readAllBytes();
                if(debugging){
                    System.out.println("[getSender] decrypted body: "+new String(d, StandardCharsets.UTF_8));
                }
                String dd = new String(Base64.getDecoder().decode(d), StandardCharsets.UTF_8);
                if(dd.isEmpty()){
                    if(debugging){
                        System.out.println("[decryptBody] decrypted body is empty");
                    }
                    return Optional.empty();
                }
                var ddd=deenServer.decryptJsonContainer(key,dd);
                return ddd;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        server.createContext("/b", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                System.out.println("received request: "+exchange.getRequestURI());
                try{
                    var pubKeyOpt = getSender.apply(exchange);
                    if(pubKeyOpt.isEmpty()){
                        if(debugging){
                            System.out.println("Cannot sender available");
                        }
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    PublicKey publicKey = pubKeyOpt.get();

                    var dddOpt=decryptBody.apply(exchange,publicKey);
                    if(dddOpt.isEmpty()){
                        if(debugging){
                            System.out.println("Cannot decrypt available");
                        }
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    var ddd = dddOpt.get();
                    if(debugging){
                        System.out.println("decrypted body: "+ddd);
                    }
                    var os = exchange.getResponseBody();

                    var req = new ObjectMapper().readValue(ddd, Request.class);
                    // Test timing
                    var now=Instant.now();
                    var upper = now.plus(1, ChronoUnit.MINUTES);
                    var lower = now.plus(-1, ChronoUnit.MINUTES);
                    var dateSent=Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(req.getDate()));
                    if(dateSent.isBefore(lower) || dateSent.isAfter(upper)){
                        if(debugging){
                            System.out.println("rejected due to invalid datetime");
                        }
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    if(!verifier.isValidCode(secrete, req.getCode())){
                        if(debugging){
                            System.out.println("secret: "+secrete);
                            System.out.println("code: "+req.getCode());
                            System.out.println("expect code: "+codeGen.generate(secrete, Math.floorDiv(timeProvider.getTime(), timePeriod)));
                            System.out.println("rejected due to invalid code");
                        }
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    if(req.getExpiresInM()<=0 || req.getExpiresInM() > 43200){
                        if(debugging){
                            System.out.println("rejected due to invalid expires period");
                        }
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    var tempCert = new TempCert();
                    tempCert.setExpiresInM(req.getExpiresInM());
                    tempCert.setDate(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(BaseTimeZone.Asia_Hong_Kong.timeZone().toZoneId())));
                    tempCert.setTo(Base64.getEncoder().encodeToString(publicKey.getEncoded()));

                    final var ff = new ObjectMapper().writeValueAsString(tempCert);
                    if(debugging){
                        System.out.println("Temp cert to be issue: "+ff);
                    }
                    var fff = deenServer.encryptToJsonContainer(kp.getPublic(), ff);
                    var ffff = Base64.getEncoder().encodeToString(fff.getBytes(StandardCharsets.UTF_8));

                    if(debugging){
                        System.out.println("encrypted json: "+fff);
                    }

                    exchange.getResponseHeaders().set(CONTENT_TYPE, TEXT_PLAIN);
                    exchange.sendResponseHeaders(200, ffff.length());
                    os.write(ffff.getBytes());
                    os.close();
                    if(debugging){
                        System.out.println(String.format("[%s] process completed", exchange.getRequestURI()));
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                }
            }
        });

        server.createContext("/c", new HttpHandler() {

            @Override
            public void handle(HttpExchange exchange) throws IOException {
                System.out.println("received request: "+exchange.getRequestURI());
                var pubKeyOpt = getSender.apply(exchange);
                if(pubKeyOpt.isEmpty()){
                    if(debugging){
                        System.out.println("Cannot sender available");
                    }
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                    return;
                }
                PublicKey publicKey = pubKeyOpt.get();

                var dddOpt=decryptBody.apply(exchange,publicKey);
                if(dddOpt.isEmpty()){
                    if(debugging){
                        System.out.println("Cannot decrypt available");
                    }
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                    return;
                }

                var ddd = dddOpt.get();

                try{
                    var certStrOpt = deenServer.decryptJsonContainer(kp.getPublic(),new String(Base64.getDecoder().decode(ddd), StandardCharsets.UTF_8));
                    if(certStrOpt.isEmpty()){
                        if(debugging){
                            System.out.println("rejected due to fail decrypt or verify certificate");
                        }
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    if(debugging){
                        System.out.println("received cert: "+certStrOpt);
                    }
                    var cert = new ObjectMapper().readValue(certStrOpt.get(), TempCert.class);

                    // Test timing
                    var now=Instant.now();
                    var dateCert=Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(cert.getDate())).atZone(BaseTimeZone.Asia_Hong_Kong.timeZone().toZoneId());
                    var timeLimit = dateCert.plus(cert.getExpiresInM(), ChronoUnit.MINUTES);
                    if(debugging){
                        System.out.println("time now: "+now);
                        System.out.println("time expire: "+timeLimit);
                    }
                    if(!now.isBefore(timeLimit.toInstant())){
                        if(debugging){
                            System.out.println("Cert expired");
                        }
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

                    var os = exchange.getResponseBody();

                    if(debugging){
                        System.out.println("All validation passed, releasing secret value");
                    }
                    var fff = deenServer.encryptToJsonContainer(publicKey, value);
                    var ffff = Base64.getEncoder().encodeToString(fff.getBytes(StandardCharsets.UTF_8));

                    exchange.getResponseHeaders().set(CONTENT_TYPE, TEXT_PLAIN);
                    exchange.sendResponseHeaders(200, ffff.length());
                    os.write(ffff.getBytes());
                    os.close();
                    if(debugging){
                        System.out.println(String.format("[%s] process completed", exchange.getRequestURI()));
                    }
                }
                catch(Exception e){
                    e.printStackTrace();
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                }
            }
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        server.setExecutor(executor);
        server.start();
        System.out.printf("[Auth Server Started - %d] \nenter to exit: \n", port);
        var s=true;
        while(s){
            Thread.sleep(Duration.ofMinutes(60).toMillis());
            if(!s){
                break;
            }
        }
        //Scanner scanner = new Scanner(System.in);
        //scanner.nextLine();
        //System.out.println("\n[Auth Server Stopping...]");
        //server.stop(1);
        //System.out.println("[Auth Server Stopped]");
        //System.exit(0);
        return 0;
    }
}
