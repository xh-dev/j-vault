package me.xethh.tools.jvault.cmds.deen.sub;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "auth-server",
        description = "authentication server",
        subcommands = {
                SimpleAuthServer.class,
                AuthServerSecretGen.class
        }
)
public class AuthServer  implements Callable<Integer> {
    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested;
    @Override
    public Integer call() throws Exception {
        return 0;
    }
}
