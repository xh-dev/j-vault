package me.xethh.tools.jVault;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.


import me.xethh.tools.jVault.cmds.deen.Vault;
import me.xethh.tools.jVault.cmds.file.FileCommand;
import me.xethh.tools.jVault.cmds.token.Token;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        mixinStandardHelpOptions = true,
        versionProvider = VP.class,
        name = "J-Vault",
        subcommands = {
                Vault.class, FileCommand.class, Token.class
        },
        description = "j-vault is a very simple key value based password vault cli program. "
)
public class Main implements Callable<Integer> {

    public static void main(String[] args) {
        CommandLine cmd = new CommandLine(new Main());
        cmd.execute(args);
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }
}