package me.xethh.tools.jvault.cmds.autocomplete;

import me.xethh.tools.jvault.Main;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static me.xethh.tools.jvault.cmds.deen.sub.Common.Out;

@CommandLine.Command(
        name = "autocomplete",
        description = "generate bash autocomplete script"
)
public class GenAutoCompleteCmd implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        final var bashScript = picocli.AutoComplete.bash("j-vault", new CommandLine(new Main()));
        Out.get().println(bashScript);
        return 0;
    }
}
