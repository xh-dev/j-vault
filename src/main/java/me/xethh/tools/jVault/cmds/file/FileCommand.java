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
            if(authServer.endsWith("/")){
                authServer = authServer.substring(0,authServer.length()-1);
            }
            var as = new AuthServerClient(){
                @Override
                public String authServer() {
                    return authServer;
                }
            };

            return as.getCode();
        } else {
            return credential;
        }
    }
}
