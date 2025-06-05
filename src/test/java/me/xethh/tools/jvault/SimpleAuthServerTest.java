package me.xethh.tools.jvault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test simple auth server")
class SimpleAuthServerTest {
    @Test
    @DisplayName("When run j-vault auth-server code-gen -i some-app -n some-user")
    void testOpensslEncryptCommand() {
        PasswordGenTest.borrowStdOut(os->{
            String filename = "qr.png";
            Path path = Path.of(filename);
            if(path.toFile().exists()){
                assertTrue(path.toFile().delete());
            }
            if(path.toFile().exists()){
                fail();
            }
            Main.main("auth-server code-gen -i some-app -n some-user".split(" "));
            final var res = os.toString().split("\n");
            assertTrue(res[0].contains("some-app"));
            assertTrue(res[1].contains("some-user"));
            assertTrue(res[res.length-1].contains("Generated QRCode.png"));
            if(!path.toFile().exists()){
                fail();
            }
        });
    }

}
