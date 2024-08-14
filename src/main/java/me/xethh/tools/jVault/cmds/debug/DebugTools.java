package me.xethh.tools.jVault.cmds.debug;

import picocli.CommandLine;

import java.util.concurrent.Callable;

import static me.xethh.tools.jVault.cmds.deen.sub.Common.Out;

@CommandLine.Command(
        name="debug-tools",
        subcommands = {
                DebugTools.PrintEnvs.class
        }
)
public class DebugTools implements Callable<Integer> {
    @CommandLine.Command(
            name="print-env",
            description = "Print environment variables",
            subcommands = {
            }
    )
    public static class PrintEnvs implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            Out.get().println("==================== Debug tools - Print all env");
            System.getenv().forEach((key, value) -> Out.get().printf("'%s'=`%s`%n", key, value));
            Out.get().println("==================== Debug tools - Print all env [Done]");
            return 0;
        }
    }
    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, Out.get());
        return 0;
    }
}
