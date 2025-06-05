package me.xethh.tools.jvault.display;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Console {
    private static Console instance;

    public static Console getConsole() {
        if (instance == null) {
            instance = new Console(System.out, System.err);
        }
        return new Console(System.out, System.err);
    }

    public static void restConsole(){
        instance = null;
    }

    private final PrintStream display;
    private final PrintStream error;

    public PrintStream getDisplay() {
        return display;
    }

    public PrintStream getError() {
        return error;
    }

    private Console(PrintStream display, PrintStream error) {
        this.display = display;
        this.error = error;
    }

    public final static boolean DEBUGGING = Optional.ofNullable(System.getenv("DEV")).isPresent();


    public void log(String msg) {
        try {
            display.write((msg+"\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void debug(String msg) {
        try {
            error.write(msg.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void error(String msg) {
        try {
            error.write(msg.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void printStackTrace(Throwable throwable) {
        if (DEBUGGING) {
            throwable.printStackTrace(error);
        }
    }


}
