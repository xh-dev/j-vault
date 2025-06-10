package me.xethh.tools.jvault.display;

import io.vavr.CheckedRunnable;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class Console {
    public boolean isDebugging = Optional.ofNullable(System.getenv("DEV")).isPresent();
    private static Console instance;
    private final InputStream inputStream;
    private final PrintStream display;
    private final PrintStream error;
    private Console(InputStream is, PrintStream display, PrintStream error) {
        this.inputStream = is;
        this.display = display;
        this.error = error;
    }

    public static Console getConsole() {
        if (instance == null) {
            instance = new Console(System.in, System.out, System.err);
        }
        return instance;
    }

    public void logIf(boolean res, String msg) {
        if(res){
            getConsole().log(msg);
        }
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

    public void doIfDebug(CheckedRunnable runnable) {
        if (isDebugging) {
            runnable.unchecked().run();
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
