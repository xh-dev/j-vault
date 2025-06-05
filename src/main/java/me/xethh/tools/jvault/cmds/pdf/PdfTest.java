package me.xethh.tools.jvault.cmds.pdf;

import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import picocli.CommandLine;

import java.util.Optional;
import java.util.concurrent.Callable;

import static me.xethh.tools.jvault.cmds.pdf.PdfModification.loadFile;

@CommandLine.Command(
        name = "test",
        description = "decrypt pdf with password"
)
public class PdfTest implements ConsoleOwner, Callable<Integer> {

    @CommandLine.ParentCommand
    private PdfManaging pdfManaging;

    @Override
    public Integer call() throws Exception {
        try {
            final var file = pdfManaging.getFile();

            if (pdfManaging.getPassword().isEmpty()) {
                final var f = loadFile(file, Optional.empty());
                if (f.isPresent()) {
                    console().log("The file is not protected by password");
                } else {
                    console().log("The file maybe protected by password");
                }
            } else {
                if (loadFile(file, Optional.empty()).isPresent()) {
                    console().log("The file is not protected by password");
                } else if (loadFile(file, pdfManaging.getPassword()).isEmpty()) {
                    console().log("The provided password is not correct");
                } else {
                    console().log("The provided password is correct");
                }
            }
        } catch (Exception e) {
            console().printStackTrace(e);
        }
        return 0;
    }
}
