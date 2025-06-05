package me.xethh.tools.jvault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Test autocomplete script")
class AutoCompleteTest {
    @Test
    @DisplayName("When run j-vault autocomplete")
    void testOpensslEncryptCommand() {
        PasswordGenTest.borrowStdOut(os->{
            Main.main("autocomplete".split(" "));
            final var res = os.toString().split("\n");
            final var head = res[0];
            final var last = res[res.length - 1];
            assertEquals("#!/usr/bin/env bash", head,"Console output should be the same");
            assertEquals("complete -F _complete_j-vault -o default j-vault j-vault.sh j-vault.bash", last,"Console output should be the same");
        });
    }
}
