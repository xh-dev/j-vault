package me.xethh.tools.jVault.cmds.debug;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name="debug-tools",
        subcommands = {

        }
)
public class DebugTools implements Callable<Integer> {
    @CommandLine.Command(
            name="print-env",
            description = "Print environment variables"
    )
    public static class PrintEnvs implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            System.out.println("==================== Debug tools - Print all env");
            System.getenv().forEach((key, value) -> System.out.printf("'%s'=`%s`%n", key, value));
            System.out.println("==================== Debug tools - Print all env [Done]");
            return 0;
        }
    }
    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }
}
