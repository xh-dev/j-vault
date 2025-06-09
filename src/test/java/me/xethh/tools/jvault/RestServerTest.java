package me.xethh.tools.jvault;

import io.vavr.control.Try;
import me.xethh.libs.encryptDecryptLib.jwtVer.Sys;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

import static me.xethh.tools.jvault.authserv.AuthServerClient.TestConst.TEST_TOKEN;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test vault rest server")
public class RestServerTest {
    @Test
    @DisplayName("When run vault rest server with secure mode")
    public void testSecureRestServer() {
        var streams = PasswordGenTest.streamsWithPipe();
        PasswordGenTest.borrowStdOutV3(streams._1(), streams._2(), streams._3(), () -> {
            var f1 = Path.of("target/test-case/vault.kv");
            if (f1.toFile().exists())
                f1.toFile().delete();
            f1.getParent().toFile().mkdirs();
            var cmd = String.format("vault -c %s set -f %s -k x -v y", TEST_TOKEN, f1.toString());
            Main.main(cmd.split(" "));

            var tokenGen = new AtomicReference<String>(null);
            new Thread(() -> {
                var cmd1 = String.format("vault -c %s over-http --secure -f %s ", TEST_TOKEN, f1.toString());
                Main.main(cmd1.split(" "));
            }).start();
            var client = HttpClient.newHttpClient();
            HttpResponse res = null;

            final int MAX=10;
            int i=0;
            boolean success = false;
            while (i<MAX) {
                var out = streams._2().toString().split("\n");
                if(out.length > 1){
                    var found_token=false;
                    for(var str : out){
                        if(str.trim().equalsIgnoreCase("token:")){
                            found_token=true;
                            continue;
                        }
                        if(found_token){
                            tokenGen.set(str);
                            success = true;
                            break;
                        }
                    }
                }
                if(tokenGen.get() != null){
                    break;
                }
                Thread.sleep(Duration.ofSeconds(1).toMillis());
                i++;
            }
            res = client.send(HttpRequest.newBuilder(new URI("http://localhost:7910/kv/x?token="+tokenGen)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals("y", res.body());
        });
    }

    @Test
    @DisplayName("When run vault rest server with harden mode")
    public void testHardenRestServer() {
        var streams = PasswordGenTest.streamsWithPipe();
        PasswordGenTest.borrowStdOutV3(streams._1(), streams._2(), streams._3(), () -> {
            var f1 = Path.of("target/test-case/vault.kv");
            if (f1.toFile().exists())
                f1.toFile().delete();
            f1.getParent().toFile().mkdirs();
            var cmd = String.format("vault -c %s set -f %s -k x -v y", TEST_TOKEN, f1.toString());
            Main.main(cmd.split(" "));
            new Thread(() -> {
                var cmd1 = String.format("vault -c %s over-http --harden -f %s ", TEST_TOKEN, f1.toString());
                Main.main(cmd1.split(" "));
            }).start();
            var client = HttpClient.newHttpClient();
            HttpResponse res = null;
            var tokenGen = DigestUtils.md5Hex(String.format("http://0.0.0.0:%s", "7910"));

            final int MAX=10;
            int i=0;
            boolean success = false;
            while (i<MAX) {
                try {
                    res = client.send(HttpRequest.newBuilder(new URI("http://localhost:7910/kv/x?token="+tokenGen)).GET().build(), HttpResponse.BodyHandlers.ofString());
                    assertEquals("y", res.body());
                    success = true;
                    break;
                } catch (ConnectException x) {
                    // ignored
                }
                Thread.sleep(Duration.ofSeconds(1).toMillis());
                i++;
            }
            assertTrue(success);
        });
    }

    @Test
    @DisplayName("When run vault rest server with normal case")
    public void testBasicRestServer() {
        var streams = PasswordGenTest.streamsWithPipe();
        PasswordGenTest.borrowStdOutV3(streams._1(), streams._2(), streams._3(), ()->{
            var f1= Path.of("target/test-case/vault.kv");
            if(f1.toFile().exists())
                f1.toFile().delete();
            f1.getParent().toFile().mkdirs();
            var cmd = String.format("vault -c %s set -f %s -k x -v y", TEST_TOKEN, f1.toString());
            Main.main(cmd.split(" "));
            new Thread(() -> {
                var cmd1 = String.format("vault -c %s over-http -f %s ", TEST_TOKEN, f1.toString());
                Main.main(cmd1.split(" "));
            }).start();
            var client = HttpClient.newHttpClient();
            HttpResponse res=null;
            while (true){
                try{
                    res=client.send(HttpRequest.newBuilder(new URI("http://localhost:7910/kv/x")).GET().build(), HttpResponse.BodyHandlers.ofString());
                } catch (ConnectException x){
                    // ignored
                    Thread.sleep(Duration.ofSeconds(1).toMillis());
                    continue;
                }
                break;
            }
            res=client.send(HttpRequest.newBuilder(new URI("http://localhost:7910/kv/x")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals("y", res.body());
            res = client.send(HttpRequest.newBuilder(new URI("http://localhost:7910/keys")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals("[\"x\"]", res.body());

            res = client.send(HttpRequest.newBuilder(new URI("http://localhost:7910/kvs")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertEquals("{\"x\":\"y\"}", res.body());


        });
    }
}
