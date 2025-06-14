package me.xethh.tools.jvault.cmds.pdf;

import me.xethh.tools.jvault.display.Console;
import me.xethh.tools.jvault.exceptionhandling.CommonHandle;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.DosFileAttributeView;
import java.util.stream.Stream;

public class NativeHandling {
    public static final String OS_NAME="os.name";
    public static final String OS_WINDOWS="windows";
    static void log(String msg) {
        Console.getConsole().debug(msg);
    }
    public static boolean setFileHidden(Path path) {
        return CommonHandle.tryCatchThrow(()->{
            final var view = Files.getFileAttributeView(path, DosFileAttributeView.class);
            view.setHidden(true);
            if (Files.isHidden(path)) {
                log("File set hidden now");
                return true;
            } else {
                log("File fail to set hidden");
                return false;
            }
        });
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
    public static void loadNativeLib() {
        log("Running under graalvm");
        if (System.getProperty("org.graalvm.nativeimage.imagecode") != null) {
            try (
                    var res = PdfManaging.class.getClassLoader().getResourceAsStream("native.txt");
            ) {
                if (res == null) {
                    throw new RuntimeException("native.txt not found");
                }
                final var nativeTxt = new String(res.readAllBytes(), StandardCharsets.UTF_8);
                final var tmpDir = System.getProperty("java.io.tmpdir");
                final var tmpJVaultFolder = System.getProperty(OS_NAME).toLowerCase().contains(OS_WINDOWS) ? Path.of(".").toAbsolutePath() : Path.of(tmpDir).toAbsolutePath().resolve("tmp-j-vault");
                if (!tmpJVaultFolder.toFile().mkdirs()) {
                    Console.getConsole().log("failed to create tmp dir");
                }

                log("Native library listing file: ");
                log(nativeTxt);
                log("");

                Stream.of(nativeTxt.replace("\r\n", "\n").split("\n")).filter(s -> !s.isEmpty())
                        .filter(libFileName -> !libExists(libFileName.substring(0, libFileName.length() - 4)))
                        .forEach(libFileName -> {
                            try (
                                    var s = PdfManaging.class.getClassLoader().getResourceAsStream(libFileName);
                            ) {
                                if (s != null) {
                                    log(String.format("%s exists in executable %n", libFileName));
                                    if (!tmpJVaultFolder.resolve(libFileName).toFile().exists()) {
                                        log("tmp jvault file does not exist: " + libFileName);
                                        try (

                                                var fos = new FileOutputStream(tmpJVaultFolder.resolve(libFileName).toFile());
                                        ) {
                                            fos.write(s.readAllBytes());
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                        if (System.getProperty(OS_NAME).toLowerCase().contains(OS_WINDOWS)) {
                                            setFileHidden(tmpJVaultFolder.resolve(libFileName));
                                        }
                                    } else {
                                        log("tmp jvault file already exist: " + libFileName);
                                    }
                                } else {
                                    log(String.format("%s not exists%n", libFileName));
                                    throw new RuntimeException(String.format("%s not exists%n", libFileName));
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                        });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            log("Not running under native-image");
        }
    }
}
