package me.xethh.tools.jvault;

import me.xethh.tools.jvault.cmds.pdf.PdfModification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static me.xethh.tools.jvault.cmds.pdf.NativeHandling.libExists;
import static me.xethh.tools.jvault.cmds.pdf.NativeHandling.setFileHidden;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test pdf file features")
public class PdfFileTest {
    private Path prepare(){
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
        return f2;
    }

    @Test
    @DisplayName("When encrypt and decrypt pdf")
    public void testPdfTest() {
        var f2 = prepare();
        var cmd = String.format("pdf -f %s test", f2);
        var streams = PasswordGenTest.streamsWithPipe();
        String finalCmd = cmd;
        io.vavr.Tuple3<java.io.PipedOutputStream, java.io.ByteArrayOutputStream, java.io.ByteArrayOutputStream> finalStreams = streams;
        PasswordGenTest.borrowStdOutV3(streams._1(), streams._2(), streams._3(), ()->{
            Main.main(finalCmd.split(" "));
            var res = finalStreams._2().toString();
            assertEquals("The file is not protected by password\n", res);
        });

        streams = PasswordGenTest.streamsWithPipe();
        io.vavr.Tuple3<java.io.PipedOutputStream, java.io.ByteArrayOutputStream, java.io.ByteArrayOutputStream> finalStreams1 = streams;
        PasswordGenTest.borrowStdOutV3(streams._1(), streams._2(), streams._3(), ()->{
            Main.main(String.format("pdf -f %s set-password -p 12345 -u 2345", f2).split(" "));
            Main.main(String.format("pdf -f %s test", f2).split(" "));
            var res = finalStreams1._2().toString();
            assertEquals("The file maybe protected by password\n", res);

            Main.main(String.format("pdf -f %s -p 3333 test", f2).split(" "));
            res = finalStreams1._2().toString();
            assertEquals("The provided password is not correct", res.split("\n")[1]);

            Main.main(String.format("pdf -f %s -p 12345 test", f2).split(" "));
            res = finalStreams1._2().toString();
            assertEquals("The provided password is correct", res.split("\n")[2]);
        });


    }

    @Test
    @DisplayName("When encrypt and decrypt pdf")
    public void testPdfFileEncrypt() {
        var streams = PasswordGenTest.streams();
        var is = streams._1();
        var os = streams._2();
        var es = streams._3();

        var f2 = prepare();

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
            libExists("awt");
            libExists("abcii");
        } catch (Exception e){
            System.out.println("This is false test");
        }

        try {
            assertFalse(setFileHidden(f2));
        } catch (Exception e) {
            System.out.println("This maybe false in linux");
        }
    }
}
