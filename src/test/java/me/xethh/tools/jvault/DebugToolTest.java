package me.xethh.tools.jvault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Test debug tools")
class DebugToolTest {
    @Test
    @DisplayName("When run j-vault debug-tools print-env")
    void testDebugToolPrintEnv() {
        PasswordGenTest.borrowStdOut(os->{
            Main.main("debug-tools print-env".split(" "));
            final var res = os.toString().split("\n");
            final var head = res[0];
            final var last = res[res.length - 1];
            assertEquals("==================== Debug tools - Print all env", head,"Console output should be the same");
            assertEquals("==================== Debug tools - Print all env [Done]", last,"Console output should be the same");
        });
    }

    @Test
    @DisplayName("When run j-vault debug-tools print-props")
    void testDebugToolPrintProps() {
        PasswordGenTest.borrowStdOut(os->{
            Main.main("debug-tools print-props".split(" "));
            final var res = os.toString().split("\n");
            final var head = res[0];
            final var last = res[res.length - 1];
            assertEquals("==================== Debug tools - Print all properties", head,"Console output should be the same");
            assertEquals("==================== Debug tools - Print all properties [Done]", last,"Console output should be the same");
        });
    }

}
