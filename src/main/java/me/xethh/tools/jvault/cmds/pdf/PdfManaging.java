package me.xethh.tools.jvault.cmds.pdf;

import me.xethh.tools.jvault.display.Console;
import me.xethh.tools.jvault.exceptionhandling.CommonHandle;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.Optional;
import java.util.concurrent.Callable;

import static me.xethh.tools.jvault.cmds.pdf.NativeHandling.*;

@CommandLine.Command(
        name = "pdf",
        subcommands = {PdfEncrypt.class, PdfDecrypt.class, PdfTest.class},
        description = "handle pdf encrypt and decryption"
)
public class PdfManaging implements ConsoleOwner, Callable<Integer> {

    static {
        if (System.getProperty(OS_NAME).toLowerCase().contains(OS_WINDOWS)) {
            log("running on windows");
        } else {
            log("not running on Windows");
            final var tmpDir = System.getProperty("java.io.tmpdir");
            final var tmpJVaultFolder = Path.of(tmpDir).toAbsolutePath().resolve("tmp-j-vault");
            System.setProperty("java.library.path", tmpJVaultFolder.toString());
        }
        loadNativeLib();
        log("java.library.path: " + System.getProperty("java.library.path"));
    }

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    private boolean helpRequested;
    @CommandLine.Option(names = {"-f", "--file"}, required = true, description = "file to use")
    private File file;
    @CommandLine.Option(names = {"-p", "--password"}, description = "password to unlock the zip file in case encrypted")
    private Optional<String> password;

    static void log(String msg) {
        Console.getConsole().debug(msg);
    }



    @Override
    public Integer call() throws Exception {
        return 0;
    }

    public File getFile() {
        return file;
    }

    public Optional<String> getPassword() {
        return password;
    }
}
