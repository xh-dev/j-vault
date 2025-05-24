package me.xethh.tools.jVault.cmds.zip;

import picocli.CommandLine;

import java.io.File;
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

    @Override
    public Integer call() throws Exception {
        return 0;
    }

    public File getFile() {
        return file;
    }
}
