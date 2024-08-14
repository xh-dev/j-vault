package me.xethh.tools.jVault.cmds.openssl;

import picocli.CommandLine;

import java.util.concurrent.Callable;

import static me.xethh.tools.jVault.cmds.deen.sub.Common.Out;

@CommandLine.Command(
        name = "openssl",
        description = "openssl script",
        subcommands = {
                Openssl.Encrypt.class,
                Openssl.Decrypt.class
        }
)
public class Openssl implements Callable<Integer> {
    @CommandLine.Command(
            name="encrypt",
            description = "encryption script of using openssl to generate a file called kv-pass.enc"
    )
    public static class Encrypt implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            String cmd = String.format("openssl aes-256-cbc -a -salt -pbkdf2 -in kv-pass -out kv-pass.enc");
            Out.get().println(cmd);
            return 0;
        }
    }
    @CommandLine.Command(
            name="decrypt",
            description = "decrypt script of using openssl to generate a file called kv-pass.enc"
    )
    public static class Decrypt implements Callable<Integer> {
        @CommandLine.Option(names = {"--out-bash-env"}, required = false, description = "Output as bash export env", defaultValue = "false")
        private boolean outBash;

        @Override
        public Integer call() throws Exception {
            String cmd = "";
            if (outBash) {
                cmd = String.format("export x_credential=\"$(openssl aes-256-cbc -d -a -salt -pbkdf2 -in kv-pass.enc)\"");
            } else {
                cmd = String.format("openssl aes-256-cbc -d -a -salt -pbkdf2 -in kv-pass.enc");
            }
            Out.get().println(cmd);
            return 0;
        }
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, Out.get());
        return 0;
    }
}
