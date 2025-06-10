package me.xethh.tools.jvault;

import me.xethh.tools.jvault.display.Console;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test gen token")
public class TokenGenTest {
    @Test
    @DisplayName("When call token")
    public void testTokenCall(){
        assertDoesNotThrow(()->{
            Main.main("token".split(" "));
        });

    }

    @Test
    @DisplayName("When gen token")
    public void testVaultSetOverride() {
        var is = new ByteArrayInputStream(new byte[]{});
        var os = new ByteArrayOutputStream();
        var es = new ByteArrayOutputStream();

        ByteArrayOutputStream finalOs2 = os;
        PasswordGenTest.borrowStdOutV2(is, os, es, () -> {
                    Main.main("token gen".split(" "));
                    var output = finalOs2.toString();
                    assertTrue(output.split(":").length==2);
                    assertTrue(output.endsWith("\n"));
                }
        );

        is = new ByteArrayInputStream(new byte[]{});
        os = new ByteArrayOutputStream();
        es = new ByteArrayOutputStream();
        ByteArrayOutputStream finalOs = os;
        PasswordGenTest.borrowStdOutV2(is, os, es, () -> {
                    Console.getConsole().disableDebugging();
                    Main.main("token gen --out-bash-env".split(" "));
                    var output = finalOs.toString();
                    assertTrue(output.split(":").length==2);
                    assertTrue(output.endsWith("\n"));
                }
        );
        is = new ByteArrayInputStream(new byte[]{});
        os = new ByteArrayOutputStream();
        es = new ByteArrayOutputStream();
        ByteArrayOutputStream finalOs1 = os;
        PasswordGenTest.borrowStdOutV2(is, os, es, () -> {
                    Console.getConsole().disableDebugging();
                    Main.main("token gen --out-win-env".split(" "));
                    var output = finalOs1.toString();
                    assertTrue(output.split(":").length==2);
                    assertTrue(output.endsWith("\n"));
                }
        );
    }
}
