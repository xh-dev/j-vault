package me.xethh.tools.jvault.cmds.authserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import me.xethh.libs.encryptDecryptLib.encryption.RsaEncryption;
import me.xethh.libs.encryptDecryptLib.op.deen.DeEnCryptor;
import me.xethh.libs.encryptDecryptLib.op.deen.DeEnCryptorImpl;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.auth0.jwt.HeaderParams.CONTENT_TYPE;
import static me.xethh.tools.jvault.cmds.authserver.HandlerFactory.Const.TEXT_PLAIN;

public class HandlerFactory implements ConsoleOwner {
    public static class Const {
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String TEXT_PLAIN = "text/plain";
        public static final String APPLICATION_JSON = "application/json";
        private Const() {
            throw new IllegalStateException("Not expected to be executed!");
        }
    }

    Consumer<HttpExchange> logRequest = (exchange) -> console().log("received request: " + exchange.getRequestURI());
    public final KeyPair kp;
    public final DeEnCryptorImpl deenServer;

    public HandlerFactory(KeyPair kp) {
        this.kp = kp;
        this.deenServer = DeEnCryptor.instance(kp.getPublic(), kp.getPrivate());
    }

    public HttpHandler replyWithPubKey() {
        return exchange -> {
            logRequest.accept(exchange);
            var response = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            exchange.getResponseHeaders().set(CONTENT_TYPE, TEXT_PLAIN);
            exchange.sendResponseHeaders(200, response.length());
            var os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        };

    }
    
    public interface HandlingEncryptedData<T,R>{
        Tuple2<R, Boolean> process(PublicKey publicKey, KeyPair myKeyPair, T data) throws Exception;
    }
    
    public <T,R> HttpHandler handleEncryptedData(Class<T> clazz, HandlingEncryptedData<T,R> handlingEncryptedData) {
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
        
        return exchange -> {
            logRequest.accept(exchange);
            try {
                var pubKeyOpt = getSender.apply(exchange);
                if (pubKeyOpt.isEmpty()) {
                    console().debug("Cannot sender available");
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                    return;
                }

                PublicKey publicKey = pubKeyOpt.get();

                var dddOpt = decryptBody.apply(exchange, publicKey);
                if (dddOpt.isEmpty()) {
                    console().log("Cannot decrypt available");
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                    return;
                }

                var ddd = dddOpt.get();
                console().debug("decrypted body: " + ddd);
                final var processedResult = Try.of(()->handlingEncryptedData.process(
                        publicKey,
                        kp,
                        clazz.isAssignableFrom(String.class) ?
                                (T) ddd :
                                new ObjectMapper().readValue(ddd, clazz)));


                if(processedResult.isFailure()){
                    exchange.sendResponseHeaders(400, 0);
                    exchange.getResponseBody().close();
                }
                else{
                    final var ff = processedResult.get()._2?new ObjectMapper().writeValueAsString(processedResult.get()._1):(String)processedResult.get()._1;
                    console().debug("Temp cert to be issue: " + ff);
                    var fff = deenServer.encryptToJsonContainer(processedResult.get()._2?kp.getPublic():publicKey, ff);
                    var ffff = Base64.getEncoder().encodeToString(fff.getBytes(StandardCharsets.UTF_8));

                    console().debug("encrypted json: " + fff);

                    exchange.getResponseHeaders().set(Const.CONTENT_TYPE, TEXT_PLAIN);
                    exchange.sendResponseHeaders(200, ffff.length());
                    final var os = exchange.getResponseBody();
                    os.write(ffff.getBytes());
                    os.close();
                    console().debug(String.format("[%s] process completed%n", exchange.getRequestURI()));
                }
                
            } catch (Exception e) {
                console().printStackTrace(e);
                exchange.sendResponseHeaders(400, 0);
                exchange.getResponseBody().close();
            }
        };

    }

}
