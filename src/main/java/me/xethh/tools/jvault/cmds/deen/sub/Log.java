package me.xethh.tools.jvault.cmds.deen.sub;

import java.io.PrintStream;
import java.util.function.Supplier;

public class Log {
    /**
     * debug option, should never be valued as true in production
     */
    public static final boolean DEBUG = false;
    public static final PrintStream DebugLog = System.err;
    public static final PrintStream console = System.out;

    public static void debug(Supplier<String> logMsg){
        if(DEBUG){
            DebugLog.println(logMsg.get());
        }
    }

    public static void msg(Supplier<String> logMsg){
        console.println(logMsg.get());
    }
}
