package me.xethh.tools.jvault.cmds.debug;

import me.xethh.tools.jvault.display.Console;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import picocli.CommandLine;

import java.util.concurrent.Callable;


@CommandLine.Command(
        name = "debug-tools",
        subcommands = {
                DebugTools.PrintEnvs.class,
                DebugTools.PrintProps.class,
        }
)
public class DebugTools implements ConsoleOwner, Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, Console.getConsole().getDisplay());
        return 0;
    }

    @CommandLine.Command(
            name = "print-props",
            description = "Print java properties in the running jvm",
            subcommands = {
            }
    )
    public static class PrintProps implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            Console.getConsole().log("==================== Debug tools - Print all properties");
            System.getProperties().forEach((key, value) -> Console.getConsole().log(String.format("'%s'=`%s`%n", key, value)));
            Console.getConsole().log("==================== Debug tools - Print all properties [Done]");
            return 0;
        }
    }

    @CommandLine.Command(
            name = "print-env",
            description = "Print environment variables",
            subcommands = {
            }
    )
    public static class PrintEnvs implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            Console.getConsole().log("==================== Debug tools - Print all env");
            System.getenv().forEach((key, value) -> Console.getConsole().log(String.format("'%s'=`%s`%n", key, value)));
            Console.getConsole().log("==================== Debug tools - Print all env [Done]");
            return 0;
        }
    }
}
