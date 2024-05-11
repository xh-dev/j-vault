package me.xethh.tools.jVault.cmds.autoComplete;

import me.xethh.tools.jVault.Main;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "autocomplete",
        description = "generate bash autocomplete script"
)
public class GenAutoCompleteCmd implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        final var bashScript = picocli.AutoComplete.bash("j-vault", new CommandLine(new Main()));
        System.out.println(bashScript);
        return 0;
    }
}
