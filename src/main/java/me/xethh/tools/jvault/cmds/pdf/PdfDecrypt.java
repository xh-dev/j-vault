package me.xethh.tools.jvault.cmds.pdf;

import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import picocli.CommandLine;

import java.util.Optional;
import java.util.concurrent.Callable;

import static me.xethh.tools.jvault.cmds.pdf.PdfModification.loadFile;

@CommandLine.Command(
        name = "unset-password",
        description = "decrypt pdf with password"
)
public class PdfDecrypt implements ConsoleOwner, Callable<Integer> {
    @CommandLine.ParentCommand
    private PdfManaging pdfManaging;

    @CommandLine.Option(names = {"-p", "--password"}, required = true, defaultValue = "", description = "if present, will also test if the password correct")
    private String password;

    @Override
    public Integer call() throws Exception {
        var ifNoPassPdfModification = loadFile(pdfManaging.getFile(), Optional.empty());
        if (ifNoPassPdfModification.isPresent()) {
            console().log("The file is not encrypted");
        } else {
            ifNoPassPdfModification = loadFile(pdfManaging.getFile(), Optional.of(password));
            if (ifNoPassPdfModification.isEmpty()) {
                console().log("The password is not correct");
            } else {
                PdfModification.modify(pdfManaging.getFile(), ifNoPassPdfModification.get(), pdDocument -> {
                    pdDocument.setAllSecurityToBeRemoved(true);
                    return pdDocument;
                });
            }
        }
        return 0;
    }
}
