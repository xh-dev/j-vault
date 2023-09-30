package me.xethh.tools.jVault.cmds.deen.sub;

import java.io.PrintStream;
import java.util.function.Supplier;

public class DebugLog {
    /**
     * debug option, should never be valued as true in production
     */
    public static final boolean DEBUG = false;
    public static final PrintStream DebugLog = System.err;

    public static void log(Supplier<String> logMsg){
        if(DEBUG){
            DebugLog.println(logMsg.get());
        }
    }
}
