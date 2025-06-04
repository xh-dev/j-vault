package me.xethh.tools.jvault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Supplier;

import static me.xethh.tools.jvault.PasswordGenTest.borrowStdOut;
import static me.xethh.tools.jvault.PasswordGenTest.prepareEmptyDirectoryAsHome;
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
        public void before() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), home -> {
            });
        }

        @Test
        @DisplayName("When run j-vault vault view")
        public void testVaultView() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), (home) -> {
                var vaultKv = home.resolve("vault.kv");
                borrowStdOut(os -> {
                    assertFalse(vaultKv.toFile().exists());
                    new CommandLine(new Main()).execute("vault", "-c", password.get(), "view", "-f", vaultKv.toString());
                    viewVault(password.get(), vaultKv);
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });
            });
        }

        @Test
        @DisplayName("When run j-vault vault set -k key1 -v value1")
        public void testVaultSetSimple() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), (home) -> {
                var vaultKv = home.resolve("vault.kv");
                borrowStdOut(os -> {
                    assertFalse(vaultKv.toFile().exists());
                    setValue(password.get(), vaultKv, "key1", "value1");
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });

                borrowStdOut(os -> {
                    findValue(password.get(), vaultKv, "key1");
                    assertEquals("value1\n", os.toString());
                });
                borrowStdOut(os -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("key1=value1\n", os.toString());
                });
            });
        }

        @Test
        @DisplayName("When run j-vault override `set` command")
        public void testVaultSetOverride() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), (home) -> {
                var vaultKv = home.resolve("vault.kv");
                borrowStdOut(os -> {
                    assertFalse(vaultKv.toFile().exists());
                    setValue(password.get(), vaultKv, "key1", "value1");
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                borrowStdOut(os -> {
                    assertTrue(vaultKv.toFile().exists());
                    setValue(password.get(), vaultKv, "key1", "value2");
                    assertEquals("Found key[key1] and replace\n", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });

                borrowStdOut(os -> {
                    findValue(password.get(), vaultKv, "key1");
                    assertEquals("value2\n", os.toString());
                });
                borrowStdOut(os -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("key1=value2\n", os.toString());
                });
            });
        }

        @Test
        @DisplayName("When run j-vault `unset` command")
        public void testVaultSet() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), (home) -> {
                var vaultKv = home.resolve("vault.kv");
                borrowStdOut(os -> {
                    assertFalse(vaultKv.toFile().exists());
                    setValue(password.get(), vaultKv, "key1", "value1");
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                borrowStdOut(os -> {
                    findValue(password.get(), vaultKv, "key1");
                    assertEquals("value1\n", os.toString());
                });
                borrowStdOut(os -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("key1=value1\n", os.toString());
                });

                borrowStdOut(os -> {
                    unsetValue(password.get(), vaultKv, "key1");
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });

                borrowStdOut(os -> {
                    findValue(password.get(), vaultKv, "key1");
                    assertEquals("", os.toString());
                });
                borrowStdOut(os -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("", os.toString());
                });
            });
        }

        @Test
        @DisplayName("When run j-vault `unset` in-exist entry")
        public void testVaultUnsetInExistsValue() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), (home) -> {
                var vaultKv = home.resolve("vault.kv");
                borrowStdOut(os -> {
                    assertFalse(vaultKv.toFile().exists());
                    setValue(password.get(), vaultKv, "key1", "value1");
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                borrowStdOut(os -> {
                    setValue(password.get(), vaultKv, "key2", "value2");
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                borrowStdOut(os -> {
                    setValue(password.get(), vaultKv, "key3", "value3");
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                borrowStdOut(os -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("key1=value1\nkey2=value2\nkey3=value3\n", os.toString());
                });
                borrowStdOut(os -> {
                    unsetValue(password.get(), vaultKv, "key4");
                    assertEquals("", os.toString());
                });
                borrowStdOut(os -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("key1=value1\nkey2=value2\nkey3=value3\n", os.toString());
                });

            });
        }

        @Test
        @DisplayName("When run j-vault `unset` multiple command")
        public void testVaultUnsetMultipleValue() {
            prepareEmptyDirectoryAsHome(homeSupplier.get().toFile(), (home) -> {
                var vaultKv = home.resolve("vault.kv");
                borrowStdOut(os -> {
                    assertFalse(vaultKv.toFile().exists());
                    setValue(password.get(), vaultKv, "key1", "value1");
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                borrowStdOut(os -> {
                    setValue(password.get(), vaultKv, "key2", "value2");
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                borrowStdOut(os -> {
                    setValue(password.get(), vaultKv, "key3", "value3");
                    assertEquals("", os.toString());
                    assertTrue(vaultKv.toFile().exists());
                });
                borrowStdOut(os -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("key1=value1\nkey2=value2\nkey3=value3\n", os.toString());
                });

                borrowStdOut(os -> {
                    unsetValue(password.get(), vaultKv, "key1");
                    assertEquals("", os.toString());
                });
                borrowStdOut(os -> {
                    findValue(password.get(), vaultKv, "key1");
                    assertEquals("", os.toString());
                });
                borrowStdOut(os -> {
                    unsetValue(password.get(), vaultKv, "key3");
                    assertEquals("", os.toString());
                });
                borrowStdOut(os -> {
                    viewVault(password.get(), vaultKv);
                    assertEquals("key2=value2\n", os.toString());
                });
            });
        }
    }
}
