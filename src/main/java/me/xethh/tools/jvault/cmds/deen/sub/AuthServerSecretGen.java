package me.xethh.tools.jvault.cmds.deen.sub;

import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import picocli.CommandLine;

import java.io.FileOutputStream;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "code-gen",
        description = "authentication server secret gen"
)
public class AuthServerSecretGen implements Callable<Integer> {
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

        System.out.println("App: "+issueTo);
        System.out.println("Name: "+name);
        System.out.println("Secret: "+gen);
        System.out.println("Digit: "+digit);
        System.out.println("Algo: "+algo.getFriendlyName());
        System.out.println("period: "+timePeriod);
        System.out.println(String.format("https://totp.danhersam.com/?digits=%d&period=%d&algorithm=%s&key=%s", digit, timePeriod, algo.getFriendlyName(), gen));

        System.out.println("\n\n==============\n Generated QRCode.png");
        ZxingPngQrGenerator qrGenerator = new ZxingPngQrGenerator();
        new FileOutputStream("QRCode.png").write(
                qrGenerator.generate(data)
        );
        return 0;
    }
}
