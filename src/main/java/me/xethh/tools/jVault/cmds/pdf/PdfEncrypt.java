package me.xethh.tools.jVault.cmds.pdf;

import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import picocli.CommandLine;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Callable;

import static me.xethh.tools.jVault.cmds.pdf.PdfModification.loadFile;

@CommandLine.Command(
        name = "set-password",
        description = "encrypt pdf with password"
)
public class PdfEncrypt implements Callable<Integer> {

    @CommandLine.ParentCommand
    private PdfManaging pdfManaging;

    //@CommandLine.Option(names = {"--existing-password" }, defaultValue = "", description = "if present, will also test if the password correct")
    //private String oldPassword;

    @CommandLine.Option(names = { "-p", "--password" },required = true,  defaultValue = "", description = "if present, will also test if the password correct")
    private String password;

    @CommandLine.Option(names = { "-u", "--user-password" }, required = true, defaultValue = "", description = "if present, will also test if the password correct")
    private String userPassword;

    @Override
    public Integer call() throws Exception {
        PdfManaging.loadNativeLib();
        if(password.isEmpty()){
            System.err.println("password is empty");
        } else {
            if(pdfManaging.getPassword().isEmpty()){
                final var unlockedFile = loadFile(pdfManaging.getFile(), Optional.empty());
                if(unlockedFile.isPresent()){
                    final var doc = unlockedFile.get();
                    PdfModification.modify(pdfManaging.getFile(), doc, docTemp->{
                        try{
                            final var curPerm = docTemp.getCurrentAccessPermission();
                            final var spp = new StandardProtectionPolicy(password, userPassword, curPerm);
                            spp.setPreferAES(true);
                            spp.setEncryptionKeyLength(256);
                            docTemp.protect(spp);
                            return docTemp;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    System.err.printf("Fail to open pdf file[%s], the file maybe locked by password or the file is corrupted %n");
                }
            } else {
                final var unlockedFile = loadFile(pdfManaging.getFile(), pdfManaging.getPassword());
                if(unlockedFile.isPresent()){
                    final var doc = unlockedFile.get();
                    PdfModification.modify(pdfManaging.getFile(), doc, docTemp->{
                        try{
                            final var curPerm = docTemp.getCurrentAccessPermission();
                            final var spp = new StandardProtectionPolicy(password, userPassword, curPerm);
                            spp.setPreferAES(true);
                            spp.setEncryptionKeyLength(256);
                            docTemp.protect(spp);
                            return docTemp;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    System.err.printf("Fail to open pdf file[%s], the file maybe locked by password or the file is corrupted %n");
                }
            }
        }
        return 0;
    }
}
