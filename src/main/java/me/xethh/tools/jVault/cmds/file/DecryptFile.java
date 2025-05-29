package me.xethh.tools.jVault.cmds.file;

import me.xethh.tools.jVault.cmds.deen.DeenObj;
import me.xethh.tools.jVault.cmds.deen.sub.DebugLog;
import picocli.CommandLine;

import java.io.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static me.xethh.tools.jVault.cmds.deen.sub.Common.Out;

@CommandLine.Command(
        name = "decrypt",
        description = "Decrypt a specified file with j-vault encryption format"
)
public class DecryptFile implements Callable<Integer> {
    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
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
            names = {"--stdout"},
            description = "output result to stdout"
    )
    private boolean toOs;

    @CommandLine.Option(
            names = {"-f","--in-file"},
            description = "file to be decrypt or encrypt",
            required = true
    )
    private File inFile;
    @CommandLine.Option(
            names = {"-o", "--out-file"},
            description = "output file, ignored if `-o` or `--stdout` exists"
    )
    private File outFile;

    @Override
    public Integer call() throws Exception {
        var creds = command.finalCredential();

        try (var is = new FileInputStream(inFile)) {
            var bArray = Stream.generate(()->{
                try{
                    return is.read();
                } catch (Exception ex){
                    throw new RuntimeException(ex.getMessage(), ex);
                }
            }).takeWhile(it-> '\n' != it).collect(Collectors.toList());
            byte[] ba = new byte[bArray.size()];
            IntStream.range(0, bArray.size())
                    .forEach(i->{
                        ba[i]=bArray.get(i).byteValue();
                    });
            var deObj = DeenObj.fromLine(creds, new String(ba));
            if (toOs) {
                String line;
                try (var br = new BufferedReader(new InputStreamReader(deObj.decryptInputStream(is)))) {
                    while ((line = br.readLine()) != null) {
                        Out.get().println(line);
                    }
                }
            } else {
                if(outFile==null){
                    if(inFile.toString().endsWith(".crypt")){
                        outFile = new File(inFile.toPath().toAbsolutePath().getParent().toFile(), inFile.getName().substring(0, inFile.getName().length()-".crypt".length()));
                    } else {
                        throw new RuntimeException("Please specify output file");
                    }
                }
                try( var os = new FileOutputStream(outFile) ){
                    deObj.decryptInputStream(is).transferTo(os);
                }
                if(!keep){
                    try{
                        is.close();
                    } catch (Exception ex){
                        throw new RuntimeException(ex.getMessage(), ex);
                    }
                    boolean rs = inFile.delete();
                    DebugLog.log(()->"delete file result: " + rs);
                }
            }
        }
        return 0;
    }
}
