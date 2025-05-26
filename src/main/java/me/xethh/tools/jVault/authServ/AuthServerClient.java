package me.xethh.tools.jVault.authServ;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import me.xethh.libs.encryptDecryptLib.encryption.RsaEncryption;
import me.xethh.libs.encryptDecryptLib.op.deen.DeEnCryptor;
import me.xethh.tools.jVault.cmds.deen.sub.SimpleAuthServer;
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
import java.security.PublicKey;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface AuthServerClient {
    String authServer();
    default String getCode(String user){
        final boolean debugging = Optional.ofNullable(System.getenv().get("DEV")).isPresent();
        try{
            var client = HttpClient.newHttpClient();
            var om = new ObjectMapper();
            var kp = RsaEncryption.keyPair();
            var deClient = DeEnCryptor.instance(kp.getPublic(), kp.getPrivate());
            var dir =System.getProperty("user.home");
            var path= Path.of(dir).resolve(".j-vault-c");

            if(debugging){
                System.out.println("User home: "+path);
                System.out.println("Path of j-vault credential: "+ path);
            }

            Function<HttpClient, PublicKey> getPubKey = (httpClient)->{
                HttpRequest req = null;
                try {
                    final var url=authServer()+"/a";
                    if(debugging){
                        System.out.println("requesting for public key: "+url);
                    }
                    req = HttpRequest.newBuilder(new URI(url))
                            .GET()
                            .build();
                    var response=httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    if(debugging){
                        System.out.println("response code: "+response.statusCode());
                        System.out.println("response body: "+response.body());
                    }
                    var bs = Base64.getDecoder().decode(response.body());
                    return RsaEncryption.getPublicKey(bs);
                } catch (URISyntaxException | IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

            };
            BiFunction<HttpClient, PublicKey, String> getCert = (httpClient, key) -> {
                try{
                    if(debugging){
                        final var timePeriod=30;
                        final var timeProvider = new SystemTimeProvider();
                        final var algo = HashingAlgorithm.SHA512;
                        final var codeDigit = 8;
                        final var debugging_sc = "S3XARBLC62OBDS4MWZVLIJREKFP3554P";
                        System.out.println("Debugging with testing secret: "+debugging_sc);
                        final var codeGen=new DefaultCodeGenerator(algo, codeDigit);
                        final var codeNow=codeGen.generate(debugging_sc, Math.floorDiv(timeProvider.getTime(),timePeriod));
                        System.out.println("Debugging totp: "+codeNow);
                    }
                    Scanner scanner = new Scanner(System.in);
                    System.out.println("Please enter the totp: ");
                    var codeInput = scanner.nextLine();
                    System.out.println("Input code: "+codeInput);
                    var req = new SimpleAuthServer.Request();
                    req.setExpiresInM(30);
                    req.setCode(codeInput);
                    req.setKey(user);
                    req.setDate(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(BaseTimeZone.Asia_Hong_Kong.timeZone().toZoneId())));

                    final var bodyToPush = om.writeValueAsString(req);
                    if(debugging){
                        System.out.println("body to push: "+bodyToPush);
                    }
                    final var body=Base64.getEncoder().encodeToString(deClient.encryptToJsonContainer(key, bodyToPush).getBytes(StandardCharsets.UTF_8));
                    if(debugging){
                        System.out.println("body: "+body);
                    }
                    final var sender = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
                    if(debugging){
                        System.out.println("sender: "+sender);
                    }

                    final var url = authServer()+"/b";
                    if(debugging){
                        System.out.println("requesting for temp cert: "+url);
                    }

                    var response = client.send(HttpRequest.newBuilder(new URI(url))
                                    .POST(HttpRequest.BodyPublishers.ofString(body))
                                    .header("sender", sender)
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
                    ;
                    if(debugging){
                        System.out.println("response code: "+response.statusCode());
                        final var respBody=response.body();
                        System.out.println("response body: "+respBody);
                        return respBody;
                    } else {
                        return response.body();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            BiFunction<HttpClient, PublicKey, Optional<String>> getCode = (httpClient, key) -> {
                try {
                    if(!path.toFile().exists()){
                        if(debugging){
                            System.out.println("j-vault credential not exists ");
                        }
                        return Optional.empty();
                    }

                    var tempCert = new String(new FileInputStream(path.toFile()).readAllBytes(), StandardCharsets.UTF_8);

                    if(debugging){
                        System.out.println("temp cert: "+tempCert);
                    }

                    var sender = Base64.getEncoder().encodeToString(deClient.getPublicKey().getEncoded());
                    if(debugging){
                        System.out.println("sender: "+sender);
                    }
                    var body = Base64.getEncoder().encodeToString(deClient.encryptToJsonContainer(key, tempCert).getBytes(StandardCharsets.UTF_8));
                    if(debugging){
                        System.out.println("body: "+body);
                    }
                    var req = HttpRequest.newBuilder(new URI(authServer()+"/c"))
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .header("sender", sender)
                            .build();
                    final var response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    if(debugging){
                        System.out.println("response status: "+response.statusCode());
                        System.out.println("response body: "+response.body());
                    }
                    if(response.statusCode()!=200){
                        return Optional.empty();
                    } else {
                        final var responseText = new String(Base64.getDecoder().decode(response.body()),StandardCharsets.UTF_8);
                        final var data = deClient.decryptJsonContainer(key, responseText).get();
                        return Optional.of(data);
                    }
                } catch (URISyntaxException | IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

            };

            // Logic starting
            PublicKey pubKey=null;
            if(!path.toFile().exists()){
                if(debugging){
                    System.out.println("Creating new key: "+path);
                }
                pubKey=  getPubKey.apply(client);
                if(debugging){
                    System.out.println("Public key obtained");
                }
                var tempCert= getCert.apply(client, pubKey);
                if(debugging){
                    System.out.println("Cert generated: "+tempCert);
                }
                var os = new FileOutputStream(path.toFile());
                os.write(tempCert.getBytes());
                os.close();
            }
            if(pubKey == null){
                if(debugging){
                    System.out.println("Public key is not set yet ");
                }
                pubKey=  getPubKey.apply(client);
            }

            var code = getCode.apply(client, pubKey);
            if(code.isEmpty()){
                if(debugging){
                    System.out.println("Code not ready, retry code obtaining process... ");
                }
                if(path.toFile().exists()){
                    if(debugging){
                        System.out.println("File already exists ");
                    }
                    final var pathDeleteResult = path.toFile().delete();
                    if(debugging){
                        System.out.println("Try delete existing j-vault credential: "+pathDeleteResult);
                    }
                }

                if(debugging){
                    System.out.println("Request for temp cert again");
                }
                var tempCert= getCert.apply(client, pubKey);
                if(debugging){
                    System.out.println("Temp cert requested");
                }
                var os = new FileOutputStream(path.toFile());
                os.write(tempCert.getBytes());
                os.close();
                if(debugging){
                    System.out.println("Temp cert stored");
                }
                var c = getCode.apply(client, pubKey);
                if(c.isEmpty()){
                    throw new RuntimeException("Code obtaining failed");
                } else {
                    return c.get();
                }
            } else {
                if(debugging){
                    System.out.println("obtain code: "+code);
                }
                return code.get();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };
}
