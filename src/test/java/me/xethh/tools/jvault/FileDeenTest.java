package me.xethh.tools.jvault;

import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("File decrypt and encrypt test")
public class FileDeenTest implements ConsoleOwner {


    @Test
    @DisplayName("When run j-vault file encrypt -f target/test-case/a.txt")
    public void testGenDefault() {
        final var TOKEN = "abcd:sss";
        final var MESSAGE = "helloworld";
        var parent = new File("target/test-case/");
        if(!parent.mkdirs()){
            console().log("Folder creation failed");
        }

        assertTrue(parent.exists(), "parent folder should exists");
        for (var x : Objects.requireNonNull(parent.listFiles())) {
            final var res = x.delete();
            if(!res){
                console().log(String.format("File[%s] deleted fail", x.toString()));
            }

        }
        assertEquals(0, Objects.requireNonNull(parent.listFiles()).length, "parent folder should be empty");

        var f = new File(parent, "a.txt");
        var fo = new File(parent, "a.txt.crypt");
        if (f.exists()) {
            var res = f.delete();
            if(!res){
                console().log(String.format("File[%s] deleted fail", f.toString()));
            }
        }

        assertFalse(f.exists(), "original file should not exists when init");
        assertFalse(fo.exists(), "encrypted file should not exists when init");
        try (var os = new FileOutputStream(f)) {
            os.write(MESSAGE.getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        assertTrue(f.exists(), "original file should exists before execute command");

        new CommandLine(new Main()).execute("file", "-c", TOKEN, "encrypt", "-f", f.getAbsolutePath());
        assertFalse(f.exists(), "original file should be deleted after execute command");
        assertTrue(fo.exists(), "encrypted file should exists after execute command");

        new CommandLine(new Main()).execute("file", "-c", TOKEN, "decrypt", "-f", fo.getAbsolutePath());
        assertTrue(f.exists(), "original file should exists after execute second command");
        assertFalse(fo.exists(), "encrypted file should be deleted after execute second command");

        try (var fi = new FileInputStream(f);) {
            assertEquals(MESSAGE, new String(fi.readAllBytes(), StandardCharsets.UTF_8));
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
            if(parent.mkdirs()){
                console().log("Folder creation failed");
            }

            assertTrue(parent.exists(), "parent folder should exists");
            for (var x : Objects.requireNonNull(parent.listFiles())) {
                if(!x.delete()){
                    console().log(String.format("File[%s] deleted fail", x.toString()));
                }
            }
            assertEquals(0, Objects.requireNonNull(parent.listFiles()).length, "parent folder should be empty");

            var f = new File(parent, "a.txt");
            var fo = new File(parent, "a.txt.crypt");
            if (f.exists()) {
                var res = f.delete();
                if(!res){
                    console().log(String.format("File[%s] deleted fail", f.toString()));
                }
            }

            assertFalse(f.exists(), "original file should not exists when init");
            assertFalse(fo.exists(), "encrypted file should not exists when init");
            try (var os = new FileOutputStream(f)) {
                os.write(MESSAGE.getBytes(StandardCharsets.UTF_8));
                os.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            assertTrue(f.exists(), "original file should exists before execute command");

            new CommandLine(new Main()).execute("file", "-c", TOKEN, "encrypt", "-f", f.getAbsolutePath());
            assertFalse(f.exists(), "original file should be deleted after execute command");
            assertTrue(fo.exists(), "encrypted file should exists after execute command");

            new CommandLine(new Main()).execute("file", "-c", TOKEN, "decrypt", "-f", fo.getAbsolutePath(), "--stdout");
            assertFalse(f.exists(), "original file should exists after execute second command");
            assertTrue(fo.exists(), "encrypted file should be deleted after execute second command");

            assertEquals(MESSAGE + "\n", bos.toString());

        });
    }
}
