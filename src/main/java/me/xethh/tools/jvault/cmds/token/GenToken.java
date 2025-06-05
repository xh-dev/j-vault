package me.xethh.tools.jvault.cmds.token;

import me.xethh.tools.jvault.cmds.deen.DeenObj;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "gen",
        description = "generate token"

)
public class GenToken implements ConsoleOwner, Callable<Integer> {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    private boolean helpRequested;

    @CommandLine.Option(names = {"-p", "--password"}, description = "`password` to generate", defaultValue = "")
    private String password;

    @CommandLine.Option(names = {"--out-bash-env"}, required = false, description = "Output as bash export env", defaultValue = "false")
    private boolean outBash;

    @CommandLine.Option(names = {"--out-win-env"}, required = false, description = "Output as windows set env string", defaultValue = "false")
    private boolean outCmd;

    @CommandLine.Option(names = {"--as-kv-pass"}, required = false, description = "Output to filename token file name", defaultValue = "false")
    private boolean asKvPass;

    @CommandLine.Option(names = {"--token-file"}, required = false, description = "file name of token", defaultValue = "kv-pass")
    private File kvFile;

    @Override
    public Integer call() {
        if (password.isBlank()) {
            var rand = new SecureRandom();
            byte[] bs = new byte[16];
            rand.nextBytes(bs);
            password = Base64.getEncoder().encodeToString(bs);
        }
        String fPass = DeenObj.getFullPassword(password);
        if (outBash) {
            console().log(String.format("export x_credential=%s%n", fPass));
        } else if (outCmd) {
            console().log(String.format("set x-credential=%s%n", fPass));
        } else {
            if (asKvPass) {
                try (
                        FileOutputStream os = new FileOutputStream(kvFile);
                ) {
                    os.write(fPass.getBytes());
                } catch (Exception exception) {
                    console().printStackTrace(exception);
                }
            }
            console().log(fPass);
        }
        return 0;
    }

}
