package me.xethh.tools.jvault;

import me.xethh.tools.jvault.exceptionhandling.CommonHandle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Test Console Object")
class CommonHandleTest {
    @Test
    @DisplayName("When console is not debugging")
    void testConsoleNormalMode() {
        assertThrows(RuntimeException.class, () -> CommonHandle.throwExceptionIfNotExpected(true, "test mess"));
        assertThrows(RuntimeException.class,()->
                CommonHandle.tryCatchThrow(() -> {
                    throw new RuntimeException("test message");
                })
        );
    }

}
