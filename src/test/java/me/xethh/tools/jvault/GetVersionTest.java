package me.xethh.tools.jvault;

import io.vavr.control.Try;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test get version")
public class GetVersionTest {
    @Test
    @DisplayName("When retrieve version.txt")
    public void testGetVersion() {
        var version = Try.of(()->{
            var vp = new VP();
            return vp.getVersion();
        }).get();
        assertEquals("testing-version", version[0]);
    }
}
