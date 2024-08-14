package me.xethh.tools.jVault.cmds.deen;

import me.xethh.tools.jVault.cmds.deen.sub.*;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import static me.xethh.tools.jVault.cmds.deen.sub.Common.Out;

@CommandLine.Command(
        name = "vault",
        subcommands = {
                View.class,
                Find.class,
                Set.class,
                Unset.class
        },
        description = "allow user modify and read vault information"
)
public class Vault implements Callable<Integer>, CredentialOwner {

    public interface Consumer2{
        void handle(String line, Common.KVExtractor.KV kv, AtomicBoolean byPass) throws Exception;
    }
    public interface Consumer3{
        void handle(String line, AtomicBoolean byPass) throws Exception;
    }

    @CommandLine.Option(names = {"-c","--credential"}, defaultValue = "", description = "The credential to use, if missing, would try find env variable `x-credential` or `x_credential`")
    private String credential;
    public String getCredential() {
        return credential;
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, Out.get());
        return 0;
    }

    public void writeHeader(OutputStream os, DeenObj deObj) throws IOException {
        os.write(String.format("%s\n", deObj.fileHeader).getBytes());
        os.flush();
    }
    public void switchTempFile(Path path, Path tmpPath){
        var delRs = path.toFile().delete();
        DebugLog.log(() -> "delete file result: " + delRs);
        var renameRs = tmpPath.toFile().renameTo(path.toFile());
        DebugLog.log(() -> "rename file result: " + renameRs);
    }

    public void loopAndFindKey(
            BufferedReader isr,
            String key,
            Consumer2 matchKeyLogic,
            Consumer2 inMatchKeyLogic,
            Consumer3 notAcceptedLineLogic
    ) throws Exception {
        String line;
        while ((line = isr.readLine()) != null) {
            String finalLine = line;
            DebugLog.log(() -> "KV line: " + finalLine);
            final var kv = Common.KVExtractor.extract(line);
            //final var matcher = PATTERN.matcher(line);
            DebugLog.log(() -> "Match result: " + kv.isMatch());
            var byPass = new AtomicBoolean(false);
            if (kv.isMatch()) {
                var name = kv.getKey();
                DebugLog.log(() -> "Match name: " + name);
                if (name.equals(key)) {
                    matchKeyLogic.handle(line, kv, byPass);
                    if(byPass.get())
                        break;
                } else {
                    inMatchKeyLogic.handle(line, kv,byPass);
                    if(byPass.get())
                        break;
                }
            } else {
                notAcceptedLineLogic.handle(line,byPass);
                if(byPass.get())
                    break;
            }
        }
    }
}
