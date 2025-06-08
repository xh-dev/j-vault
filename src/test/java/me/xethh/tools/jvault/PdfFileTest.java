package me.xethh.tools.jvault;

import me.xethh.tools.jvault.cmds.pdf.PdfManaging;
import me.xethh.tools.jvault.cmds.pdf.PdfModification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Test pdf file features")
public class PdfFileTest {
    @Test
    @DisplayName("When encrypt and decrypt pdf")
    public void testPdfFileEncrypt() {
        var streams = PasswordGenTest.streams();
        var is = streams._1();
        var os = streams._2();
        var es = streams._3();

        final var f1 = Path.of("stuff/test.pdf");
        final var f2 = Path.of("target/test-case/test.pdf");
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
                    assertFalse(PdfModification.loadFile(f2.toFile(), Optional.empty()).get().isEncrypted());
                    Main.main("pdf -f target/test-case/test.pdf set-password -p 12345678 -u 1234".split(" "));
                    assertTrue(PdfModification.loadFile(f2.toFile(), Optional.of("12345678")).get().isEncrypted());
                    assertTrue(PdfModification.loadFile(f2.toFile(), Optional.of("12")).isEmpty());

                    Main.main("pdf -f target/test-case/test.pdf unset-password -p 178".split(" "));
                    assertTrue(PdfModification.loadFile(f2.toFile(), Optional.of("12345678")).get().isEncrypted());
                    assertTrue(PdfModification.loadFile(f2.toFile(), Optional.of("12")).isEmpty());

                    Main.main("pdf -f target/test-case/test.pdf -p 123 set-password -p 8888".split(" "));
                    assertTrue(PdfModification.loadFile(f2.toFile(), Optional.of("12345678")).get().isEncrypted());
                    assertTrue(PdfModification.loadFile(f2.toFile(), Optional.of("12")).isEmpty());

                    Main.main("pdf -f target/test-case/test.pdf -p 12345678 set-password -p 8888 -u 7777".split(" "));
                    assertTrue(PdfModification.loadFile(f2.toFile(), Optional.of("8888")).get().isEncrypted());
                    assertTrue(PdfModification.loadFile(f2.toFile(), Optional.of("7777")).isPresent());


                    Main.main("pdf -f target/test-case/test.pdf unset-password -p 8888".split(" "));
                    assertFalse(PdfModification.loadFile(f2.toFile(), Optional.empty()).get().isEncrypted());

                    Main.main("pdf -f target/test-case/test.pdf test".split(" "));
                    Main.main("pdf -f target/test-case/test.pdf -p 8888 test".split(" "));
                    Main.main("pdf -f target/test-case/test.pdf -p 0000 test".split(" "));

                }
        );

        try{
            PdfManaging.libExists("awt");
            PdfManaging.libExists("abcii");
        } catch (Exception e){
            System.out.println("This is false test");
        }

        try {
            assertFalse(PdfManaging.setFileHidden(f2));
        } catch (Exception e) {
            System.out.println("This maybe false in linux");
        }
    }
}
