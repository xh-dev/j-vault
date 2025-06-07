package me.xethh.tools.jvault.display;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Console {
    public boolean isDebugging = Optional.ofNullable(System.getenv("DEV")).isPresent();
    private static Console instance;
    private final PrintStream display;
    private final PrintStream error;
    private Console(PrintStream display, PrintStream error) {
        this.display = display;
        this.error = error;
    }

    public static Console getConsole() {
        if (instance == null) {
            instance = new Console(System.out, System.err);
        }
        return new Console(System.out, System.err);
    }

    public static void restConsole() {
        instance = null;
    }

    public PrintStream getDisplay() {
        return display;
    }

    public PrintStream getError() {
        return error;
    }

    public void log(String msg) {
        try {
            display.write((msg + "\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void debug(String msg) {
        try {
            if (isDebugging) {
                error.write((msg + "\n").getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isDebugging() {
        return isDebugging;
    }

    public void doIfDebug(Runnable runnable) {
        if (isDebugging) {
            runnable.run();
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
        if (isDebugging) {
            throwable.printStackTrace(error);
        }
    }

    public void setDebugging() {
        this.isDebugging = true;
    }

    public void disableDebugging() {
        this.isDebugging = false;
    }


}
