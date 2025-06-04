package me.xethh.tools.jvault.cmds.debug;

import picocli.CommandLine;

import java.util.concurrent.Callable;

import static me.xethh.tools.jvault.cmds.deen.sub.Common.Out;

@CommandLine.Command(
        name="debug-tools",
        subcommands = {
                DebugTools.PrintEnvs.class,
                DebugTools.PrintProps.class,
        }
)
public class DebugTools implements Callable<Integer> {
    @CommandLine.Command(
            name="print-props",
            description = "Print java properties in the running jvm",
            subcommands = {
            }
    )
    public static class PrintProps implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            Out.get().println("==================== Debug tools - Print all properties");
            System.getProperties().forEach((key, value) -> Out.get().printf("'%s'=`%s`%n", key, value));
            Out.get().println("==================== Debug tools - Print all properties [Done]");
            return 0;
        }
    }
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
