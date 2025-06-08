package me.xethh.tools.jvault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Test gen token")
public class TokenGenTest {
    @Test
    @DisplayName("When gen token")
    public void testVaultSetOverride() {
        var is = new ByteArrayInputStream(new byte[]{});
        var os = new ByteArrayOutputStream();
        var es = new ByteArrayOutputStream();

        PasswordGenTest.borrowStdOutV2(is, os, es, () -> {
                    Main.main("token gen".split(" "));
                    var output = os.toString();
                    assertTrue(output.split(":").length==2);
                    assertTrue(output.endsWith("\n"));
                }
        );
    }
}
