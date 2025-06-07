package me.xethh.tools.jvault.authserv;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.time.SystemTimeProvider;
import me.xethh.libs.encryptDecryptLib.encryption.RsaEncryption;
import me.xethh.libs.encryptDecryptLib.op.deen.DeEnCryptor;
import me.xethh.tools.jvault.cmds.authserver.SimpleAuthServer;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import me.xethh.utils.dateManipulation.BaseTimeZone;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface AuthServerClient extends ConsoleOwner {
    String authServer();

    default Optional<String> protocolAbcA(HttpClient httpClient){

        HttpRequest req = null;
        try {
            final var url = authServer() + "/a";

            console().debug("requesting for public key: " + url);

            req = HttpRequest.newBuilder(new URI(url))
                    .GET()
                    .build();
            var response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            console().debug("response code: " + response.statusCode());
            console().debug("response body: " + response.body());

            return Optional.of(response.body());
            //var bs = Base64.getDecoder().decode(response.body());
            //return Optional.ofNullable(RsaEncryption.getPublicKey(bs));
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            console().printStackTrace(e);
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    default Optional<String> protocolAbcB(HttpClient client, ObjectMapper om, PublicKey key, KeyPair kp, String user){
        var deClient = (DeEnCryptor)DeEnCryptor.instance(kp.getPublic(), kp.getPrivate());
        try {
            if (console().isDebugging()) {
                final var timePeriod = 30;
                final var timeProvider = new SystemTimeProvider();
                final var algo = HashingAlgorithm.SHA512;
                final var codeDigit = 8;
                final var debugging_sc = "S3XARBLC62OBDS4MWZVLIJREKFP3554P";
                console().debug("Debugging with testing secret: " + debugging_sc);
                final var codeGen = new DefaultCodeGenerator(algo, codeDigit);
                final var codeNow = codeGen.generate(debugging_sc, Math.floorDiv(timeProvider.getTime(), timePeriod));
                console().debug("Debugging totp: " + codeNow);
            }
            Scanner scanner = new Scanner(System.in);
            console().log("Please enter the totp: ");
            var codeInput = scanner.nextLine();
            console().log("Input code: " + codeInput);
            var req = new SimpleAuthServer.Request();
            req.setExpiresInM(30);
            req.setCode(codeInput);
            req.setKey(user);
            req.setDate(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(BaseTimeZone.Asia_Hong_Kong.timeZone().toZoneId())));

            final var bodyToPush = om.writeValueAsString(req);
            console().debug("body to push: " + bodyToPush);
            final var body = Base64.getEncoder().encodeToString(deClient.encryptToJsonContainer(key, bodyToPush).getBytes(StandardCharsets.UTF_8));
            console().debug("body: " + body);
            final var sender = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            console().debug("sender: " + sender);

            final var url = authServer() + "/b";
            console().debug("requesting for temp cert: " + url);

            var response = client.send(HttpRequest.newBuilder(new URI(url))
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .header("sender", sender)
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            if (console().isDebugging()) {
                console().debug("response code: " + response.statusCode());
                final var respBody = response.body();
                console().debug("response body: " + respBody);
                return Optional.ofNullable(respBody);
            } else {
                return Optional.ofNullable(response.body());
            }
        } catch (InterruptedException e) {
            console().printStackTrace(e);
            Thread.currentThread().interrupt();
        } catch (IOException | URISyntaxException | CodeGenerationException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();

    };

    default Optional<String> protocolAbcC(HttpClient httpClient,Path path, PublicKey key, KeyPair kp){
        var deClient = (DeEnCryptor)DeEnCryptor.instance(kp.getPublic(), kp.getPrivate());
        try {
            if (!path.toFile().exists()) {
                console().debug("j-vault credential not exists ");
                return Optional.empty();
            }

            var tempCert = new String(new FileInputStream(path.toFile()).readAllBytes(), StandardCharsets.UTF_8);

            console().debug("temp cert: " + tempCert);

            var sender = Base64.getEncoder().encodeToString(key.getEncoded());
            console().debug("sender: " + sender);
            var body = Base64.getEncoder().encodeToString(deClient.encryptToJsonContainer(key, tempCert).getBytes(StandardCharsets.UTF_8));
            console().debug("body: " + body);
            var req = HttpRequest.newBuilder(new URI(authServer() + "/c"))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .header("sender", sender)
                    .build();
            final var response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            console().debug("response status: " + response.statusCode());
            console().debug("response body: " + response.body());
            if (response.statusCode() != 200) {
                return Optional.empty();
            } else {
                final var responseText = new String(Base64.getDecoder().decode(response.body()), StandardCharsets.UTF_8);
                final var data = deClient.decryptJsonContainer(key, responseText).get();
                return Optional.of(data);
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            console().printStackTrace(e);
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    default String getCode(String user) {
        try {
            var client = HttpClient.newHttpClient();
            var om = new ObjectMapper();
            var kp = RsaEncryption.keyPair();
            //var deClient = (DeEnCryptor)DeEnCryptor.instance(kp.getPublic(), kp.getPrivate());
            var dir = System.getProperty("user.home");
            var path = Path.of(dir).resolve(".j-vault-c");

            console().debug("User home: " + path);
            console().debug("Path of j-vault credential: " + path);

            //Function<HttpClient, Optional<PublicKey>> getPubKey = (httpClient) -> {
            //    HttpRequest req = null;
            //    try {
            //        final var url = authServer() + "/a";
            //
            //        console().debug("requesting for public key: " + url);
            //
            //        req = HttpRequest.newBuilder(new URI(url))
            //                .GET()
            //                .build();
            //        var response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            //
            //        console().debug("response code: " + response.statusCode());
            //        console().debug("response body: " + response.body());
            //
            //        var bs = Base64.getDecoder().decode(response.body());
            //        return Optional.ofNullable(RsaEncryption.getPublicKey(bs));
            //    } catch (URISyntaxException | IOException e) {
            //        throw new RuntimeException(e);
            //    } catch (InterruptedException e) {
            //        console().printStackTrace(e);
            //        Thread.currentThread().interrupt();
            //    }
            //    return Optional.empty();
            //};
            //BiFunction<HttpClient, PublicKey, Optional<String>> getCert = (httpClient, key) -> {
            //    try {
            //        if (console().isDebugging()) {
            //            final var timePeriod = 30;
            //            final var timeProvider = new SystemTimeProvider();
            //            final var algo = HashingAlgorithm.SHA512;
            //            final var codeDigit = 8;
            //            final var debugging_sc = "S3XARBLC62OBDS4MWZVLIJREKFP3554P";
            //            console().debug("Debugging with testing secret: " + debugging_sc);
            //            final var codeGen = new DefaultCodeGenerator(algo, codeDigit);
            //            final var codeNow = codeGen.generate(debugging_sc, Math.floorDiv(timeProvider.getTime(), timePeriod));
            //            console().debug("Debugging totp: " + codeNow);
            //        }
            //        Scanner scanner = new Scanner(System.in);
            //        console().log("Please enter the totp: ");
            //        var codeInput = scanner.nextLine();
            //        console().log("Input code: " + codeInput);
            //        var req = new SimpleAuthServer.Request();
            //        req.setExpiresInM(30);
            //        req.setCode(codeInput);
            //        req.setKey(user);
            //        req.setDate(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(BaseTimeZone.Asia_Hong_Kong.timeZone().toZoneId())));
            //
            //        final var bodyToPush = om.writeValueAsString(req);
            //        console().debug("body to push: " + bodyToPush);
            //        final var body = Base64.getEncoder().encodeToString(deClient.encryptToJsonContainer(key, bodyToPush).getBytes(StandardCharsets.UTF_8));
            //        console().debug("body: " + body);
            //        final var sender = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            //        console().debug("sender: " + sender);
            //
            //        final var url = authServer() + "/b";
            //        console().debug("requesting for temp cert: " + url);
            //
            //        var response = client.send(HttpRequest.newBuilder(new URI(url))
            //                        .POST(HttpRequest.BodyPublishers.ofString(body))
            //                        .header("sender", sender)
            //                        .build(),
            //                HttpResponse.BodyHandlers.ofString());
            //
            //        if (console().isDebugging()) {
            //            console().debug("response code: " + response.statusCode());
            //            final var respBody = response.body();
            //            console().debug("response body: " + respBody);
            //            return Optional.ofNullable(respBody);
            //        } else {
            //            return Optional.ofNullable(response.body());
            //        }
            //    } catch (InterruptedException e) {
            //        console().printStackTrace(e);
            //        Thread.currentThread().interrupt();
            //    } catch (IOException | URISyntaxException | CodeGenerationException e) {
            //        throw new RuntimeException(e);
            //    }
            //    return Optional.empty();
            //
            //};
            //BiFunction<HttpClient, PublicKey, Optional<String>> getCode = (httpClient, key) -> {
            //    try {
            //        if (!path.toFile().exists()) {
            //            console().debug("j-vault credential not exists ");
            //            return Optional.empty();
            //        }
            //
            //        var tempCert = new String(new FileInputStream(path.toFile()).readAllBytes(), StandardCharsets.UTF_8);
            //
            //        console().debug("temp cert: " + tempCert);
            //
            //        var sender = Base64.getEncoder().encodeToString(deClient.getPublicKey().getEncoded());
            //        console().debug("sender: " + sender);
            //        var body = Base64.getEncoder().encodeToString(deClient.encryptToJsonContainer(key, tempCert).getBytes(StandardCharsets.UTF_8));
            //        console().debug("body: " + body);
            //        var req = HttpRequest.newBuilder(new URI(authServer() + "/c"))
            //                .POST(HttpRequest.BodyPublishers.ofString(body))
            //                .header("sender", sender)
            //                .build();
            //        final var response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            //        console().debug("response status: " + response.statusCode());
            //        console().debug("response body: " + response.body());
            //        if (response.statusCode() != 200) {
            //            return Optional.empty();
            //        } else {
            //            final var responseText = new String(Base64.getDecoder().decode(response.body()), StandardCharsets.UTF_8);
            //            final var data = deClient.decryptJsonContainer(key, responseText).get();
            //            return Optional.of(data);
            //        }
            //    } catch (URISyntaxException | IOException e) {
            //        throw new RuntimeException(e);
            //    } catch (InterruptedException e) {
            //        console().printStackTrace(e);
            //        Thread.currentThread().interrupt();
            //    }
            //    return Optional.empty();
            //};

            // Logic starting
            Optional<PublicKey> pubKey = Optional.empty();
            if (!path.toFile().exists()) {
                console().debug("Creating new key: " + path);
                pubKey = protocolAbcA(client)
                        .map(res->{
                                var bs = Base64.getDecoder().decode(res);
                                return RsaEncryption.getPublicKey(bs);
                        })
                ;
                if (pubKey.isEmpty()) {
                    console().log("Fail to obtain public key: " + path);
                }
                console().debug("Public key obtained");
                var tempCert = protocolAbcB(client, om, pubKey.orElseThrow(), kp, user);
                console().debug("Cert generated: " + tempCert);
                var os = new FileOutputStream(path.toFile());
                os.write(tempCert.orElseThrow().getBytes());
                os.close();
            }
            if (pubKey.isEmpty()) {
                console().debug("Public key is not set yet ");
                pubKey = protocolAbcA(client)
                        .map(res->{
                            var bs = Base64.getDecoder().decode(res);
                            return RsaEncryption.getPublicKey(bs);
                        })
                ;
            }

            var code = protocolAbcC(client,path, pubKey.orElseThrow(),kp);
            if (code.isEmpty()) {
                console().debug("Code not ready, retry code obtaining process... ");
                if (path.toFile().exists()) {
                    console().debug("File already exists ");
                    final var pathDeleteResult = path.toFile().delete();
                    console().debug("Try delete existing j-vault credential: " + pathDeleteResult);
                }

                console().debug("Request for temp cert again");
                var tempCert = protocolAbcB(client, om, pubKey.orElseThrow(), kp, user);
                console().debug("Temp cert requested");
                var os = new FileOutputStream(path.toFile());
                os.write(tempCert.orElseThrow().getBytes());
                os.close();
                console().debug("Temp cert stored");
                var c = protocolAbcC(client, path, pubKey.orElseThrow(), kp);
                if (c.isEmpty()) {
                    throw new RuntimeException("Code obtaining failed");
                } else {
                    return c.get();
                }
            } else {
                console().debug("obtain code: " + code);
                return code.get();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
