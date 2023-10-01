package me.xethh.tools.jVault.cmds.deen;

import me.xethh.tools.jVault.cmds.deen.sub.*;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;

import static me.xethh.tools.jVault.cmds.deen.sub.Common.PATTERN;

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
public class Deen implements Callable<Integer>, CredentialOwner{
    public interface Consumer2{
        void handle(String line, Matcher matcher, AtomicBoolean byPass) throws Exception;
    }
    public interface Consumer3{
        void handle(String line, AtomicBoolean byPass) throws Exception;
    }

    @CommandLine.Option(names = {"-c","--credential"}, defaultValue = "", description = "The credential to use, if missing, would try find env variable `x-credential`")
    private String credential;
    public String getCredential() {
        return credential;
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
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
            final var matcher = PATTERN.matcher(line);
            DebugLog.log(() -> "Match result: " + matcher.matches());
            var byPass = new AtomicBoolean(false);
            if (matcher.matches()) {
                var name = matcher.group(1);
                DebugLog.log(() -> "Match name: " + matcher.group(1));
                if (name.equals(key)) {
                    matchKeyLogic.handle(line, matcher, byPass);
                    if(byPass.get())
                        break;
                } else {
                    inMatchKeyLogic.handle(line, matcher,byPass);
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
