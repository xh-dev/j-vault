package me.xethh.tools.jvault;

import picocli.CommandLine;

import java.util.Objects;

public class VP implements CommandLine.IVersionProvider {
    @Override
    public String[] getVersion() throws Exception {
        try (
                var is = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("version.txt"));
        ) {
            var text = new String(is.readAllBytes());
            return text.split("\n");
        }
    }
}
