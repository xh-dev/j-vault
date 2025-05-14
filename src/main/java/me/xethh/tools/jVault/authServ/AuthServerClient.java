package me.xethh.tools.jVault.authServ;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.time.SystemTimeProvider;
import me.xethh.libs.encryptDecryptLib.encryption.RsaEncryption;
import me.xethh.libs.encryptDecryptLib.op.deen.DeEnCryptor;
import me.xethh.tools.jVault.cmds.deen.sub.AuthServer;
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
    default String getCode(){
        try{
            var client = HttpClient.newHttpClient();
            var om = new ObjectMapper();
            var kp = RsaEncryption.keyPair();
            var deClient = DeEnCryptor.instance(kp.getPublic(), kp.getPrivate());
            var dir =System.getProperty("user.home");
            var path= Path.of(dir).resolve(".vault-c");

            Function<HttpClient, PublicKey> getPubKey = (httpClient)->{
                HttpRequest req = null;
                try {
                    req = HttpRequest.newBuilder(new URI(authServer()+"/a"))
                            .GET()
                            .build();
                    var response=httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                    //return om.readValue(Base64.getDecoder().decode(response.body()), AuthServer.TempCert.class);
                    var bs = Base64.getDecoder().decode(response.body());
                    return RsaEncryption.getPublicKey(bs);
                } catch (URISyntaxException | IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

            };
            BiFunction<HttpClient, PublicKey, String> getCert = (httpClient, key) -> {
                try{
                    String codeNow="";
                    var timePeriod=30;
                    var timeProvider = new SystemTimeProvider();
                    var codeGen=new DefaultCodeGenerator(HashingAlgorithm.SHA512, 8);
                    codeNow=codeGen.generate("S3XARBLC62OBDS4MWZVLIJREKFP3554P", Math.floorDiv(timeProvider.getTime(),timePeriod));
                    System.out.println(codeNow);
                    Scanner scanner = new Scanner(System.in);
                    System.out.println("Please enter the totp: ");
                    var codeInput = scanner.nextLine();
                    var req = new AuthServer.Request();
                    req.setExpiresInM(30);
                    req.setCode(codeInput);
                    req.setKey("xeth");
                    req.setDate(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(BaseTimeZone.Asia_Hong_Kong.timeZone().toZoneId())));

                    var body=Base64.getEncoder().encodeToString(deClient.encryptToJsonContainer(key, om.writeValueAsString(req)).getBytes(StandardCharsets.UTF_8));
                    var sender = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());

                    var response = client.send(HttpRequest.newBuilder(new URI(authServer()+"/b"))
                                    .POST(HttpRequest.BodyPublishers.ofString(body))
                                    .header("sender", sender)
                                    .build(),
                            HttpResponse.BodyHandlers.ofString()).body();
                    ;
                    return response;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
            BiFunction<HttpClient, PublicKey, Optional<String>> getCode = (httpClient, key) -> {
                try {
                    var tempCert = new String(new FileInputStream(path.toFile()).readAllBytes(), StandardCharsets.UTF_8);
                    var sender = Base64.getEncoder().encodeToString(deClient.getPublicKey().getEncoded());
                    var body = Base64.getEncoder().encodeToString(deClient.encryptToJsonContainer(key, tempCert).getBytes(StandardCharsets.UTF_8));
                    var req = HttpRequest.newBuilder(new URI(authServer()+"/c"))
                            .POST(HttpRequest.BodyPublishers.ofString(body))
                            .header("sender", sender)
                            .build();
                    var response = new String(Base64.getDecoder().decode(httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body()),StandardCharsets.UTF_8);
                    if(response.equalsIgnoreCase("")){
                        return Optional.empty();
                    } else {
                        var data = deClient.decryptJsonContainer(key, response).get();
                        return Optional.of(data);
                    }
                } catch (URISyntaxException | IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

            };

            PublicKey pubKey=null;
            if(!path.toFile().exists()){
                pubKey=  getPubKey.apply(client);
                var tempCert= getCert.apply(client, pubKey);
                var os = new FileOutputStream(path.toFile());
                os.write(tempCert.getBytes());
                os.close();
            }
            if(pubKey == null){
                pubKey=  getPubKey.apply(client);
            }

            var code = getCode.apply(client, pubKey);
            if(code.isEmpty()){
                path.toFile().delete();
                var tempCert= getCert.apply(client, pubKey);
                var os = new FileOutputStream(path.toFile());
                os.write(tempCert.getBytes());
                os.close();
                var c = getCode.apply(client, pubKey);
                return c.get();
            } else {
                return code.get();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };
}
