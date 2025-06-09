package me.xethh.tools.jvault;

import dev.samstevens.totp.exceptions.CodeGenerationException;
import me.xethh.tools.jvault.authserv.AuthServerClient;
import me.xethh.tools.jvault.display.Console;
import org.bouncycastle.crypto.prng.drbg.DualECPoints;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test simple auth server")
class SimpleAuthServerTest {
    @Test
    @DisplayName("When run j-vault auth-server code-gen -i some-app -n some-user")
    void testAuthServerCodeGen() {
        var streams = PasswordGenTest.streams();
        PasswordGenTest.borrowStdOutV2(streams._1(),streams._2(),streams._3(),()->{
            String filename = "qr.png";
            Path path = Path.of(filename);
            if(path.toFile().exists()){
                assertTrue(path.toFile().delete());
            }
            if(path.toFile().exists()){
                fail();
            }
            Main.main("auth-server code-gen -i some-app -n some-user".split(" "));
            final var res = streams._2().toString().split("\n");
            assertTrue(res[0].contains("some-app"));
            assertTrue(res[1].contains("some-user"));
            assertTrue(res[res.length-1].contains("Generated QRCode.png"));
            if(!path.toFile().exists()){
                fail();
            }
        });
    }

    @Test
    @DisplayName("Test simple auth server start up and receive code")
    void testAuthServer(){
        var streams = PasswordGenTest.streamsWithPipe();
        PasswordGenTest.borrowStdOutV3(streams._1(),streams._2(),streams._3(),()->{
            Console.getConsole().setDebugging();
            new Thread(()->{
                var cmdForStartUpServer = String.format("auth-server simple -v %s -s %s -k %s", AuthServerClient.TestConst.TEST_TOKEN, AuthServerClient.TestConst.TEST_SECRET, AuthServerClient.TestConst.TEST_USER);
                Main.main(cmdForStartUpServer.split(" "));
            }).start();

            final int MAX=10;
            int i=0;
            boolean done = false;
            while (i < MAX) {
                var resp = HttpClient.newHttpClient()
                        .send(HttpRequest.newBuilder(URI.create("http://localhost:8001/a")).GET().build(), HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() == 200) {
                    done = true;
                    break;
                }
                Thread.sleep(1000);
                i++;
            }
            assertTrue(done);

            var as = new AuthServerClient() {
                @Override
                public Optional<String> testingModeCode() {
                    try {
                        String code = nextCode(TestConst.ALGO, TestConst.CODE_DIGIT, TestConst.TEST_SECRET, TestConst.TIME_PERIOD);
                        return Optional.of(code);
                    } catch (CodeGenerationException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public String authServer() {
                    return "http://localhost:8001";
                }
            };

            var returnedCode = as.getCode(AuthServerClient.TestConst.TEST_USER);

            streams._1().write("Done".getBytes());
        });
    }

}
