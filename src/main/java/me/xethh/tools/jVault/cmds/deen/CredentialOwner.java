package me.xethh.tools.jVault.cmds.deen;

import me.xethh.tools.jVault.cmds.deen.sub.Common;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

public interface CredentialOwner {
    String getCredential();

    default String finalCredential(){
        return Optional.of(getCredential())
                .map(it -> {
                    if (it.isEmpty()) {
                        if(System.getenv("x-credential")!=null){
                            return System.getenv("x-credential");
                        } else if (System.getenv("x_credential")!=null){
                            return System.getenv("x_credential");
                        } else {
                            return null;
                        }
                    } else {
                        return it;
                    }
                }).orElseThrow(() -> new RuntimeException("missing credential"));
    }

    default DeenObj getDeenObj(Path path) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        var credsEnv = finalCredential();
        return Common.getDeenObj(path, credsEnv);
    }
}
