package me.xethh.tools.jvault.cmds.autocomplete;

import me.xethh.tools.jvault.Main;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "autocomplete",
        description = "generate bash autocomplete script"
)
public class GenAutoCompleteCmd implements ConsoleOwner, Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        final var bashScript = picocli.AutoComplete.bash("j-vault", new CommandLine(new Main()));
        console().log(bashScript);
        return 0;
    }
}
