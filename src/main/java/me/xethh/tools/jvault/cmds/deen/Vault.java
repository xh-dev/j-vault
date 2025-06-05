package me.xethh.tools.jvault.cmds.deen;

import me.xethh.tools.jvault.authserv.AuthServerClient;
import me.xethh.tools.jvault.cmds.deen.sub.*;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

@CommandLine.Command(
        name = "vault",
        subcommands = {
                View.class,
                Find.class,
                Set.class,
                Unset.class,
                RestServer.class,
        },
        description = "allow user modify and read vault information"
)
public class Vault implements ConsoleOwner, Callable<Integer>, CredentialOwner {

    public interface Consumer2{
        void handle(String line, Common.KVExtractor.KV kv, AtomicBoolean byPass) throws Exception;
    }
    public interface Consumer3{
        void handle(String line, AtomicBoolean byPass) throws Exception;
    }

    @CommandLine.Option(names = {"-c","--credential"}, defaultValue = "", description = "The credential to use, if missing, would try find env variable `x-credential` or `x_credential`")
    private String credential;

    @CommandLine.Option(names = {"--auth-server"}, defaultValue = "", description = "The authentication server`")
    private String authServer;

    @CommandLine.Option(names = {"--user"}, defaultValue = "", description = "The user`")
    private String user="";

    public String getCredential() {
        if(!authServer.equalsIgnoreCase("")) {
            if(authServer.endsWith("/")){
                authServer = authServer.substring(0,authServer.length()-1);
            }
            var as = new AuthServerClient(){
                @Override
                public String authServer() {
                    return authServer;
                }
            };

            user = Optional.ofNullable(user)
                    .flatMap(t-> t.equalsIgnoreCase("")?Optional.empty():Optional.of(t))
                    .orElseThrow(()->new RuntimeException("user is entered"));
            return as.getCode(user);
        } else {
            return credential;
        }
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, console().getDisplay());
        return 0;
    }

    public void writeHeader(OutputStream os, DeenObj deObj) throws IOException {
        os.write(String.format("%s\n", deObj.fileHeader).getBytes());
        os.flush();
    }
    public void switchTempFile(Path path, Path tmpPath){
        var delRs = path.toFile().delete();
        Log.debug(() -> "delete file result: " + delRs);
        var renameRs = tmpPath.toFile().renameTo(path.toFile());
        Log.debug(() -> "rename file result: " + renameRs);
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
            Log.debug(() -> "KV line: " + finalLine);
            final var kv = Common.KVExtractor.extract(line);
            //final var matcher = PATTERN.matcher(line);
            Log.debug(() -> "Match result: " + kv.isMatch());
            var byPass = new AtomicBoolean(false);
            if (kv.isMatch()) {
                var name = kv.getKey();
                Log.debug(() -> "Match name: " + name);
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
