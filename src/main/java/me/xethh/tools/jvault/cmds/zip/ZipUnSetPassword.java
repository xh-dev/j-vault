package me.xethh.tools.jvault.cmds.zip;

import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

@CommandLine.Command(
        name = "unset-password",
        description = "unset password to existing zip file"
)
public class ZipUnSetPassword implements ConsoleOwner, Callable<Integer> {
    @CommandLine.ParentCommand
    private ZipManaging zipManaging;

    @Override
    public Integer call() throws Exception {
        final var zipFile = zipManaging.getZipFile();
        final var newZipFileName = Path.of(zipManaging.getFile().toString() + ".tmp");
        final var newZipFile = new ZipFile(newZipFileName.toFile());

        final var getZipParameter = (Supplier<ZipParameters>) () -> {
            ZipParameters newZipParameters = new ZipParameters();
            newZipParameters.setEncryptFiles(false);
            newZipParameters.setEncryptionMethod(EncryptionMethod.AES);
            newZipParameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            return newZipParameters;
        };

        for (var zippedFile : zipFile.getFileHeaders()) {
            final var is = zipFile.getInputStream(zippedFile);
            final var parameters = getZipParameter.get();
            parameters.setFileNameInZip(zippedFile.getFileName());
            parameters.setFileComment(zippedFile.getFileComment());
            parameters.setLastModifiedFileTime(zippedFile.getLastModifiedTime());
            newZipFile.addStream(is, parameters);
        }
        zipFile.close();
        console().logIf(!zipManaging.getFile().delete(), "Failed to delete zip file");
        console().logIf(!newZipFileName.toFile().renameTo(zipManaging.getFile()), "Failed to rename zip file");
        console().log(String.format("file [%s] is now zip without password.", zipManaging.getFile().getName()));
        return 0;
    }
}
