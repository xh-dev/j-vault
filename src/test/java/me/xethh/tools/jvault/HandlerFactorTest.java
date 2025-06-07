package me.xethh.tools.jvault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import me.xethh.libs.encryptDecryptLib.encryption.RSAFormatting;
import me.xethh.libs.encryptDecryptLib.encryption.RsaEncryption;
import me.xethh.libs.encryptDecryptLib.op.deen.DeEnCryptor;
import me.xethh.tools.jvault.authserv.AuthServerClient;
import me.xethh.tools.jvault.authserv.SimpleAuthServerClient;
import me.xethh.tools.jvault.cmds.authserver.HandlerFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.security.KeyPair;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Test handler factory")
class HandlerFactorTest {
    @Test
    @DisplayName("When test protocol `abc`")
    void testProtocolAbc() throws CodeGenerationException {
        final var serverPriKey = Try.of(() -> this.getClass().getClassLoader().getResourceAsStream("kps/1/pri.pem"))
                .mapTry(stream -> RSAFormatting.loadPemBytes(stream.readAllBytes()))
                .map(RsaEncryption::getPrivateKey)
                .get();

        final var serverPubKey = Try.of(() -> this.getClass().getClassLoader().getResourceAsStream("kps/1/pub.pem"))
                .mapTry(stream -> RSAFormatting.loadPemBytes(stream.readAllBytes()))
                .map(RsaEncryption::getPublicKey)
                .get();

        final var clientPriKey = Try.of(() -> this.getClass().getClassLoader().getResourceAsStream("kps/2/pri.pem"))
                .mapTry(stream -> RSAFormatting.loadPemBytes(stream.readAllBytes()))
                .map(RsaEncryption::getPrivateKey)
                .get();

        final var clientPubKey = Try.of(() -> this.getClass().getClassLoader().getResourceAsStream("kps/2/pub.pem"))
                .mapTry(stream -> RSAFormatting.loadPemBytes(stream.readAllBytes()))
                .map(RsaEncryption::getPublicKey)
                .get();

        final var om = new ObjectMapper();

        final var factor = new HandlerFactory(new KeyPair(serverPubKey, serverPriKey));
        final int PORT = 7920;

        final var server = Try.of(() ->
                        HttpServer.create(new InetSocketAddress(PORT), 0)
                )
                .map(ser -> {
                    ExecutorService executor = Executors.newFixedThreadPool(2);
                    ser.setExecutor(executor);
                    ser.createContext("/a", factor.replyWithPubKey());
                    ser.createContext("/b", factor.handleEncryptedData(String.class, (x, y, z)-> new Tuple2<>("/b", false)));
                    ser.start();
                    return ser;
                })
                .get();

        final var client = Try.of(() ->
                HttpClient.newBuilder().build()
        ).get();

        final var serverClient = new SimpleAuthServerClient(String.format("http://localhost:%d", PORT));
        final var testResult = serverClient.protocolAbcA(client);
        final var expectedResult = "MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAhWZeRftvS0dYhMtcSAMrfZWrob6Nr5fnIqa1iRuH5HrVF0q4ieTX+dlr4UroUonyWN1VWbO5S3jTkzYWEFGATg6AtS9rRZwq++R5GL0WncOr2Y2ruHN58rs8vYLbp0M8kzoEoCNXKy9BbluZPUh6wKdCS4SPevSmBnxV35y5OyH0vDxQ0H+3ocZvpuvU5IDDaDO6P6UYZeJ5sNxeW5WpVfmHAM3J1gJMNEtGMTqDHfu/RLsBHn7cJKnzd3MEU9Uu2kZX3nM6norFiXNWp1alrLUMRcSrmVYQdyG3Rv51bk7iXEsTMFs3XjmCw3/YPdk1QXWEu3tdxxYEk1lDyaSA5VBLHdRGAtMmcs8qIQI1vnLYwsVR7J1gKx6hI6DIef+QzGUwFVU7/pWWCcB53EEnZY2MB1AEfXxGAHcvYDCtNpMUcGY4WyLHCUQHMZ0Sfn1knZq45HT3Vu1rHuOlpJ3NKDESQwuTuT3cOhYu96oXj6QMD81UjCQaklB8CEep0CgClfv2hW1ETCGztc+Sg0BY6NIZQ1jUAy7W5WSKbquO9jMKJtOkpiFKFXaHlWnWhw/Wijf/H9ckXalPeqSYU9KHEzCSk3RIJLiCDT/1phiKF/IC00WYFSLCwt0qOrfgrDgD15FLb8BOsFl46jIZJnYfZaEvIvAoH3msHQlLz8Wy4nUCAwEAAQ==";
        assertTrue(testResult.isPresent());
        assertEquals(expectedResult, testResult.get(), "expecting return the base 64 encoded public key");

        final var tempServerPubKey = testResult
                .map(res->{
                    var bs = Base64.getDecoder().decode(res);
                    return RsaEncryption.getPublicKey(bs);
                });

        final var codeNow = serverClient.nextCode(AuthServerClient.TestConst.ALGO, AuthServerClient.TestConst.CODE_DIGIT, AuthServerClient.TestConst.TEST_SECRET, AuthServerClient.TestConst.TIME_PERIOD);
        final var protocolAbcBTestResult = serverClient.protocolAbcB(client, om, tempServerPubKey.get(), new KeyPair(clientPubKey, clientPriKey), "test", codeNow);
        assertTrue(protocolAbcBTestResult.isPresent());

        Optional<String> data = DeEnCryptor.instance(clientPubKey, clientPriKey).decryptJsonContainer(
                serverPubKey,
                new String(Base64.getDecoder().decode(protocolAbcBTestResult.get()))
        );
        assertTrue(data.isPresent());
        server.stop(1);


    }

    //public static void main(String[] args) throws IOException {
    //    final var kp = RsaEncryption.keyPair();
    //    final var pri = Path.of("src/test/resources/kps/2/pri.pem");
    //    final var pub = Path.of("src/test/resources/kps/2/pub.pem");
    //
    //    pri.getParent().toFile().mkdirs();
    //    new FileOutputStream(pri.toFile())
    //            .write(RSAFormatting.toPem(kp.getPrivate()).getBytes(StandardCharsets.UTF_8));
    //    new FileOutputStream(pub.toFile())
    //            .write(RSAFormatting.toPem(kp.getPublic()).getBytes(StandardCharsets.UTF_8));
    //}

}
