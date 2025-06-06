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
import java.util.concurrent.Callable;

import static me.xethh.tools.jvault.cmds.deen.sub.Common.skipFirstLine;

@CommandLine.Command(
        name = "view",
        description = "view the vault content"
)
public class View implements ConsoleOwner, Callable<Integer> {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    private boolean helpRequested;
    @CommandLine.Option(names = {"-f", "--file"}, defaultValue = "vault.kv", description = "The file to encrypt")
    private File file;

    @CommandLine.ParentCommand
    private Vault deen;

    @CommandLine.Option(names = {"--out-bash-env"}, required = false, description = "Output as bash export env", defaultValue = "false")
    private boolean outBash;

    @CommandLine.Option(names = {"--out-win-env"}, required = false, description = "Output as windows set env string", defaultValue = "false")
    private boolean outCmd;

    @CommandLine.Option(names = {"--out-raw"}, required = false, description = "Output as url encoded form", defaultValue = "false")
    private boolean outRaw;


    @Override
    public Integer call() throws Exception {
        var path = file.toPath().toAbsolutePath();

        var credsEnv = deen.finalCredential();
        var deObj = Common.getDeenObj(path, credsEnv);

        try (
                var is = new FileInputStream(path.toFile());
        ) {
            skipFirstLine(is);
            try (
                    var isr = new BufferedReader(new InputStreamReader(deObj.decryptInputStream(is)));
            ) {
                String line;
                while ((line = isr.readLine()) != null) {
                    final var kv = Common.KVExtractor.extract(URLDecoder.decode(line, StandardCharsets.UTF_8));
                    if (outBash) {
                        console().log(String.format("export %s=\"%s\"", kv.getKey().replace("-", "_"), kv.getValue()));
                    } else if (outCmd) {
                        console().log(String.format("set %s=%s", kv.getKey(), kv.getValue()));
                    } else if (outRaw) {
                        console().log(line);
                    } else {
                        console().log(String.format("%s=%s", kv.getKey(), kv.getValue()));
                    }
                }
            }
        }
        return 0;


    }
}
