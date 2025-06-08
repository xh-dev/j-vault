package me.xethh.tools.jvault;

import net.lingala.zip4j.ZipFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Test zip file features")
public class ZipFileTest {
    @Test
    @DisplayName("When ")
    public void testVaultSetOverride() {
        var is = new ByteArrayInputStream(new byte[]{});
        var os = new ByteArrayOutputStream();
        var es = new ByteArrayOutputStream();

        final var f1 = Path.of("stuff/dummy.zip");
        final var f2 = Path.of("target/test-case/dummy.zip");
        f2.getParent().toFile().mkdirs();
        if (f2.toFile().exists()) {
            f2.toFile().delete();
        }

        try (
                var tis = new FileInputStream(f1.toFile());
                var tos = new FileOutputStream(f2.toFile())
        ) {
            tos.write(tis.readAllBytes());

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        PasswordGenTest.borrowStdOutV2(is, os, es, () -> {
                    var zip = new ZipFile(f2.toFile());
                    assertFalse(zip.isEncrypted());
                    Main.main("zip -f target/test-case/dummy.zip set-password -p 12345678".split(" "));
                    zip = new ZipFile(f2.toFile());
                    assertTrue(zip.isEncrypted());
                    Main.main("zip -f target/test-case/dummy.zip -p 12345678 unset-password".split(" "));
                    zip = new ZipFile(f2.toFile());
                    assertFalse(zip.isEncrypted());
                }
        );
    }
}
