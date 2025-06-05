package me.xethh.tools.jvault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Test openssl script gen")
class OpensslTest {
    @Test
    @DisplayName("When run j-vault openssl encrypt")
    void testOpensslEncryptCommand() {
        PasswordGenTest.borrowStdOut(os->{
            Main.main("openssl encrypt".split(" "));
            final var res = os.toString();
            assertEquals("openssl aes-256-cbc -a -salt -pbkdf2 -in kv-pass -out kv-pass.enc\n", res,"Console output should be the same");
        });
    }

    @Test
    @DisplayName("When run j-vault openssl decrypt")
    void testOpensslDecryptCommand() {
        PasswordGenTest.borrowStdOut(os->{
            Main.main("openssl decrypt".split(" "));
            final var res = os.toString();
            assertEquals("openssl aes-256-cbc -d -a -salt -pbkdf2 -in kv-pass.enc\n", res,"Console output should be the same");
        });
    }
}
