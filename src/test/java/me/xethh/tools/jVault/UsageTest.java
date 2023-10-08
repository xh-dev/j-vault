package me.xethh.tools.jVault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static me.xethh.tools.jVault.PasswordGenTest.borrowStdOut;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Usage command test test")
public class UsageTest {


    @Test
    @DisplayName("When run j-vault")
    public void testGenDefault() {
        var oos = borrowStdOut((os) -> {
            new CommandLine(new Main()).execute();

            assertEquals("Usage: J-Vault [-hV] [COMMAND]\n" +
                    "j-vault is a very simple key value based password vault cli program.\n" +
                    "  -h, --help      Show this help message and exit.\n" +
                    "  -V, --version   Print version information and exit.\n" +
                    "Commands:\n" +
                    "  vault  allow user modify and read vault information\n" +
                    "  file   file encrypt and decrypt with j-vault\n" +
                    "  token  manage token\n", os.toString());
        });
    }

    @Test
    @DisplayName("When run j-vault token")
    public void testJvaultToken() {
        borrowStdOut((os) -> {
            new CommandLine(new Main()).execute("token");

            assertEquals("Usage: token [COMMAND]\n" +
                    "manage token\n" +
                    "Commands:\n" +
                    "  gen  generate token\n", os.toString());
        });
    }

    @Test
    @DisplayName("When run j-vault file")
    public void testJvaultFile() {
        borrowStdOut((os) -> {
            new CommandLine(new Main()).execute("file");

            assertEquals("Usage: file [-c=<credential>] [COMMAND]\n" +
                    "file encrypt and decrypt with j-vault\n" +
                    "  -c, --credential=<credential>\n" +
                    "         The credential to use, if missing, would try find env variable\n" +
                    "           `x-credential`\n" +
                    "Commands:\n" +
                    "  encrypt  Encrypt a specified file with j-vault encryption format\n" +
                    "  decrypt  Decrypt a specified file with j-vault encryption format\n", os.toString());
        });
    }

    @Test
    @DisplayName("When run j-vault file encrypt -h")
    public void testJvaultFileEncryptHelp() {
        borrowStdOut((os) -> {
            new CommandLine(new Main()).execute("file", "encrypt", "-h");

            assertEquals("Usage: J-Vault file encrypt [-hk] -f=<infile> [-o=<outFile>]\n" +
                    "Encrypt a specified file with j-vault encryption format\n" +
                    "  -f, --in-file=<infile>     file to be decrypt or encrypt\n" +
                    "  -h, --help                 display a help message\n" +
                    "  -k, --keep                 self destruct file after encrypt\n" +
                    "  -o, --out-file=<outFile>   output file\n", os.toString());
        });
    }

    @Test
    @DisplayName("When run j-vault file encrypt -h")
    public void testJvaultFileDecryptHelp() {
        borrowStdOut((os) -> {
            new CommandLine(new Main()).execute("file", "decrypt", "-h");

            assertEquals("Usage: J-Vault file decrypt [-hk] [--stdout] -f=<inFile> [-o=<outFile>]\n" +
                    "Decrypt a specified file with j-vault encryption format\n" +
                    "  -f, --in-file=<inFile>     file to be decrypt or encrypt\n" +
                    "  -h, --help                 display a help message\n" +
                    "  -k, --keep                 self destruct file after encrypt\n" +
                    "  -o, --out-file=<outFile>   output file, ignored if `-o` or `--stdout` exists\n" +
                    "      --stdout               output result to stdout\n", os.toString());
        });
    }

    @Test
    @DisplayName("When run j-vault vault")
    public void runVault() {
        borrowStdOut((os) -> {
            new CommandLine(new Main()).execute("vault");

            assertEquals("Usage: vault [-c=<credential>] [COMMAND]\n" +
                    "allow user modify and read vault information\n" +
                    "  -c, --credential=<credential>\n" +
                    "         The credential to use, if missing, would try find env variable\n" +
                    "           `x-credential`\n" +
                    "Commands:\n" +
                    "  view   view the vault content\n" +
                    "  find   find the vault content by key\n" +
                    "  set    set a key value entry\n" +
                    "  unset  unset a key value entry by key\n", os.toString());
        });
    }

    @Test
    @DisplayName("When run j-vault vault set -h")
    public void runVaultSetHelp() {
        borrowStdOut((os) -> {
            new CommandLine(new Main()).execute("vault", "set", "-h");

            assertEquals("Usage: J-Vault vault set [-h] [-f=<file>] -k=<key> -v=<value>\n" +
                    "set a key value entry\n" +
                    "  -f, --file=<file>     The file to encrypt\n" +
                    "  -h, --help            display a help message\n" +
                    "  -k, --key=<key>       the key to set\n" +
                    "  -v, --value=<value>   the value to set\n", os.toString());
        });
    }

    @Test
    @DisplayName("When run j-vault vault find -h")
    public void runVaultFindHelp() {
        borrowStdOut((os) -> {
            new CommandLine(new Main()).execute("vault", "find", "-h");

            assertEquals("Usage: J-Vault vault find [-h] [-f=<file>] -k=<key>\n" +
                    "find the vault content by key\n" +
                    "  -f, --file=<file>   The file to encrypt\n" +
                    "  -h, --help          display a help message\n" +
                    "  -k, --key=<key>     the key to find\n", os.toString());
        });
    }

    @Test
    @DisplayName("When run j-vault vault view -h")
    public void runVaultViewHelp() {
        borrowStdOut((os) -> {
            new CommandLine(new Main()).execute("vault", "view", "-h");

            assertEquals("Usage: J-Vault vault view [-h] [-f=<file>]\n" +
                    "view the vault content\n" +
                    "  -f, --file=<file>   The file to encrypt\n" +
                    "  -h, --help          display a help message\n", os.toString());
        });
    }

    @Test
    @DisplayName("When run j-vault vault unset -h")
    public void runVaultUnsetHelp() {
        borrowStdOut((os) -> {
            new CommandLine(new Main()).execute("vault", "unset", "-h");

            assertEquals("Usage: J-Vault vault unset [-h] [-f=<file>] -k=<key>\n" +
                    "unset a key value entry by key\n" +
                    "  -f, --file=<file>   The file to encrypt\n" +
                    "  -h, --help          display a help message\n" +
                    "  -k, --key=<key>     the key to remove\n", os.toString());
        });
    }
}
