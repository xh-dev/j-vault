package me.xethh.tools.jVault;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.


import me.xethh.tools.jVault.cmds.deen.Deen;
import me.xethh.tools.jVault.cmds.file.FileCommand;
import me.xethh.tools.jVault.cmds.token.Token;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "J-Vault",
        subcommands = {
                Deen.class, FileCommand.class, Token.class
        }
)
public class Main implements Callable<Integer> {

    public static void main(String[] args) {
        var cmd = new CommandLine(new Main());
        cmd.execute(args);
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }
}