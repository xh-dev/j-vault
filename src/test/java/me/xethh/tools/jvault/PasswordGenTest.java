package me.xethh.tools.jvault;

import io.vavr.CheckedRunnable;
import io.vavr.Tuple3;
import me.xethh.tools.jvault.cmds.deen.DeenObj;
import me.xethh.tools.jvault.display.Console;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.*;
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

    //public static ByteArrayOutputStream borrowStdOut(Consumer<ByteArrayOutputStream> r) {
    //    var o = System.out;
    //    var os = new ByteArrayOutputStream();
    //    Console.restConsole();
    //    System.setOut(new java.io.PrintStream(os, true, StandardCharsets.UTF_8));
    //    r.accept(os);
    //    System.setOut(o);
    //    return os;
    //}

    public static Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> streams() {
        return streams(new byte[]{});
    }

    public static Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> streams(byte[] data) {
        return new Tuple3<>(new ByteArrayInputStream(data), new ByteArrayOutputStream(), new ByteArrayOutputStream() );
    }

    public static void borrowStdOutV2(InputStream is, OutputStream os, OutputStream es, CheckedRunnable r) {
        var i = System.in;
        var o = System.out;
        var e = System.err;
        System.setIn(is);
        System.setOut(new PrintStream(os, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(es, true, StandardCharsets.UTF_8));
        Console.restConsole();
        r.unchecked().run();
        System.setIn(i);
        System.setOut(o);
        System.setErr(e);
    }


    @Test
    @DisplayName("When run j-vault token gen")
    void testGenDefault() {
        var streams = streams();
        borrowStdOutV2(streams._1(), streams._2(), streams._3(),() -> {
            CommandLine cmd = new CommandLine(new Main());
            cmd.execute("token", "gen");
            var line = streams._2().toString();
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
    void testGen() {
        var streams = streams();
        borrowStdOutV2(streams._1(),streams._2(),streams._3(), () -> {
            CommandLine cmd = new CommandLine(new Main());
            cmd.execute("token", "gen", "-p", "abcd");
            var line = streams._2().toString();
            assertTrue(line.endsWith("\n"));
            line = line.substring(0, line.length() - 1);
            var part1 = line.split(":")[0];
            var part2 = line.split(":")[1];

            assertEquals("abcd", part1);
            assertEquals(DeenObj.DEFAULT_SALT1_LENGTH, Base64.getDecoder().decode(part2).length);
        });
    }
}
