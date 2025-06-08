package me.xethh.tools.jvault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Test autocomplete script")
class AutoCompleteTest {
    @Test
    @DisplayName("When run j-vault autocomplete")
    void testOpensslEncryptCommand() {
        var streams = PasswordGenTest.streams();
        PasswordGenTest.borrowStdOutV2(streams._1(),streams._2(),streams._3(),()->{
            Main.main("autocomplete".split(" "));
            final var res = streams._2().toString().split("\n");
            final var head = res[0];
            final var last = res[res.length - 1];
            assertEquals("#!/usr/bin/env bash", head,"Console output should be the same");
            assertEquals("complete -F _complete_j-vault -o default j-vault j-vault.sh j-vault.bash", last,"Console output should be the same");
        });
    }
}
