package me.xethh.tools.jvault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Supplier;

import static me.xethh.tools.jvault.PasswordGenTest.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Vault test")
public class VaultTest {
    private static Supplier<Path> homeSupplier = () -> new File("target/test-case/").toPath().toAbsolutePath();

    public static void unsetValue(String token, Path file, String key) {
        new CommandLine(new Main()).execute("vault", "-c", token, "unset", "-f", file.toString(), "-k", key);
    }

    public static void setValue(String token, Path file, String key, String value) {
        new CommandLine(new Main()).execute("vault", "-c", token, "set", "-f", file.toString(), "-k", key, "-v", value);
    }

    public static void findValue(String token, Path file, String key) {
        new CommandLine(new Main()).execute("vault", "-c", token, "find", "-f", file.toString(), "-k", key);
    }

    public static void viewVault(String token, Path file) {
        new CommandLine(new Main()).execute("vault", "-c", token, "view", "-f", file.toString());
    }

    @Nested
    @DisplayName("Given no vault.kv exists")
    class GivenNoVaultExists {
        Supplier<String> password = () -> "abcd:dddd";

        @BeforeEach
        void before() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), home -> {
            });
        }

        @Test
        @DisplayName("When run j-vault vault view")
        void testVaultView() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), (home) -> {
                var vaultKv = home.resolve("vault.kv");
                var streams = PasswordGenTest.streams();
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    assertFalse(vaultKv.toFile().exists());
                    new CommandLine(new Main()).execute("vault", "-c", password.get(), "view", "-f", vaultKv.toString());
                    viewVault(password.get(), vaultKv);
                    assertEquals("", streams._2().toString());
                    assertTrue(vaultKv.toFile().exists());
                });
            });
        }

        @Test
        @DisplayName("When run j-vault vault set -k key1 -v value1")
        public void testVaultSetSimple() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), (home) -> {
                var vaultKv = home.resolve("vault.kv");
                InputStream is = new ByteArrayInputStream(new byte[]{});
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ByteArrayOutputStream es = new ByteArrayOutputStream();

                borrowStdOutV2(is, os, es,() -> {
                    assertFalse(vaultKv.toFile().exists());
                    setValue(password.get(), vaultKv, "key1", "value1");
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });

                borrowStdOutV2(is, os, es, () -> {
                    findValue(password.get(), vaultKv, "key1");
                    assertEquals("value1\n", os.toString());
                });
                borrowStdOutV2(is, os, es, () -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("value1\nkey1=value1\n", os.toString());
                });
            });
        }

        @Test
        @DisplayName("When run j-vault override `set` command")
        public void testVaultSetOverride() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), (home) -> {
                var vaultKv = home.resolve("vault.kv");
                InputStream is = new ByteArrayInputStream(new byte[]{});
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                ByteArrayOutputStream es = new ByteArrayOutputStream();

                borrowStdOutV2(is, os, es, () -> {
                    assertFalse(vaultKv.toFile().exists());
                    setValue(password.get(), vaultKv, "key1", "value1");
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                borrowStdOutV2(is, os, es, () -> {
                    assertTrue(vaultKv.toFile().exists());
                    setValue(password.get(), vaultKv, "key1", "value2");
                    assertEquals("Found key[key1] and replace\n", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });

                borrowStdOutV2(is,os,es, () -> {
                    findValue(password.get(), vaultKv, "key1");
                    assertEquals("Found key[key1] and replace\nvalue2\n", os.toString());
                });
                borrowStdOutV2(is,os,es, () -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("Found key[key1] and replace\nvalue2\nkey1=value2\n", os.toString());
                });
            });
        }

        @Test
        @DisplayName("When run j-vault `unset` command")
        public void testVaultSet() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), (home) -> {
                var vaultKv = home.resolve("vault.kv");
                var streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    assertFalse(vaultKv.toFile().exists());
                    setValue(password.get(), vaultKv, "key1", "value1");
                    assertEquals("", finalStreams._2().toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams1 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    findValue(password.get(), vaultKv, "key1");
                    assertEquals("value1\n", finalStreams1._2().toString());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams5 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("key1=value1\n", finalStreams5._2().toString());
                });

                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams2 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    unsetValue(password.get(), vaultKv, "key1");
                    assertEquals("", finalStreams2._2().toString());
                    assertTrue(vaultKv.toFile().exists());
                });

                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams3 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    findValue(password.get(), vaultKv, "key1");
                    assertEquals("", finalStreams3._2().toString());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams4 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("", finalStreams4._2().toString());
                });
            });
        }

        @Test
        @DisplayName("When run j-vault `unset` in-exist entry")
        public void testVaultUnsetInExistsValue() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), (home) -> {
                var vaultKv = home.resolve("vault.kv");
                var streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    assertFalse(vaultKv.toFile().exists());
                    setValue(password.get(), vaultKv, "key1", "value1");
                    assertEquals("", finalStreams._2().toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams1 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    setValue(password.get(), vaultKv, "key2", "value2");
                    assertEquals("", finalStreams1._2().toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams2 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    setValue(password.get(), vaultKv, "key3", "value3");
                    assertEquals("", finalStreams2._2().toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams5 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("key1=value1\nkey2=value2\nkey3=value3\n", finalStreams5._2().toString());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams3 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    unsetValue(password.get(), vaultKv, "key4");
                    assertEquals("", finalStreams3._2().toString());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams4 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("key1=value1\nkey2=value2\nkey3=value3\n", finalStreams4._2().toString());
                });

            });
        }

        @Test
        @DisplayName("When run j-vault `unset` multiple command")
        public void testVaultUnsetMultipleValue() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), (home) -> {
                var vaultKv = home.resolve("vault.kv");
                var streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    assertFalse(vaultKv.toFile().exists());
                    setValue(password.get(), vaultKv, "key1", "value1");
                    assertEquals("", finalStreams._2().toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams1 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    setValue(password.get(), vaultKv, "key2", "value2");
                    assertEquals("", finalStreams1._2().toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams2 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    setValue(password.get(), vaultKv, "key3", "value3");
                    assertEquals("", finalStreams2._2().toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams3 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("key1=value1\nkey2=value2\nkey3=value3\n", finalStreams3._2().toString());
                });

                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams4 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    unsetValue(password.get(), vaultKv, "key1");
                    assertEquals("", finalStreams4._2().toString());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams5 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    findValue(password.get(), vaultKv, "key1");
                    assertEquals("", finalStreams5._2().toString());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams6 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    unsetValue(password.get(), vaultKv, "key3");
                    assertEquals("", finalStreams6._2().toString());
                });
                streams = PasswordGenTest.streams();
                io.vavr.Tuple3<InputStream, ByteArrayOutputStream, ByteArrayOutputStream> finalStreams7 = streams;
                borrowStdOutV2(streams._1(),streams._2(), streams._3(),() -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("key2=value2\n", finalStreams7._2().toString());
                });
            });
        }
    }
}
