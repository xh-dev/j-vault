package me.xethh.tools.jvault.cmds.deen.sub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.vavr.control.Try;
import me.xethh.tools.jvault.cmds.deen.Vault;
import me.xethh.tools.jvault.interfaces.ConsoleOwner;
import org.apache.commons.codec.digest.DigestUtils;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.regex.Pattern;

import static me.xethh.tools.jvault.cmds.authserver.HandlerFactory.Const.*;
import static me.xethh.tools.jvault.cmds.deen.sub.Common.skipFirstLine;

@CommandLine.Command(
        name = "over-http",
        description = "the restful access to the j-vault"
)
public class RestServer implements ConsoleOwner, Callable<Integer> {
    private static final String DEFAULT_PORT = "7910";
    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    private boolean helpRequested;
    @CommandLine.Option(names = {"-f", "--file"}, defaultValue = "vault.kv", description = "The file to encrypt")
    private File file;
    @CommandLine.Option(names = {"-p", "--port"}, defaultValue = DEFAULT_PORT, description = "the port to be used for setup restful server, default as `" + DEFAULT_PORT + "`", required = true)
    private String port;

    @CommandLine.Option(names = {"--harden"}, description = "If defined, a secure token will be generate.")
    private boolean lessSecure;

    @CommandLine.Option(names = {"-s", "--secure"}, description = "If defined, a secure token will be generate when start up the server.")
    private boolean genToken;

    @CommandLine.ParentCommand
    private Vault deen;

    public void handleError(HttpExchange exchange, int status) {
        Try.of(()->{
            exchange.sendResponseHeaders(status, 0);
            var os = exchange.getResponseBody();
            os.close();
            return true;
        }).getOrElseThrow(()->new RuntimeException("Fail to close response"));
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
            skipFirstLine(is);
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
            final var portInt = Integer.parseInt(port);
            final var om = new ObjectMapper();

            var tokenGen = genToken ? UUID.randomUUID().toString() : null;
            if (tokenGen == null && lessSecure) {
                tokenGen = DigestUtils.md5Hex(String.format("http://0.0.0.0:%s", port));
                genToken = true;
            }

            HttpServer server = HttpServer.create(new InetSocketAddress(portInt), 0);

            console().log("Token: \n" + tokenGen + "\n---\n\n");

            final var tokenProvider = (Function<HttpExchange, Optional<String>>) (exchange) -> Arrays.stream(exchange.getRequestURI().getRawQuery().split("&"))
                    .filter(x -> x.split("=").length >= 2)
                    .map(x -> x.split("="))
                    .filter(x -> x[0].equalsIgnoreCase("token"))
                    .map(x -> x[1])
                    .findFirst();

            String finalTokenGen = tokenGen;
            if( genToken){
                assert finalTokenGen != null; // officially guaranteed in the coding level
            }

            server.createContext("/keys", exchange -> {
                if (exchange.getRequestMethod().equalsIgnoreCase("get")) {
                    if (genToken) {
                        var param = tokenProvider.apply(exchange);
                        if (param.isEmpty()) {
                            handleError(exchange, 403);
                        } else {
                            if (!finalTokenGen.equalsIgnoreCase(param.get())) {
                                handleError(exchange, 403);
                            }
                        }
                    }
                    final var rs = om.writeValueAsString(map.keySet());
                    exchange.getResponseHeaders().set(CONTENT_TYPE, APPLICATION_JSON);
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
                if (exchange.getRequestMethod().equalsIgnoreCase("get")) {
                    if (genToken) {
                        var param = tokenProvider.apply(exchange);
                        if (param.isEmpty()) {
                            handleError(exchange, 403);
                        } else if (!finalTokenGen.equalsIgnoreCase(param.get())) {
                            handleError(exchange, 403);
                        }
                    }
                    final var p = exchange.getRequestURI().getPath();
                    final var pattern = Pattern.compile("/kv/([\\w+][\\w+-_0-9]*)");
                    final var matcher = pattern.matcher(p);
                    if (matcher.matches()) {
                        final var k = matcher.group(1);
                        if (map.containsKey(k)) {
                            exchange.getResponseHeaders().set(CONTENT_TYPE, TEXT_PLAIN);
                            final var v = map.get(k);
                            final var body = exchange.getResponseBody();
                            exchange.sendResponseHeaders(200, v.length());
                            body.write(v.getBytes(StandardCharsets.UTF_8));
                            body.close();
                        } else {
                            handleError(exchange, 404);
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
                if (exchange.getRequestMethod().equalsIgnoreCase("get")) {
                    if (genToken) {
                        var param = tokenProvider.apply(exchange);
                        if (param.isEmpty()) {
                            handleError(exchange, 403);
                        } else if (!finalTokenGen.equalsIgnoreCase(param.get())) {
                            handleError(exchange, 403);
                        }
                    }
                    final var mapResult = om.writeValueAsString(map);
                    exchange.getResponseHeaders().set(CONTENT_TYPE, APPLICATION_JSON);
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
            console().log("Restful server started on port " + portInt);
            console().log("Plean enter shutdown the server");
            new Scanner(System.in).nextLine();
            server.stop(1);
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

}
