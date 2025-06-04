package me.xethh.tools.jvault.cmds.deen;

import me.xethh.tools.jvault.cmds.deen.sub.Common;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

public interface CredentialOwner {
    String CRED_WIN_ENV="x-credential";
    String CRED_NIX_ENV="x_credential";
    String getCredential();

    default String finalCredential(){
        return Optional.of(getCredential())
                .map(it -> {
                    if (it.isEmpty()) {
                        if(System.getenv(CRED_WIN_ENV)!=null){
                            return System.getenv(CRED_WIN_ENV);
                        } else if (System.getenv(CRED_NIX_ENV)!=null){
                            return System.getenv(CRED_NIX_ENV);
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
