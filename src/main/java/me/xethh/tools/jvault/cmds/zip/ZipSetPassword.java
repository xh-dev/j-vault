package me.xethh.tools.jvault.cmds.zip;

import me.xethh.tools.jvault.Log;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@CommandLine.Command(
        name = "set-password",
        description = "set password to existing zip file"
)
public class ZipSetPassword implements Callable<Integer> {

    @CommandLine.ParentCommand
    private ZipManaging zipManaging;

    @CommandLine.Option(names = { "-p", "--password" },required = true,  defaultValue = "", description = "if present, will also test if the password correct")
    private String password;

    @Override
    public Integer call() throws Exception {
        if(password.isEmpty()){
            System.err.println("password is empty");
        } else {
            final var zipFile = zipManaging.getZipFile();
            final var newZipFileName = Path.of(zipManaging.getFile().toString()+".tmp");
            final var newZipFile = new ZipFile(newZipFileName.toFile(), password.toCharArray());

            final var getZipParameter = (Supplier<ZipParameters>) ()->{
                ZipParameters newZipParameters = new ZipParameters();
                newZipParameters.setEncryptFiles(true);
                newZipParameters.setEncryptionMethod(EncryptionMethod.AES);
                newZipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
                return newZipParameters;
            };

            for(var zippedFile : zipFile.getFileHeaders()){
                final var is = zipFile.getInputStream(zippedFile);
                final var parameters = getZipParameter.get();
                parameters.setFileNameInZip(zippedFile.getFileName());
                parameters.setFileComment(zippedFile.getFileComment());
                parameters.setLastModifiedFileTime(zippedFile.getLastModifiedTime());
                newZipFile.addStream(is, parameters);
            }
            zipFile.close();
            zipManaging.getFile().delete();
            newZipFileName.toFile().renameTo(zipManaging.getFile());
            Log.log(String.format("file [%s] is now zip with password.", zipManaging.getFile().getName()));
        }
        return 0;
    }
}
