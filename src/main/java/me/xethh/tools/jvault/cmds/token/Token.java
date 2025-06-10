package me.xethh.tools.jvault.cmds.token;

import me.xethh.tools.jvault.exceptionhandling.CommonHandle;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import picocli.CommandLine;

import java.util.Objects;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "token",
        subcommands = {GenToken.class},
        description = "manage token"
)
public class Token implements ConsoleOwner, Callable<Integer> {

    public static boolean validate(String line) {
        CommonHandle.throwExceptionIfNotExpected(Objects.isNull(line) || line.isEmpty(), "credential is empty");
        if (Objects.isNull(line) || line.isEmpty()) {
            throw new RuntimeException();
        }
        var creds = line.split(":");
        CommonHandle.throwExceptionIfNotExpected(creds.length != 2, "credential not corrected formatted");
        return true;
    }

    @Override
    public Integer call() throws Exception {
        CommandLine.usage(this, console().getDisplay());
        return 0;
    }

}
