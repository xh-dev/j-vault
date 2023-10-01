package me.xethh.tools.jVault.cmds.token;

import me.xethh.tools.jVault.cmds.deen.DeenObj;
import picocli.CommandLine;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "gen",
        description = "generate token"

)
public class GenToken implements Callable<Integer> {
    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested;
    @CommandLine.Option(names = {"-p","--password"}, description = "`password` to generate", defaultValue = "")
    private String password;

    @Override
    public Integer call() {
        if(password.isBlank()) {
            var rand = new SecureRandom();
            byte[] bs= new byte[16];
            rand.nextBytes(bs);
            password = Base64.getEncoder().encodeToString(bs);
        }
        System.out.println(DeenObj.getFullPassword(password));
        return 0;
    }

    public static void main(String[] args) {
        var cmd = new CommandLine(new GenToken());
        System.out.println(cmd.execute(args));
    }

}
