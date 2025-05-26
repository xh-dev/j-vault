package me.xethh.tools.jVault.cmds.zip;

import net.lingala.zip4j.ZipFile;
import picocli.CommandLine;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "zip",
        subcommands = {ZipSetPassword.class, ZipUnSetPassword.class},
        description = "handle zip file"
)
public class ZipManaging implements Callable<Integer> {
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

    public ZipFile getZipFile() {
        if(password.isPresent()) {
            return new ZipFile(getFile(),password.get().toCharArray());
        } else {
            return new ZipFile(getFile());
        }
    }
}
