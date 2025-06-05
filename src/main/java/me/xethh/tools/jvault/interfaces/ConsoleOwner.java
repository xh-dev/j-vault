package me.xethh.tools.jvault.interfaces;

import me.xethh.tools.jvault.display.Console;

public interface ConsoleOwner {
    default Console console() {
        return Console.getConsole();
    }
}
