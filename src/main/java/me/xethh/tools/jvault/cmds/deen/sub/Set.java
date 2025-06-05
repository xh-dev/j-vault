package me.xethh.tools.jvault.cmds.deen.sub;

import me.xethh.tools.jvault.cmds.deen.Vault;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import picocli.CommandLine;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.xethh.tools.jvault.cmds.deen.sub.Common.SkipFirstLine;

@CommandLine.Command(
        name = "set",
        description = "set a key value entry"
)
public class Set implements ConsoleOwner, Callable<Integer> {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
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
                    (line, matcher, byPass) -> {
                        console().debug("Matching key: ");
                        if (found.get()) {
                            console().debug("Duplicate line found");
                            // Already found refers to duplicate key existing, should be ignored
                            console().log(String.format("Key[%s] already exist, skipped%n", key));
                        } else {
                            console().debug("First value encounter");
                            console().log(String.format("Found key[%s] and replace%n", key));
                            found.set(true);
                            cos.write(String.format("%s=%s\n", key, URLEncoder.encode(value, StandardCharsets.UTF_8)).getBytes());
                            console().debug("Complete write value");
                        }
                    },
                    (line, matcher, byPass) -> {
                        console().debug("In-matching key: ");
                        cos.write(String.format("%s\n", line).getBytes());
                    },
                    (line, byPass) -> {
                    }
            );

            if (!found.get()) { // The key not found in the whole vault, add to the end
                console().debug("Append KV to the end of vault");
                cos.write(String.format("%s=%s\n", key, URLEncoder.encode(value, StandardCharsets.UTF_8)).getBytes());
            }
            cos.flush();
        }
        deen.switchTempFile(path, tmpPath);
        return 0;

    }
}
