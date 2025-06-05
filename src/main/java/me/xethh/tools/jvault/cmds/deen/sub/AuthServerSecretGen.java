package me.xethh.tools.jvault.cmds.deen.sub;

import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import picocli.CommandLine;

import java.io.FileOutputStream;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "code-gen",
        description = "authentication server secret gen"
)
public class AuthServerSecretGen implements ConsoleOwner, Callable<Integer> {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    private boolean helpRequested;

    @CommandLine.Option(names = {"-i", "--issue-to"}, description = "issue to / app name")
    private String issueTo;

    @CommandLine.Option(names = {"-n", "--name"}, description = "name of user")
    private String name;

    @Override
    public Integer call() throws Exception {
        SecretGenerator secretGenerator = new DefaultSecretGenerator(64);
        final var gen = secretGenerator.generate();
        final var digit = 8;
        final var timePeriod = 30;
        final var algo = HashingAlgorithm.SHA512;
        final var data = new QrData.Builder()
                .secret(gen)
                .issuer(issueTo)
                .label(name)
                .algorithm(algo)
                .digits(digit)
                .period(timePeriod)
                .build();

        console().log("App: " + issueTo);
        console().log("Name:  " + name);
        console().log("Secret :  " + gen);
        console().log("Digit:   " + digit);
        console().log("Algo:  " + algo.getFriendlyName());
        console().log("period :  " + timePeriod);
        console().log(String.format("https://totp.danhersam.com/?digits=%d&period=%d&algorithm=%s&key=%s%n", digit, timePeriod, algo.getFriendlyName(), gen));

        console().log("\n\n==============\n Generated QRCode.png");
        ZxingPngQrGenerator qrGenerator = new ZxingPngQrGenerator();
        try (FileOutputStream fos = new FileOutputStream("qr.png")) {
            fos.write(qrGenerator.generate(data));
        }
        return 0;
    }
}
