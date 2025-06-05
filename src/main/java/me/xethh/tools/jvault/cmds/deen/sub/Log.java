package me.xethh.tools.jvault.cmds.deen.sub;

import java.io.PrintStream;
import java.util.function.Supplier;

public class Log {
    /**
     * debug option, should never be valued as true in production
     */
    public static final boolean DEBUG = false;
    public static final PrintStream DEBUG_LOG = System.err;
    public static final PrintStream CONSOLE = System.out;

    public static void debug(Supplier<String> logMsg) {
        if (DEBUG) {
            DEBUG_LOG.println(logMsg.get());
        }
    }

    public static void msg(Supplier<String> logMsg) {
        CONSOLE.println(logMsg.get());
    }
}
