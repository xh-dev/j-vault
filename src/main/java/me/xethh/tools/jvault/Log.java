package me.xethh.tools.jvault;

import java.util.Optional;

public class Log {
    final static boolean debugging = Optional.ofNullable(System.getenv("DEV")).isPresent();
    public static void log(String msg) {
        if(debugging){
            System.out.println(msg);
        }
    }

}
