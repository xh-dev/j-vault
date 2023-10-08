package me.xethh.tools.jVault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("File decrypt and encrypt test")
public class FileDeenTest {


    @Test
    @DisplayName("When run j-vault file encrypt -f target/test-case/a.txt")
    public void testGenDefault() {
        final var TOKEN = "abcd:sss";
        final var MESSAGE = "helloworld";
        var parent = new File("target/test-case/");
        parent.mkdirs();

        assertTrue(parent.exists(), "parent folder should exists");
        for (var x : parent.listFiles()) {
            x.delete();
        }
        assertTrue(Objects.requireNonNull(parent.listFiles()).length == 0, "parent folder should be empty");

        var f = new File(parent, "a.txt");
        var fo = new File(parent, "a.txt.crypt");
        if (f.exists())
            f.delete();

        assertTrue(!f.exists(), "original file should not exists when init");
        assertTrue(!fo.exists(), "encrypted file should not exists when init");
        try (var os = new FileOutputStream(f)) {
            os.write(MESSAGE.getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertTrue(f.exists(), "original file should exists before execute command");

        new CommandLine(new Main()).execute("file", "-c", TOKEN, "encrypt", "-f", f.getAbsolutePath());
        assertTrue(!f.exists(), "original file should be deleted after execute command");
        assertTrue(fo.exists(), "encrypted file should exists after execute command");

        new CommandLine(new Main()).execute("file", "-c", TOKEN, "decrypt", "-f", fo.getAbsolutePath());
        assertTrue(f.exists(), "original file should exists after execute second command");
        assertTrue(!fo.exists(), "encrypted file should be deleted after execute second command");

        try (var fi = new FileInputStream(f);) {
            assertEquals(MESSAGE, new String(fi.readAllBytes(), StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("When run j-vault file encrypt -f target/test-case/a.txt --stdout")
    public void testFileEncryptDecryptAsStdout() {
        PasswordGenTest.borrowStdOut(bos -> {
            final var TOKEN = "abcd:sss";
            final var MESSAGE = "helloworld";
            var parent = new File("target/test-case/");
            parent.mkdirs();

            assertTrue(parent.exists(), "parent folder should exists");
            for (var x : parent.listFiles()) {
                x.delete();
            }
            assertTrue(Objects.requireNonNull(parent.listFiles()).length == 0, "parent folder should be empty");

            var f = new File(parent, "a.txt");
            var fo = new File(parent, "a.txt.crypt");
            if (f.exists())
                f.delete();

            assertTrue(!f.exists(), "original file should not exists when init");
            assertTrue(!fo.exists(), "encrypted file should not exists when init");
            try (var os = new FileOutputStream(f)) {
                os.write(MESSAGE.getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertTrue(f.exists(), "original file should exists before execute command");

            new CommandLine(new Main()).execute("file", "-c", TOKEN, "encrypt", "-f", f.getAbsolutePath());
            assertTrue(!f.exists(), "original file should be deleted after execute command");
            assertTrue(fo.exists(), "encrypted file should exists after execute command");

            new CommandLine(new Main()).execute("file", "-c", TOKEN, "decrypt", "-f", fo.getAbsolutePath(), "--stdout");
            assertTrue(!f.exists(), "original file should exists after execute second command");
            assertTrue(fo.exists(), "encrypted file should be deleted after execute second command");

            assertEquals(MESSAGE + "\n", bos.toString());

        });
    }
}
