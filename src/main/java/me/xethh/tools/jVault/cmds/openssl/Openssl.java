package me.xethh.tools.jVault.cmds.openssl;

import picocli.CommandLine;

import java.io.File;
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
    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested;
    @CommandLine.Command(
            name="encrypt",
            description = "encryption script of using openssl to generate a file called token file(default kv-pass.enc)"
    )
    public static class Encrypt implements Callable<Integer> {
        @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
        private boolean helpRequested;

        @CommandLine.Option(names = {"--token-file"}, required = false, description = "file name of token", defaultValue = "kv-pass")
        private File kvFile;

        @CommandLine.Option(names = {"--encoded-token-file"}, required = false, description = "file name of encoded token file", defaultValue = "kv-pass.enc")
        private File kvPass;

        @Override
        public Integer call() throws Exception {
            String cmd = String.format("openssl aes-256-cbc -a -salt -pbkdf2 -in %s -out %s", kvFile, kvPass);
            Out.get().println(cmd);
            return 0;
        }
    }
    @CommandLine.Command(
            name="decrypt",
            description = "decrypt script of using openssl to generate a file called token file (default kv-pass.enc)"
    )
    public static class Decrypt implements Callable<Integer> {
        @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
        private boolean helpRequested;
        @CommandLine.Option(names = {"--encoded-token-file"}, required = false, description = "file name of encoded token file", defaultValue = "kv-pass.enc")
        private File kvPass;

        @CommandLine.Option(names = {"--out-bash-env"}, required = false, description = "Output as bash export env", defaultValue = "false")
        private boolean outBash;

        @Override
        public Integer call() throws Exception {
            String cmd = "";
            if (outBash) {
                cmd = String.format("export x_credential=\"$(openssl aes-256-cbc -d -a -salt -pbkdf2 -in %s)\"", kvPass);
            } else {
                cmd = String.format("openssl aes-256-cbc -d -a -salt -pbkdf2 -in %s", kvPass);
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
