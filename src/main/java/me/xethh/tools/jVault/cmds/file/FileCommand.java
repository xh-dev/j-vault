package me.xethh.tools.jVault.cmds.file;

import me.xethh.tools.jVault.cmds.deen.CredentialOwner;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static me.xethh.tools.jVault.cmds.deen.sub.Common.Out;

@CommandLine.Command(
        name = "file",
        subcommands = {EncryptFile.class, DecryptFile.class},
        description = "file encrypt and decrypt with j-vault"
)
public class FileCommand implements Callable<Integer>, CredentialOwner {
    @CommandLine.Option(names = {"-c","--credential"}, defaultValue = "", description = "The credential to use, if missing, would try find env variable `x-credential` or `x_credential`")
    private String credential;

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, Out.get());
        return 0;
    }

    @Override
    public String getCredential() {
        return credential;
    }
}
