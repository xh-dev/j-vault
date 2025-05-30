package me.xethh.tools.jVault.cmds.deen.sub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import me.xethh.tools.jVault.cmds.deen.Vault;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import static me.xethh.tools.jVault.cmds.deen.sub.Common.Out;
import static me.xethh.tools.jVault.cmds.deen.sub.Common.SkipFirstLine;

@CommandLine.Command(
        name = "rest-server",
        description = "restful access to the j-vault"
)
public class RestServer implements Callable<Integer> {
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    private boolean helpRequested;

    @CommandLine.Option(names = {"-f", "--file"}, defaultValue = "vault.kv", description = "The file to encrypt")
    private File file;

    @CommandLine.Option(names = {"-p", "--port"}, defaultValue = "8000", description = "the port to use for setup restful server", required = true)
    private String port;

    @CommandLine.ParentCommand
    private Vault deen;


    private static Map<String, String> parseQueryParams(String queryString) {
        Map<String, String> queryParams = new HashMap<>();
        if (queryString == null || queryString.isEmpty()) {
            return queryParams;
        }

        // Split the query string by '&' to get individual key=value pairs
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            // Split each pair by '='
            int idx = pair.indexOf("=");
            if (idx > 0) { // Ensure there's a key and a value
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                queryParams.put(key, value);
            } else if (idx == -1 && !pair.isEmpty()) { // Handle parameters without a value (e.g., "?flag")
                String key = URLDecoder.decode(pair, StandardCharsets.UTF_8);
                queryParams.put(key, ""); // Assign an empty string value
            }
        }
        return queryParams;
    }

    @Override
    public Integer call() throws Exception {
        var path = file.toPath().toAbsolutePath();

        var credsEnv = deen.finalCredential();
        var deObj = Common.getDeenObj(path, credsEnv);

        var map = new HashMap<String, String>();

        try (
                var is = new FileInputStream(path.toFile());
        ) {
            SkipFirstLine(is);
            try (
                    var isr = new BufferedReader(new InputStreamReader(deObj.decryptInputStream(is)));
            ) {
                String line;
                while ((line = isr.readLine()) != null) {
                    final var kv = Common.KVExtractor.extract(URLDecoder.decode(line, StandardCharsets.UTF_8));
                    map.put(kv.getKey(), kv.getValue());
                }
            }
        }


        try {
            final var portInt = Integer.valueOf(port);
            final var om = new ObjectMapper();

            HttpServer server = HttpServer.create(new InetSocketAddress(portInt), 0);
            server.createContext("/keys", exchange -> {
                if(exchange.getRequestMethod().toLowerCase().equals("get")) {
                    final var rs = om.writeValueAsString(map.keySet());
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, rs.length());
                    var os = exchange.getResponseBody();
                    os.write(rs.getBytes(StandardCharsets.UTF_8));
                    os.close();
                } else {
                    exchange.sendResponseHeaders(405, 0);
                    var os = exchange.getResponseBody();
                    os.close();
                }
            });
            server.createContext("/kv", exchange -> {
                if(exchange.getRequestMethod().equalsIgnoreCase("get")) {
                    final var p = exchange.getRequestURI().getPath();
                    final var pattern = Pattern.compile("/kv/([\\w+][\\w+-_0-9]*)");
                    final var matcher = pattern.matcher(p);
                    if(matcher.matches()) {
                        final var k=matcher.group(1);
                        if(map.containsKey(k)){
                            exchange.getResponseHeaders().set("Content-Type", "text/plain");
                            final var v = map.get(k);
                            final var body = exchange.getResponseBody();
                            exchange.sendResponseHeaders(200, v.length());
                            body.write(v.getBytes(StandardCharsets.UTF_8));
                            body.close();
                        } else {
                            exchange.sendResponseHeaders(404, 0);
                            var os = exchange.getResponseBody();
                            os.close();
                        }
                    } else {
                        exchange.sendResponseHeaders(405, 0);
                        var os = exchange.getResponseBody();
                        os.close();
                    }
                } else {
                    exchange.sendResponseHeaders(405, 0);
                    var os = exchange.getResponseBody();
                    os.close();
                }
            });
            server.createContext("/kvs", exchange -> {
                if(exchange.getRequestMethod().toLowerCase().equals("get")) {
                    final var q = parseQueryParams(exchange.getRequestURI().getQuery());
                    final var mapResult = om.writeValueAsString(map);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    final var body = exchange.getResponseBody();
                    exchange.sendResponseHeaders(200, mapResult.length());
                    body.write(mapResult.getBytes(StandardCharsets.UTF_8));
                    body.close();
                } else {
                    exchange.sendResponseHeaders(405, 0);
                    var os = exchange.getResponseBody();
                    os.close();
                }
            });
            server.start();
            Log.msg(()->"Restful server started on port " + portInt);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    public static void main(String[] args) {
        var cmd = new CommandLine(new RestServer());
        Out.get().println(cmd.execute(args));
    }
}
