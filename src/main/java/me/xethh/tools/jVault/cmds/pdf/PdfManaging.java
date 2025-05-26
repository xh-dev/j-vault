package me.xethh.tools.jVault.cmds.pdf;

import picocli.CommandLine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

@CommandLine.Command(
        name = "pdf",
        subcommands = {PdfEncrypt.class, PdfDecrypt.class, PdfTest.class},
        description = "handle pdf encrypt and decryption"
)
public class PdfManaging implements Callable<Integer> {
    final static boolean debugging = Optional.ofNullable(System.getenv("DEV")).isPresent();
    final static boolean deleteOnExit = Optional.ofNullable(System.getenv("DELETE_ON_EXIT")).isPresent();
    final static boolean testLibExists = Optional.ofNullable(System.getenv("TEST_LIB_EXISTS")).isPresent();

    static void log(String msg) {
        if(debugging){
            System.out.println(msg);
        }
    }

    static {
        if(System.getProperty("os.name").toLowerCase().contains("windows")) {
            log("running on windows");
        } else {
            log("not running on Windows");
            final var tmpDir = System.getProperty("java.io.tmpdir");
            final var tmpJVaultFolder  =Path.of(tmpDir).toAbsolutePath().resolve("tmp-j-vault");
            System.setProperty("java.library.path", tmpJVaultFolder.toString());
        }
        log("java.library.path: "+System.getProperty("java.library.path"));
    }


    public static boolean libExists(String libName) {
        log(String.format("Testing lib: %s for existence", libName));
        try {
            System.loadLibrary(libName);
            log(String.format("lib [%s] exists", libName));
            return true;
        } catch (UnsatisfiedLinkError e) {
            log(String.format("lib [%s] not exists", libName));
            return false;
        }
    }

    public static boolean setFileHidden(Path path){
        try {
            final var view = Files.getFileAttributeView(path, DosFileAttributeView.class);
            view.setHidden(true);
            if(Files.isHidden(path)) {
                log("File set hidden now");
                return true;
            } else {
                log("File fail to set hidden");
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void loadNativeLib(){
        log("Running under graalvm");
        if(System.getProperty("org.graalvm.nativeimage.imagecode") != null) {
            try {

                final var res = PdfManaging.class.getClassLoader().getResourceAsStream("native.txt");
                if(res == null){
                    throw new RuntimeException("native.txt not found");
                }
                final var nativeTxt = new String(res.readAllBytes(), StandardCharsets.UTF_8);
                final var tmpDir = System.getProperty("java.io.tmpdir");
                final var tmpJVaultFolder  = System.getProperty("os.name").toLowerCase().contains("windows")?Path.of(".").toAbsolutePath():Path.of(tmpDir).toAbsolutePath().resolve("tmp-j-vault");
                tmpJVaultFolder.toFile().mkdirs();

                log("Native library listing file: ");
                log(nativeTxt);
                log("");

                Stream.of(nativeTxt.replace("\r\n", "\n").split("\n")).filter(s -> !s.isEmpty())
                        .filter(libFileName -> !libExists(libFileName.substring(0, libFileName.length() - 4)))
                        .forEach(libFileName -> {
                    final var s = PdfManaging.class.getClassLoader().getResourceAsStream(libFileName);
                    if (s != null) {
                        log(String.format("%s exists in executable %n", libFileName));
                        if(!tmpJVaultFolder.resolve(libFileName).toFile().exists()) {
                            log("tmp jvault file does not exist: "+libFileName);
                            try {
                                new FileOutputStream(tmpJVaultFolder.resolve(libFileName).toFile()).write(s.readAllBytes());
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            if(System.getProperty("os.name").toLowerCase().contains("windows")) {
                                setFileHidden(tmpJVaultFolder.resolve(libFileName));
                            }
                        } else {
                            log("tmp jvault file already exist: "+libFileName);
                        }
                    } else {
                        log(String.format("%s not exists%n", libFileName));
                        throw new RuntimeException(String.format("%s not exists%n", libFileName));
                    }

                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Not running under native-image");
        }
    }

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested;

    @CommandLine.Option(names = { "-f", "--file" }, required = true, description = "file to use")
    private File file;

    @CommandLine.Option(names = { "-p", "--password" }, description = "password to unlock the zip file in case encrypted")
    private Optional<String> password;

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
