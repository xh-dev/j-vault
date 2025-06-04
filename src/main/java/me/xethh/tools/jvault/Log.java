package me.xethh.tools.jvault;

import java.util.Optional;

public class Log {
    public final static boolean DEBUGGING = Optional.ofNullable(System.getenv("DEV")).isPresent();
    public static void log(String msg) {
        if(DEBUGGING){
            System.out.println(msg);
        }
    }

}
