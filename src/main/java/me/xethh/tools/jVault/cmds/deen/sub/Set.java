package me.xethh.tools.jVault.cmds.deen.sub;

import me.xethh.tools.jVault.cmds.deen.Vault;
import picocli.CommandLine;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.xethh.tools.jVault.cmds.deen.sub.Common.Out;
import static me.xethh.tools.jVault.cmds.deen.sub.Common.SkipFirstLine;

@CommandLine.Command(
        name = "set",
        description = "set a key value entry"
)
public class Set implements Callable<Integer> {
    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested;
    @CommandLine.Option(names = {"-f", "--file"}, defaultValue = "vault.kv", description = "The file to encrypt")
    private File file;

    @CommandLine.Option(names = {"-k", "--key"}, description = "the key to set", required = true)
    private String key;

    @CommandLine.Option(names = {"-v", "--value"}, description = "the value to set", required = true)
    private String value;

    @CommandLine.ParentCommand
    private Vault deen;

    @Override
    public Integer call() throws Exception {
        var path = file.toPath().toAbsolutePath();
        var tmpPath = new File(path + ".tmp").toPath();

        var deObj = deen.getDeenObj(path);

        try (var is = new FileInputStream(path.toFile());
                var os = new FileOutputStream(tmpPath.toFile());
                var isr = new BufferedReader(
                        new InputStreamReader(
                                deObj.decryptInputStream(is)
                        )
                );
                var cos = deObj.encryptOutputStream(os);
        ) {
            SkipFirstLine(is);
            deen.writeHeader(os, deObj);

            final var found = new AtomicBoolean(false);

            deen.loopAndFindKey(
                    isr,
                    key,
                    (line,matcher,byPass) -> {
                        Log.debug(() -> "Matching key: ");
                        if (found.get()) {
                            Log.debug(() -> "Duplicate line found");
                            // Already found refers to duplicate key existing, should be ignored
                            Out.get().printf("Key[%s] already exist, skipped%n", key);
                        } else {
                            Log.debug(() -> "First value encounter");
                            Out.get().printf("Found key[%s] and replace%n", key);
                            found.set(true);
                            cos.write(String.format("%s=%s\n", key, URLEncoder.encode(value, StandardCharsets.UTF_8)).getBytes());
                            Log.debug(() -> "Complete write value");
                        }
                    },
                    (line, matcher,byPass) -> {
                        Log.debug(() -> "In-matching key: ");
                        cos.write(String.format("%s\n", line).getBytes());
                    },
                    (line,byPass) -> {}
            );

            if (!found.get()) { // The key not found in the whole vault, add to the end
                Log.debug(() -> "Append KV to the end of vault");
                cos.write(String.format("%s=%s\n", key, URLEncoder.encode(value, StandardCharsets.UTF_8)).getBytes());
            }
            cos.flush();
        }
        deen.switchTempFile(path, tmpPath);
        return 0;

    }
}
