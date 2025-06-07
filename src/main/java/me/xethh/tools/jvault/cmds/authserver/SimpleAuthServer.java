package me.xethh.tools.jvault.cmds.authserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import io.vavr.Tuple2;
import me.xethh.libs.encryptDecryptLib.encryption.RsaEncryption;
import me.xethh.libs.encryptDecryptLib.op.deen.DeEnCryptor;
import me.xethh.libs.encryptDecryptLib.op.deen.DeEnCryptorImpl;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import me.xethh.utils.dateManipulation.BaseTimeZone;
import picocli.CommandLine;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
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
import java.util.function.Consumer;
import java.util.function.Function;

@CommandLine.Command(
        name = "simple",
        description = "simple auth server"
)
public class SimpleAuthServer implements ConsoleOwner, Callable<Integer> {


    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
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
    private boolean useEnv;

    public static interface CustomHandler extends ConsoleOwner, HttpHandler {

        @Override
        default void handle(HttpExchange exchange) throws IOException {
            try {
                console().log("received request: " + exchange.getRequestURI());
                doProcessing(exchange);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


            public void doProcessing (HttpExchange exchange);
        }

    @Override
    public Integer call() throws Exception {
        console().log("Launching server with port: " + port);
        console().debug("Debugging enabled");

        if (useEnv) {
            secrete = Optional.ofNullable(System.getenv("J_VAULT_SECRET"))
                    .orElseThrow(() -> new RuntimeException("SECRET not set"));
            key = Optional.ofNullable(System.getenv("J_VAULT_KEY"))
                    .orElseThrow(() -> new RuntimeException("KEY not set"));
            value = Optional.ofNullable(System.getenv("J_VAULT_VALUE"))
                    .orElseThrow(() -> new RuntimeException("VALUE not set"));
            port = Optional.ofNullable(System.getenv("J_VAULT_PORT"))
                    .map(Integer::parseInt)
                    .orElseThrow(() -> new RuntimeException("PORT not set"));

            console().debug("KEY: " + key);
            console().debug("PORT: " + port);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);


        var timePeriod = 30;
        var timeProvider = new SystemTimeProvider();
        var codeGen = new DefaultCodeGenerator(HashingAlgorithm.SHA512, 8);
        var verifier = new DefaultCodeVerifier(codeGen, timeProvider);
        verifier.setTimePeriod(timePeriod);
        verifier.setAllowedTimePeriodDiscrepancy(2);

        KeyPair kp = RsaEncryption.keyPair();
        DeEnCryptorImpl deenServer = DeEnCryptor.instance(kp.getPublic(), kp.getPrivate());

        HandlerFactory handlerFactory = new HandlerFactory(kp);

        Consumer<HttpExchange> logRequest = (exchange) -> console().log("received request: " + exchange.getRequestURI());
        server.createContext("/a", handlerFactory.replyWithPubKey());

        Function<HttpExchange, Optional<PublicKey>> getSender = exchange -> {
            console().debug("getSender: " + exchange.getRequestURI());
            if (!exchange.getRequestMethod().equalsIgnoreCase("post")) {
                console().debug("[getSender] rejected due to not post request");
                return Optional.empty();
            }
            var sender = exchange.getRequestHeaders().get("sender");
            if (sender == null) {
                console().debug("[getSender] rejected due to sender is null");
                return Optional.empty();
            }
            if (sender.isEmpty()) {
                console().debug("[getSender] rejected due to sender is empty");
                return Optional.empty();
            }

            try {
                final var senderText = sender.get(0);
                console().debug("[getSender] sender: " + senderText);
                return Optional.of(RsaEncryption.getPublicKey(Base64.getDecoder().decode(senderText.getBytes())));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        BiFunction<HttpExchange, PublicKey, Optional<String>> decryptBody = (exchange, pubkey) -> {
            try {
                byte[] d = exchange.getRequestBody().readAllBytes();
                console().debug("[getSender] decrypted body: " + new String(d, StandardCharsets.UTF_8));
                String dd = new String(Base64.getDecoder().decode(d), StandardCharsets.UTF_8);
                if (dd.isEmpty()) {
                    console().debug("[decryptBody] decrypted body is empty");
                    return Optional.empty();
                }
                return deenServer.decryptJsonContainer(pubkey, dd);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        server.createContext("/b", handlerFactory.handleEncryptedData(Request.class, (sender, myKeypair, req) -> {

            var now = Instant.now();
            var upper = now.plus(1, ChronoUnit.MINUTES);
            var lower = now.plus(-1, ChronoUnit.MINUTES);
            var dateSent = Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(req.getDate()));
            if (dateSent.isBefore(lower) || dateSent.isAfter(upper)) {
                console().debug("rejected due to invalid datetime");
                throw new RuntimeException();
            }

            if (!verifier.isValidCode(secrete, req.getCode())) {
                console().debug("secret: " + secrete);
                console().debug("code: " + req.getCode());
                console().debug("expect code: " + codeGen.generate(secrete, Math.floorDiv(timeProvider.getTime(), timePeriod)));
                console().debug("rejected due to invalid code");
                throw new RuntimeException();
            }

            if (req.getExpiresInM() <= 0 || req.getExpiresInM() > 43200) {
                console().debug("rejected due to invalid expires period");
                throw new RuntimeException();
            }

            var tempCert = new TempCert();
            tempCert.setExpiresInM(req.getExpiresInM());
            tempCert.setDate(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(BaseTimeZone.Asia_Hong_Kong.timeZone().toZoneId())));
            tempCert.setTo(Base64.getEncoder().encodeToString(sender.getEncoded()));
            return new Tuple2(tempCert, true);
        }));

        server.createContext("/c", handlerFactory.handleEncryptedData(String.class, (sender, myKeypair, certString) -> {
            final var cert = new ObjectMapper().readValue(
                    DeEnCryptor.instance(myKeypair.getPublic(), myKeypair.getPrivate())
                    .decryptJsonContainer(
                            myKeypair.getPublic(),
                            new String(Base64.getDecoder().decode(certString.getBytes(StandardCharsets.UTF_8)))
                    ).orElseThrow(), TempCert.class);
            // Test timing
            var now = Instant.now();
            var dateCert = Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(cert.getDate())).atZone(BaseTimeZone.Asia_Hong_Kong.timeZone().toZoneId());
            var timeLimit = dateCert.plusMinutes(cert.getExpiresInM());
            console().debug("time now: " + now);
            console().debug("time expire: " + timeLimit);
            if (!now.isBefore(timeLimit.toInstant())) {
                console().debug("Cert expired");
                throw new RuntimeException();
                }

            console().debug("All validation passed, releasing secret value");
            return new Tuple2(value, false);
        }));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        server.setExecutor(executor);
        server.start();
        console().log(String.format("[Auth Server Started - %d] %nenter to exit: %n", port));
        final var authServerPath = Path.of(".shutdown-auth-server");
        if (!authServerPath.toFile().exists()) {
            try (
                    final var fos = new FileOutputStream(authServerPath.toFile());
            ) {
                fos.write("hello world".getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                console().printStackTrace(e);
                System.exit(1);
            }
        }
        while (authServerPath.toFile().exists()) {
            if (!authServerPath.toFile().exists()) {
                break;
            }
            Thread.sleep(Duration.ofMinutes(60).toMillis());
        }
        return 0;
    }


    public static class Request {
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

    public static class TempCert {
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
}
