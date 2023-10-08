package me.xethh.tools.jVault.cmds.deen.sub;

import me.xethh.tools.jVault.cmds.deen.Vault;
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

import static me.xethh.tools.jVault.cmds.deen.sub.Common.Out;
import static me.xethh.tools.jVault.cmds.deen.sub.Common.SkipFirstLine;

@CommandLine.Command(
        name = "find",
        description = "find the vault content by key"
)
public class Find implements Callable<Integer> {
    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested;
    @CommandLine.Option(names = {"-f", "--file"}, defaultValue = "vault.kv", description = "The file to encrypt")
    private File file;

    @CommandLine.Option(names = {"-k", "--key"}, required = true, description = "the key to find")
    private String key;


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
            SkipFirstLine(is);

            var foundValue = new AtomicReference<Optional<String>>(Optional.empty());

            deen.loopAndFindKey(isr, key,
                    (line1, matcher, byPass) -> {
                        var value = matcher.group(2);
                        foundValue.set(Optional.of(value));
                        byPass.set(true);
                    },
                    (line1, matcher, byPass) -> {
                    },
                    (line1,byPass) -> {}
            );

            foundValue.get().ifPresent(it->{
                Out.get().println(URLDecoder.decode(it, StandardCharsets.UTF_8));
            });
        }
        return 1;
    }

}
