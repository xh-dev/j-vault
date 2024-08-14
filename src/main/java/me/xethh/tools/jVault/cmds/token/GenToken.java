package me.xethh.tools.jVault.cmds.token;

import me.xethh.tools.jVault.cmds.deen.DeenObj;
import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Callable;

import static me.xethh.tools.jVault.cmds.deen.sub.Common.Out;

@CommandLine.Command(
        name = "gen",
        description = "generate token"

)
public class GenToken implements Callable<Integer> {
    @CommandLine.Option(names = {"-p","--password"}, description = "`password` to generate", defaultValue = "")
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
        if(password.isBlank()) {
            var rand = new SecureRandom();
            byte[] bs= new byte[16];
            rand.nextBytes(bs);
            password = Base64.getEncoder().encodeToString(bs);
        }
        if(outBash) {
            Out.get().println(String.format("export x_credential=%s", password));
        } else if (outCmd) {
            Out.get().println(String.format("set x-credential=%s", password));
        } else {
            if(asKvPass){
                try(
                        FileOutputStream os = new FileOutputStream(kvFile);
                        ) {
                    os.write(password.getBytes());
                } catch (Throwable throwable){
                    throwable.printStackTrace();
                }
            }
            Out.get().println(DeenObj.getFullPassword(password));
        }
        return 0;
    }

}
