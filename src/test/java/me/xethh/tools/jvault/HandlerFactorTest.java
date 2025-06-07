package me.xethh.tools.jvault;

import com.sun.net.httpserver.HttpServer;
import io.vavr.control.Try;
import me.xethh.libs.encryptDecryptLib.encryption.RSAFormatting;
import me.xethh.libs.encryptDecryptLib.encryption.RsaEncryption;
import me.xethh.tools.jvault.authserv.AuthServerClient;
import me.xethh.tools.jvault.authserv.SimpleAuthServerClient;
import me.xethh.tools.jvault.cmds.authserver.HandlerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyPair;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Test handler factory")
class HandlerFactorTest {
    @Test
    @DisplayName("When test protocol `abc`")
    void testProtocolAbc() {
        final var priKey = Try.of(() -> this.getClass().getClassLoader().getResourceAsStream("kps/1/pri.pem"))
                .mapTry(stream -> RSAFormatting.loadPemBytes(stream.readAllBytes()))
                .map(RsaEncryption::getPrivateKey)
                .get();

        final var pubKey = Try.of(() -> this.getClass().getClassLoader().getResourceAsStream("kps/1/pub.pem"))
                .mapTry(stream -> RSAFormatting.loadPemBytes(stream.readAllBytes()))
                .map(RsaEncryption::getPublicKey)
                .get();

        final var factor = new HandlerFactory(new KeyPair(pubKey, priKey));
        final int PORT = 7920;

        final var server = Try.of(() ->
                        HttpServer.create(new InetSocketAddress(PORT), 0)
                )
                .map(ser -> {
                    ExecutorService executor = Executors.newFixedThreadPool(2);
                    ser.setExecutor(executor);
                    ser.createContext("/a", factor.replyWithPubKey());
                    ser.start();
                    return ser;
                })
                .get();

        final var client = Try.of(() ->
                HttpClient.newBuilder().build()
        ).get();

        final var serverClient = new SimpleAuthServerClient(String.format("http://localhost:%d", PORT));

        final var testResult = serverClient.protocolAbcA(client);



        //final var testResult = Try.of(() -> HttpRequest.newBuilder(URI.create(String.format("http://localhost:%d/a", PORT))).GET().build())
        //        .mapTry(res ->
        //                client.send(res, HttpResponse.BodyHandlers.ofString())
        //        ).get();

        final var expectedResult = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAhWZeRftvS0dYhMtcSAMrfZWrob6Nr5fnIqa1iRuH5HrVF0q4ieTX+dlr4UroUonyWN1VWbO5S3jTkzYWEFGATg6AtS9rRZwq++R5GL0WncOr2Y2ruHN58rs8vYLbp0M8kzoEoCNXKy9BbluZPUh6wKdCS4SPevSmBnxV35y5OyH0vDxQ0H+3ocZvpuvU5IDDaDO6P6UYZeJ5sNxeW5WpVfmHAM3J1gJMNEtGMTqDHfu/RLsBHn7cJKnzd3MEU9Uu2kZX3nM6norFiXNWp1alrLUMRcSrmVYQdyG3Rv51bk7iXEsTMFs3XjmCw3/YPdk1QXWEu3tdxxYEk1lDyaSA5VBLHdRGAtMmcs8qIQI1vnLYwsVR7J1gKx6hI6DIef+QzGUwFVU7/pWWCcB53EEnZY2MB1AEfXxGAHcvYDCtNpMUcGY4WyLHCUQHMZ0Sfn1knZq45HT3Vu1rHuOlpJ3NKDESQwuTuT3cOhYu96oXj6QMD81UjCQaklB8CEep0CgClfv2hW1ETCGztc+Sg0BY6NIZQ1jUAy7W5WSKbquO9jMKJtOkpiFKFXaHlWnWhw/Wijf/H9ckXalPeqSYU9KHEzCSk3RIJLiCDT/1phiKF/IC00WYFSLCwt0qOrfgrDgD15FLb8BOsFl46jIZJnYfZaEvIvAoH3msHQlLz8Wy4nUCAwEAAQ==";
        assertTrue(testResult.isPresent());
        assertEquals(expectedResult, testResult.get(), "expecting return the base 64 encoded public key");
        server.stop(1);
    }

}
