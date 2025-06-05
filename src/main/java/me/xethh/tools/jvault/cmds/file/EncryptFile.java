package me.xethh.tools.jvault.cmds.file;

import me.xethh.tools.jvault.cmds.deen.DeenObj;
import me.xethh.tools.jvault.cmds.deen.sub.Log;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Optional;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "encrypt",
        description = "Encrypt a specified file with j-vault encryption format"
)
public class EncryptFile implements Callable<Integer> {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    private boolean helpRequested;
    @CommandLine.ParentCommand
    private FileCommand command;

    @CommandLine.Option(
            names = {"-k", "--keep"},
            description = "self destruct file after encrypt",
            defaultValue = "false"
    )
    private boolean keep;

    @CommandLine.Option(
            names = {"-f", "--in-file"},
            description = "file to be decrypt or encrypt",
            required = true
    )
    private File infile;

    @CommandLine.Option(
            names = {"-o", "--out-file"},
            description = "output file"
    )
    private File outFile = null;

    @Override
    public Integer call() throws Exception {
        var creds = command.finalCredential();
        var deObj = DeenObj.fromLine(creds, DeenObj.genSaltWithIV());
        outFile = Optional.ofNullable(outFile)
                .orElseGet(() -> new File(infile.toPath().toAbsolutePath().getParent().toFile(), infile.getName() + ".crypt"))
        ;
        try (
                var is = deObj.encryptInputStream(new FileInputStream(infile));
                var os = new FileOutputStream(outFile)
        ) {
            os.write(String.format("%s\n", deObj.fileHeader).getBytes());
            os.flush();
            is.transferTo(os);
        }

        if (!keep) {
            boolean rs = infile.delete();
            Log.debug(() -> "delete file result: " + rs);
        }
        return 0;
    }
}
