package me.xethh.tools.jVault.cmds.deen.sub;

import me.xethh.tools.jVault.cmds.deen.Vault;
import picocli.CommandLine;

import java.io.*;
import java.util.concurrent.Callable;

import static me.xethh.tools.jVault.cmds.deen.sub.Common.Out;
import static me.xethh.tools.jVault.cmds.deen.sub.Common.SkipFirstLine;

@CommandLine.Command(
        name = "unset",
        description = "unset a key value entry by key"
)
public class Unset implements Callable<Integer> {
    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested;
    @CommandLine.Option(names = {"-f", "--file"}, defaultValue = "vault.kv", description = "The file to encrypt")
    private File file;

    @CommandLine.Option(names = {"-k", "--key"}, description = "the key to remove", required = true)
    private String key;

    @CommandLine.ParentCommand
    private Vault deen;

    @Override
    public Integer call() throws Exception {
        var path = file.toPath().toAbsolutePath();
        var tmpPath = new File(path + ".tmp").toPath();

        var deObj = deen.getDeenObj(path);

        try (

                var is = new FileInputStream(path.toFile());
                var isr = new BufferedReader(
                        new InputStreamReader(
                                deObj.decryptInputStream(is)
                        )
                );
                var os =new FileOutputStream(tmpPath.toFile());
                var cos = deObj.encryptOutputStream(os);
        ) {
            SkipFirstLine(is);
            deen.writeHeader(os, deObj);

            deen.loopAndFindKey(
                    isr,
                    key,
                    (line,matcher,byPass) -> {
                        // should be ignored, because not matching the standard format
                    },
                    (line, kv,byPass) -> {
                        var name = kv.getKey();
                        Log.debug(() -> "Match name: " + name);
                        if (!name.equals(key)) {
                            Log.debug(() -> "In-match key, write to new file");
                            cos.write(String.format("%s\n", line).getBytes());
                        } else {
                            Log.debug(() -> "Match key, skipped");
                        }
                    },
                    (line, byPass) -> {}
            );
            cos.flush();
        }

        deen.switchTempFile(path, tmpPath);
        return 0;

    }

    public static void main(String[] args) {
        var cmd = new CommandLine(new Unset());
        Out.get().println(cmd.execute(args));
    }
}
