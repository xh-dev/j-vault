package me.xethh.tools.jvault.cmds.pdf;

import me.xethh.tools.jvault.DevScope;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Function;

public class PdfModification {
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

    public static void modify(File file, PDDocument doc, Function<PDDocument, PDDocument> docOp) {
        final var fn = file.getName();
        final var fnTemp = fn + ".tmp";
        final var path = file.getAbsoluteFile().getParentFile().toPath();
        final var doc2 = docOp.apply(doc);
        try {
            doc2.save(path.resolve(fnTemp).toFile());
            doc2.close();
            final var delRes = path.resolve(fn).toFile().delete();
            if(!delRes) {
                DevScope.log("Failed to delete " + fnTemp);
            }
            if (path.resolve(fn).toFile().exists()) {
                throw new RuntimeException("File not deleted!");
            }
            final var rnRes = path.resolve(fnTemp).toFile().renameTo(path.resolve(fn).toFile());
            if(!rnRes) {
                DevScope.log("Failed to rename " + fnTemp);
            }
            if (path.resolve(fnTemp).toFile().exists()) {
                throw new RuntimeException("File not renamed!");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
