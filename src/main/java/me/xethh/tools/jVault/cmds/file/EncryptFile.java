package me.xethh.tools.jVault.cmds.file;

import me.xethh.tools.jVault.cmds.deen.DeenObj;
import me.xethh.tools.jVault.cmds.token.GenToken;
import me.xethh.tools.jVault.cmds.token.Token;
import picocli.CommandLine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.Channels;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "encrypt",
        description = "Encrypt a specified file with j-vault encryption format"
)
public class EncryptFile implements Callable<Integer> {
    @CommandLine.ParentCommand
    private FileCommand command;

    @CommandLine.Option(
            names = {"-f","--in-file"},
            description = "file to be decrypt or encrypt",
            required = true
    )
    private File infile;

    @CommandLine.Option(
            names = {"-o", "--out-file"},
            description = "output file",
            required = true
    )

    private File outFile;
    @Override
    public Integer call() throws Exception {
        var creds = command.finalCredential();
        var deObj = DeenObj.fromLine(creds, DeenObj.genSaltWithIV());
        try (
                var is = deObj.encryptInputStream(new FileInputStream(infile));
                var os = new FileOutputStream(outFile)
        ) {
            os.write(String.format("%s\n", deObj.fileHeader).getBytes());
            os.flush();
            is.transferTo(os);
        }
        return 0;
    }
}
