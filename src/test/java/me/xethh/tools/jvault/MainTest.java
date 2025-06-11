package me.xethh.tools.jvault;

import net.lingala.zip4j.ZipFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test main")
public class MainTest {
    @Test
    @DisplayName("When all main with not param")
    public void testVaultSetOverride() {
        var streams = PasswordGenTest.streamsWithPipe();
        PasswordGenTest.borrowStdOutV3(streams._1(), streams._2(), streams._3(),()->{
            Main.main(new String[]{});
            assertEquals("Usage",streams._2().toString().split("\n")[0].substring(0,5));
        });
    }
}
