package me.xethh.tools.jvault.cmds.file;

import me.xethh.tools.jvault.authserv.AuthServerClient;
import me.xethh.tools.jvault.cmds.deen.CredentialOwner;
import picocli.CommandLine;

import java.util.Optional;
import java.util.concurrent.Callable;

import static me.xethh.tools.jvault.cmds.deen.sub.Common.Out;

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

    @CommandLine.Option(names = {"--user"}, defaultValue = "", description = "The user`")
    private String user="";

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

            user = Optional.ofNullable(user)
                    .flatMap(t-> t.equalsIgnoreCase("")?Optional.empty():Optional.of(t))
                    .orElseThrow(()->new RuntimeException("user is entered"));
            return as.getCode(user);
        } else {
            return credential;
        }
    }
}
