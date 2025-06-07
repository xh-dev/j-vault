package me.xethh.tools.jvault;

import me.xethh.tools.jvault.display.Console;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test Console Object")
class ConsoleTest {
    @Test
    @DisplayName("When console is not debugging")
    void testConsoleNormalMode() {
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{});
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ByteArrayOutputStream es = new ByteArrayOutputStream();

        PasswordGenTest.borrowStdOutV2(is,os, es, ()->{
            assertFalse(Console.getConsole().isDebugging());
            Console.getConsole().log("this is a log message");
            Console.getConsole().debug("this is a debug log message");
        });

        String osMessage = os.toString();
        assertEquals("this is a log message\n", osMessage);
        String esMessage = es.toString();
        assertEquals("", esMessage);
    }

    @Test
    @DisplayName("When console is debugging")
    void testConsoleDebugMode() {
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{});
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ByteArrayOutputStream es = new ByteArrayOutputStream();

        PasswordGenTest.borrowStdOutV2(is,os, es, ()->{
            assertFalse(Console.getConsole().isDebugging());
            Console.getConsole().setDebugging();
            assertTrue(Console.getConsole().isDebugging());

            Console.getConsole().log("this is a log message");
            Console.getConsole().debug("this is a error log message");
            Console.getConsole().debug("this is a debug log message");

            Console.getConsole().getDisplay().println("this line is from getDisplay");
            Console.getConsole().getError().println("this line is from getError");

            Console.getConsole().disableDebugging();
            assertFalse(Console.getConsole().isDebugging());

            AtomicBoolean b = new AtomicBoolean(false);
            Console.getConsole().doIfDebug(()->{
                b.set(true);
            });

            assertFalse(b.get());

            Console.getConsole().setDebugging();
            Console.getConsole().doIfDebug(()->{
                b.set(true);
            });

            assertTrue(b.get());
        });

        String osMessage = os.toString();
        assertEquals("this is a log message\nthis line is from getDisplay\n", osMessage);
        String esMessage = es.toString();
        assertEquals("this is a error log message\nthis is a debug log message\nthis line is from getError\n", esMessage);


    }


    @Test
    @DisplayName("When console is print stack trace")
    void testPrintStackTrace() {
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[]{});
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ByteArrayOutputStream es = new ByteArrayOutputStream();

        PasswordGenTest.borrowStdOutV2(is,os, es, ()->{
            Console.getConsole().disableDebugging();
            Console.getConsole().printStackTrace(new RuntimeException("This message is not going to be logged"));
        });

        String osMessage = os.toString();
        assertEquals("", osMessage);
        String esMessage = es.toString();
        assertEquals("", esMessage);

        PasswordGenTest.borrowStdOutV2(is,os, es, ()->{
            Console.getConsole().setDebugging();
            Console.getConsole().printStackTrace(new RuntimeException("This message is intended to be logged"));
        });


        osMessage = os.toString();
        assertEquals("", osMessage);
        esMessage = es.toString();
        assertNotEquals("", esMessage);
        assertTrue(esMessage.contains("This message is intended to be logged"));
    }
}
