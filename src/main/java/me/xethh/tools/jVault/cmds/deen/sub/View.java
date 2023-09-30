package me.xethh.tools.jVault.cmds.deen.sub;

import me.xethh.tools.jVault.cmds.deen.Deen;
import picocli.CommandLine;

import java.io.*;
import java.util.concurrent.Callable;

import static me.xethh.tools.jVault.cmds.deen.sub.Common.*;

@CommandLine.Command(
        name = "view"
)
public class View implements Callable<Integer> {
    @CommandLine.Option(names = {"-f", "--file"}, defaultValue = "vault.kv", description = "The file to encrypt")
    private File file;

    @CommandLine.ParentCommand
    private Deen deen;


    @Override
    public Integer call() throws Exception {
        var path = file.toPath().toAbsolutePath();

        var credsEnv = deen.finalCredential();
        var deObj = Common.getDeenObj(path, credsEnv);

        try (
                var is = new FileInputStream(path.toFile());
        ) {
            SkipFirstLine(is);
            try (
                    var isr = new BufferedReader(new InputStreamReader(deObj.decryptInputStream(is)));
            ) {
                String line;
                while ((line = isr.readLine()) != null) {
                    System.out.println(line);
                }
            }
        }
        return 0;


    }
}
