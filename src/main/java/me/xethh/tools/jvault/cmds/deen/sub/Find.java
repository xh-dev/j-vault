package me.xethh.tools.jvault.cmds.deen.sub;

import me.xethh.tools.jvault.cmds.deen.Vault;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static me.xethh.tools.jvault.cmds.deen.sub.Common.skipFirstLine;

@CommandLine.Command(
        name = "find",
        description = "find the vault content by key"
)
public class Find implements ConsoleOwner, Callable<Integer> {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    private boolean helpRequested;
    @CommandLine.Option(names = {"-f", "--file"}, defaultValue = "vault.kv", description = "The file to encrypt")
    private File file;

    @CommandLine.Option(names = {"-k", "--key"}, required = true, description = "the key to find")
    private String key;

    @CommandLine.Option(names = {"--out-bash-env"}, required = false, description = "Output as bash export env", defaultValue = "false")
    private boolean outBash;

    @CommandLine.Option(names = {"--out-win-env"}, required = false, description = "Output as windows set env string", defaultValue = "false")
    private boolean outCmd;


    @CommandLine.ParentCommand
    private Vault deen;

    @Override
    public Integer call() throws Exception {
        var path = file.toPath().toAbsolutePath();
        var deObj = deen.getDeenObj(path);

        try (var is = new FileInputStream(path.toFile());
             var isr = new BufferedReader(
                     new InputStreamReader(
                             deObj.decryptInputStream(is)
                     )
             )
        ) {
            skipFirstLine(is);

            var foundValue = new AtomicReference<Optional<String>>(Optional.empty());

            deen.loopAndFindKey(isr, key,
                    (line1, kv, byPass) -> {
                        var value = kv.getValue();
                        foundValue.set(Optional.of(value));
                        byPass.set(true);
                    },
                    (line1, matcher, byPass) -> {
                    },
                    (line1, byPass) -> {
                    }
            );

            foundValue.get().ifPresent(it -> {
                final var v = URLDecoder.decode(it, StandardCharsets.UTF_8);
                if (outBash) {
                    console().log(String.format("export %s=%s", key.replace("-", "_"), v));
                } else if (outCmd) {
                    console().log(String.format("set %s=%s", key, v));
                } else {
                    console().log(v);
                }
            });
        }
        return 1;
    }

}
