package me.xethh.tools.jVault.cmds.deen.sub;

import me.xethh.tools.jVault.cmds.deen.DeenObj;

import java.io.*;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Common {
    public static final Supplier<PrintStream> Out = () -> System.out;

    public static DeenObj getDeenObj(Path path, String credsEnv) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        if (!path.toFile().exists()) {
            var saltWithIV = DeenObj.genSaltWithIV();
            var deObj = DeenObj.fromLine(credsEnv, saltWithIV);
            try (var os = new FileOutputStream(path.toString());
                 var cis = deObj.encryptInputStream(new ByteArrayInputStream("".getBytes()))) {
                os.write(String.format("%s\n", saltWithIV).getBytes());
                cis.transferTo(os);
                os.flush();
            } catch (Exception ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            }
            return deObj;
        } else {
            try (var fbs = new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile())))) {
                final var line = fbs.readLine();
                return DeenObj.fromLine(credsEnv, line);
            }
        }
    }

    final public static Pattern PATTERN = Pattern.compile("(^[a-zA-Z][a-zA-Z0-9\\[\\]\\\\._-]*)=(.*)$");

    public static void SkipFirstLine(InputStream is) throws IOException {
        var first = Stream.generate(()->{
            try {
                return is.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).dropWhile(it->!it.equals((int)'\n')).findFirst();
        DebugLog.log(()->"Skipped the first line: "+first.isPresent());
    }
}
