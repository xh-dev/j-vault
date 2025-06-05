package me.xethh.tools.jvault.cmds.deen.sub;

import me.xethh.tools.jvault.cmds.deen.DeenObj;

import java.io.*;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Common {
    private Common() {
        throw new IllegalStateException("Not expected to be instantiated");
    }

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

    public static void SkipFirstLine(InputStream is) throws IOException {
        var first = Stream.generate(() -> {
            try {
                return is.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).dropWhile(it -> !it.equals((int) '\n')).findFirst();
        Log.debug(() -> "Skipped the first line: " + first.isPresent());
    }

    public static class KVExtractor {
        final private static Pattern PATTERN = Pattern.compile("(^[a-zA-Z][a-zA-Z0-9\\[\\]\\\\._-]*)=(.*)$");

        public static KV extract(String line) {
            final var matcher = PATTERN.matcher(line);
            if (!matcher.matches()) {
                return new KV("", "", false);
            } else {
                return new KV(matcher.group(1), matcher.group(2), true);
            }
        }

        public static class KV {
            private final String key;
            private final String value;
            private final boolean match;

            public KV(String key, String value, boolean match) {
                this.key = key;
                this.value = value;
                this.match = match;
            }

            public String getKey() {
                return key;
            }

            public String getValue() {
                return value;
            }

            public boolean isMatch() {
                return match;
            }
        }


    }
}
