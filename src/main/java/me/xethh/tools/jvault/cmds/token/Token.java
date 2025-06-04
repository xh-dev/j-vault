package me.xethh.tools.jvault.cmds.token;

import picocli.CommandLine;

import java.util.Objects;
import java.util.concurrent.Callable;

import static me.xethh.tools.jvault.cmds.deen.sub.Common.Out;

@CommandLine.Command(
        name = "token",
        subcommands = {GenToken.class},
        description = "manage token"
)
public class Token implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, Out.get());
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
