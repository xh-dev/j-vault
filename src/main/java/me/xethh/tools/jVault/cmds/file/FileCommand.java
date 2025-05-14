package me.xethh.tools.jVault.cmds.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.CodeGenerationException;
import dev.samstevens.totp.time.SystemTimeProvider;
import me.xethh.libs.encryptDecryptLib.encryption.RSAFormatting;
import me.xethh.libs.encryptDecryptLib.encryption.RsaEncryption;
import me.xethh.libs.encryptDecryptLib.op.deen.DeEnCryptor;
import me.xethh.tools.jVault.authServ.AuthServerClient;
import me.xethh.tools.jVault.cmds.deen.CredentialOwner;
import me.xethh.tools.jVault.cmds.deen.sub.AuthServer;
import me.xethh.utils.dateManipulation.BaseTimeZone;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static me.xethh.tools.jVault.cmds.deen.sub.Common.Out;
import static org.bouncycastle.asn1.x509.ObjectDigestInfo.publicKey;

@CommandLine.Command(
        name = "file",
        subcommands = {EncryptFile.class, DecryptFile.class},
        description = "file encrypt and decrypt with j-vault"
)
public class FileCommand implements Callable<Integer>, CredentialOwner {
    @CommandLine.Option(names = {"-c","--credential"}, defaultValue = "", description = "The credential to use, if missing, would try find env variable `x-credential` or `x_credential`")
    private String credential;

    @CommandLine.Option(names = {"--auth-server"}, defaultValue = "", description = "The authentication server`")
    private String authServer;

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, Out.get());
        return 0;
    }

    @Override
    public String getCredential() {

        if(!authServer.equalsIgnoreCase("")) {
            var as = new AuthServerClient(){
                @Override
                public String authServer() {
                    return authServer;
                }
            };

            return as.getCode();
            //try{
            //    var client = HttpClient.newHttpClient();
            //    var om = new ObjectMapper();
            //    var kp = RsaEncryption.keyPair();
            //    var deClient = DeEnCryptor.instance(kp.getPublic(), kp.getPrivate());
            //    Function<HttpClient, PublicKey> getPubKey = (httpClient)->{
            //        HttpRequest req = null;
            //        try {
            //            req = HttpRequest.newBuilder(new URI(authServer+"/a"))
            //                    .GET()
            //                    .build();
            //            var response=httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            //            //return om.readValue(Base64.getDecoder().decode(response.body()), AuthServer.TempCert.class);
            //            var bs = Base64.getDecoder().decode(response.body());
            //            return RsaEncryption.getPublicKey(bs);
            //        } catch (URISyntaxException | IOException | InterruptedException e) {
            //            throw new RuntimeException(e);
            //        }
            //
            //    };
            //
            //    //String codeNow="";
            //    //var timePeriod=30;
            //    //var timeProvider = new SystemTimeProvider();
            //    //var codeGen=new DefaultCodeGenerator(HashingAlgorithm.SHA512, 8);
            //    //codeNow=codeGen.generate("S3XARBLC62OBDS4MWZVLIJREKFP3554P", Math.floorDiv(timeProvider.getTime(),timePeriod));
            //    //System.out.println(codeNow);
            //
            //
            //    //String finalCodeNow = codeNow;
            //
            //    Scanner scanner = new Scanner(System.in);
            //    System.out.println("Please enter the totp: ");
            //    var codeInput = scanner.nextLine();
            //
            //    BiFunction<HttpClient, PublicKey, String> getCert = (httpClient,key) -> {
            //        try{
            //            var req = new AuthServer.Request();
            //            req.setExpiresInM(30);
            //            req.setCode(codeInput);
            //            req.setKey("xeth");
            //            req.setDate(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(Instant.now().atZone(BaseTimeZone.Asia_Hong_Kong.timeZone().toZoneId())));
            //
            //            var body=Base64.getEncoder().encodeToString(deClient.encryptToJsonContainer(key, om.writeValueAsString(req)).getBytes(StandardCharsets.UTF_8));
            //            var sender = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
            //
            //            var response = client.send(HttpRequest.newBuilder(new URI(authServer+"/b"))
            //                            .POST(HttpRequest.BodyPublishers.ofString(body))
            //                            .header("sender", sender)
            //                            .build(),
            //                    HttpResponse.BodyHandlers.ofString()).body();
            //            ;
            //            return response;
            //        } catch (Exception e) {
            //            throw new RuntimeException(e);
            //        }
            //    };
            //
            //    var pubKey=  getPubKey.apply(client);
            //    var tempCert= getCert.apply(client, pubKey);
            //    BiFunction<HttpClient, PublicKey, String> getCode = (httpClient,key) -> {
            //        try {
            //            var sender = Base64.getEncoder().encodeToString(deClient.getPublicKey().getEncoded());
            //            var body = Base64.getEncoder().encodeToString(deClient.encryptToJsonContainer(key, tempCert).getBytes(StandardCharsets.UTF_8));
            //            var req = HttpRequest.newBuilder(new URI(authServer+"/c"))
            //                    .POST(HttpRequest.BodyPublishers.ofString(body))
            //                    .header("sender", sender)
            //                    .build();
            //            var response = new String(Base64.getDecoder().decode(httpClient.send(req, HttpResponse.BodyHandlers.ofString()).body()),StandardCharsets.UTF_8);
            //            var data = deClient.decryptJsonContainer(key, response).get();
            //            return data;
            //        } catch (URISyntaxException | IOException | InterruptedException e) {
            //            throw new RuntimeException(e);
            //        }
            //
            //    };
            //
            //    var code = getCode.apply(client, pubKey);
            //    return code;
            //} catch(Exception ex){
            //    ex.printStackTrace();
            //    throw new RuntimeException(ex);
            //}
        } else {
            return credential;
        }
    }
}
