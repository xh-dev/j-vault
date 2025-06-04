package me.xethh.tools.jvault;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.


import me.xethh.tools.jvault.cmds.autocomplete.GenAutoCompleteCmd;
import me.xethh.tools.jvault.cmds.debug.DebugTools;
import me.xethh.tools.jvault.cmds.deen.Vault;
import me.xethh.tools.jvault.cmds.deen.sub.AuthServer;
import me.xethh.tools.jvault.cmds.file.FileCommand;
import me.xethh.tools.jvault.cmds.openssl.Openssl;
import me.xethh.tools.jvault.cmds.pdf.PdfManaging;
import me.xethh.tools.jvault.cmds.token.Token;
import me.xethh.tools.jvault.cmds.zip.ZipManaging;
import picocli.CommandLine;

import java.util.concurrent.Callable;

import static me.xethh.tools.jvault.cmds.deen.sub.Common.Out;

@CommandLine.Command(
        mixinStandardHelpOptions = true,
        versionProvider = VP.class,
        name = "J-Vault",
        subcommands = {
                Vault.class, FileCommand.class, ZipManaging.class, Token.class, PdfManaging.class, GenAutoCompleteCmd.class, DebugTools.class, Openssl.class, AuthServer.class
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
        CommandLine.usage(this, Out.get());
        return 0;
    }
}