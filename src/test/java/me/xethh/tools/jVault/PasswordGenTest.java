package me.xethh.tools.jVault;

import me.xethh.tools.jVault.cmds.deen.DeenObj;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Password Generator Test")
public class PasswordGenTest {
    private static final String USER_DIR_PROPERTY = "user.dir";

    public static void prepareEmptyDirectoryAsHome(File file, Consumer<Path> r) {
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new RuntimeException("File " + file.getAbsolutePath() + " is not a directory");
            }
            for (File f : Objects.requireNonNull(file.listFiles())) {
                if (!f.delete()) {
                    throw new RuntimeException("Cannot delete file " + f.getAbsolutePath());
                }
            }
        } else {
            if (!file.mkdirs()) {
                throw new RuntimeException("Cannot create directory " + file.getAbsolutePath());
            }
        }
        r.accept(file.toPath().toAbsolutePath());
    }

    public static ByteArrayOutputStream borrowStdOut(Consumer<ByteArrayOutputStream> r) {
        var o = System.out;
        var os = new ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(os, true, StandardCharsets.UTF_8));
        r.accept(os);
        System.setOut(o);
        return os;
    }

    @Test
    @DisplayName("When run j-vault token gen")
    public void testGenDefault() {
        borrowStdOut((os) -> {
            CommandLine cmd = new CommandLine(new Main());
            cmd.execute("token", "gen");
            var line = os.toString();
            assertTrue(line.endsWith("\n"));
            line = line.substring(0, line.length() - 1);
            var part1 = line.split(":")[0];
            var part2 = line.split(":")[1];

            assertEquals(16, Base64.getDecoder().decode(part1).length);
            assertEquals(DeenObj.DEFAULT_SALT1_LENGTH, Base64.getDecoder().decode(part2).length);
        });
    }

    @Test
    @DisplayName("When run j-vault token gen with default password")
    public void testGen() {
        borrowStdOut((os) -> {
            CommandLine cmd = new CommandLine(new Main());
            cmd.execute("token", "gen", "-p", "abcd");
            var line = os.toString();
            assertTrue(line.endsWith("\n"));
            line = line.substring(0, line.length() - 1);
            var part1 = line.split(":")[0];
            var part2 = line.split(":")[1];

            assertEquals("abcd", part1);
            assertEquals(DeenObj.DEFAULT_SALT1_LENGTH, Base64.getDecoder().decode(part2).length);
        });
    }
}
