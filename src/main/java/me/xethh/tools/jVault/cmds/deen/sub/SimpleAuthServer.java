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
import java.security.MessageDigest;
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

@CommandLine.Command(
        name = "auth-server",
        description = "auth-server"
)
public class SimpleAuthServer implements Callable<Integer> {

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

    @CommandLine.Option(names = {"--rate-limit"}, required = false, defaultValue = "30", description = "Max requests per minute per IP for /a, /b, /c (default: ${DEFAULT-VALUE})")
    private int rateLimitPerMinute = 30;

    @CommandLine.Option(names = {"--ip-behind-azure"}, required = false, description = "Resolve client IP from Azure routing headers (X-Azure-ClientIP / X-Forwarded-For)")
    private boolean ipBehindAzure = false;

    @CommandLine.Option(names = {"--ip-behind-cloudflare"}, required = false, description = "Resolve client IP from the Cloudflare CF-Connecting-IP header (can be combined with --ip-behind-azure)")
    private boolean ipBehindCloudflare = false;

    private final java.util.concurrent.ConcurrentHashMap<String, java.util.ArrayDeque<Long>> hits = new java.util.concurrent.ConcurrentHashMap<>();

    static String stripPort(String ip) {
        ip = ip.trim();
        if (ip.startsWith("[")) { // [v6]:port
            var end = ip.indexOf(']');
            return end > 0 ? ip.substring(1, end) : ip;
        }
        var idx = ip.indexOf(':');
        if (idx > 0 && idx == ip.lastIndexOf(':')) { // v4:port
            return ip.substring(0, idx);
        }
        return ip;
    }

    String resolveClientIp(HttpExchange exchange) {
        var headers = exchange.getRequestHeaders();
        if (ipBehindCloudflare) {
            var cf = headers.getFirst("CF-Connecting-IP");
            if (cf != null && !cf.isBlank()) {
                return stripPort(cf);
            }
        }
        if (ipBehindAzure) {
            var azure = headers.getFirst("X-Azure-ClientIP");
            if (azure != null && !azure.isBlank()) {
                return stripPort(azure);
            }
            var xff = headers.getFirst("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return stripPort(xff.split(",")[0]);
            }
        }
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    /** Logs the incoming IP and returns true if the request is within the per-minute limit. */
    boolean logAndCheckRate(HttpExchange exchange) {
        var ip = resolveClientIp(exchange);
        var now = System.currentTimeMillis();
        var window = hits.computeIfAbsent(ip, k -> new java.util.ArrayDeque<>());
        boolean allowed;
        synchronized (window) {
            while (!window.isEmpty() && now - window.peekFirst() >= 60_000) {
                window.pollFirst();
            }
            allowed = window.size() < rateLimitPerMinute;
            if (allowed) {
                window.addLast(now);
            }
        }
        System.out.printf("[request] ip=%s path=%s allowed=%b%n", ip, exchange.getRequestURI().getPath(), allowed);
        if (hits.size() > 10_000) {
            hits.entrySet().removeIf(e -> {
                synchronized (e.getValue()) {
                    return e.getValue().isEmpty() || now - e.getValue().peekLast() >= 60_000;
                }
            });
        }
        return allowed;
    }

    HttpHandler rateLimited(HttpHandler delegate) {
        return exchange -> {
            if (!logAndCheckRate(exchange)) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }
            delegate.handle(exchange);
        };
    }



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

        Function<PublicKey, String> hashKey = pk -> {
            try {
                var digest = java.security.MessageDigest.getInstance("SHA-256").digest(pk.getEncoded());
                var sb = new StringBuilder();
                for (byte b : digest) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        };

        server.createContext("/a", rateLimited(new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                System.out.println("received request: "+exchange.getRequestURI());
                var response = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
                exchange.getResponseHeaders().set("Content-Type", "text/plain");
                exchange.sendResponseHeaders(200, response.length());
                var os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }));

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
        server.createContext("/b", rateLimited(new HttpHandler() {
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

                    if(key == null ||  req.getKey()==null || MessageDigest.isEqual(
                            Optional.of(req.getKey()).get().getBytes(StandardCharsets.UTF_8),
                            Optional.of(key).get().getBytes(StandardCharsets.UTF_8)
                    )) {
                        if(debugging){
                            System.out.println("rejected due to invalid key");
                        }
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

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
                    tempCert.setTo(hashKey.apply(publicKey));

                    final var ff = new ObjectMapper().writeValueAsString(tempCert);
                    if(debugging){
                        System.out.println("Temp cert to be issue: "+ff);
                    }
                    var fff = deenServer.encryptToJsonContainer(kp.getPublic(), ff);
                    var ffff = Base64.getEncoder().encodeToString(fff.getBytes(StandardCharsets.UTF_8));

                    if(debugging){
                        System.out.println("encrypted json: "+fff);
                    }

                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
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
        }));

        server.createContext("/c", rateLimited(new HttpHandler() {

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

                    if(cert.getTo() == null || !java.security.MessageDigest.isEqual(
                            cert.getTo().getBytes(StandardCharsets.UTF_8),
                            hashKey.apply(publicKey).getBytes(StandardCharsets.UTF_8))){
                        if(debugging){
                            System.out.println("rejected due to cert not issued to sender");
                        }
                        exchange.sendResponseHeaders(400, 0);
                        exchange.getResponseBody().close();
                        return;
                    }

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

                    exchange.getResponseHeaders().set("Content-Type", "text/plain");
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
        }));
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
