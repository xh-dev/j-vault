package me.xethh.tools.jVault.cmds.token;

import picocli.CommandLine;

import java.util.Objects;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "token",
        subcommands = {GenToken.class}
)
public class Token implements Callable<Integer> {
    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static boolean validate(String line){
        if (Objects.isNull(line) || line.isEmpty()) {
            throw new RuntimeException("credential is empty");
        }
        var creds = line.split(":");
        if (creds.length != 2) {
            throw new RuntimeException("credential not corrected formatted");
        }
        return true;
    }

}
