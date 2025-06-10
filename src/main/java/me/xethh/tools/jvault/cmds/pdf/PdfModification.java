package me.xethh.tools.jvault.cmds.pdf;

import me.xethh.tools.jvault.display.Console;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static me.xethh.tools.jvault.exceptionhandling.CommonHandle.throwExceptionIfNotExpected;
import static me.xethh.tools.jvault.exceptionhandling.CommonHandle.tryCatchThrow;

public class PdfModification {
    private PdfModification(){
        throw new IllegalStateException("Not expected to be instantiated");
    }
    public static Optional<PDDocument> loadFile(File file, Optional<String> withPassword) {
        if (withPassword.isPresent()) {
            try {
                return Optional.of(Loader.loadPDF(file, withPassword.get()));
            } catch (InvalidPasswordException ex) {
                return Optional.empty();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                return Optional.ofNullable(Loader.loadPDF(file));
            } catch (InvalidPasswordException ex) {
                return Optional.empty();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void modify(File file, PDDocument doc, UnaryOperator<PDDocument> docOp) {
        final var fn = file.getName();
        final var fnTemp = fn + ".tmp";
        final var path = file.getAbsoluteFile().getParentFile().toPath();
        final var doc2 = docOp.apply(doc);
        tryCatchThrow(()->{
            doc2.save(path.resolve(fnTemp).toFile());
            doc2.close();
            final var delRes = path.resolve(fn).toFile().delete();
            Console.getConsole().logIf(!delRes, "Failed to delete " + fnTemp);
            throwExceptionIfNotExpected(path.resolve(fn).toFile().exists(), "File not deleted!");
            final var rnRes = path.resolve(fnTemp).toFile().renameTo(path.resolve(fn).toFile());
            Console.getConsole().logIf(!rnRes, "Failed to rename " + fnTemp);
            throwExceptionIfNotExpected(path.resolve(fnTemp).toFile().exists(),"File not renamed!");
            return "";
        });
    }
}
