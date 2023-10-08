package me.xethh.tools.jVault.cmds.deen;

import me.xethh.tools.jVault.cmds.token.Token;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

public class DeenObj {
    public static final int DEFAULT_SALT1_LENGTH = 4;
    private static boolean DEBUG = false;
    public final SecretKey key;
    public final IvParameterSpec iv;

    public final String fileHeader;

    public DeenObj(SecretKey key, IvParameterSpec iv, String fileHeader) {
        this.key = key;
        this.iv = iv;
        this.fileHeader = fileHeader;
    }

    public final static String ALGO = "AES/CBC/PKCS5Padding";

    public Cipher decryptCipher() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        var cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        return cipher;
    }

    public InputStream decryptInputStream(InputStream is) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        if (DEBUG) {
            return is;
        } else {
            return new CipherInputStream(is, decryptCipher());
        }
    }

    public OutputStream decryptOutputStream(OutputStream os) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        if (DEBUG) {
            return os;
        } else {
            return new CipherOutputStream(os, decryptCipher());
        }
    }

    public Cipher encryptCipher() throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        var cipher = Cipher.getInstance(ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        return cipher;
    }

    public InputStream encryptInputStream(InputStream is) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        if (DEBUG) {
            return is;
        } else {
            return new CipherInputStream(is, encryptCipher());
        }
    }

    public OutputStream encryptOutputStream(OutputStream os) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        if (DEBUG) {
            return os;
        } else {
            return new CipherOutputStream(os, encryptCipher());
        }
    }

    public static DeenObj fromLine(String credentialLine, String vaultStr) throws NoSuchAlgorithmException, InvalidKeySpecException {
        Token.validate(credentialLine);
        Token.validate(vaultStr);

        var de = Base64.getDecoder();

        var creds = credentialLine.split(":");
        var secret = creds[0];

        var vaultS = vaultStr.split(":");
        var iv = new IvParameterSpec(de.decode(vaultS[1]));

        var salt = (creds[1] + vaultS[0]).getBytes();

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, 65536, 256);
        SecretKey key = new SecretKeySpec(factory.generateSecret(spec)
                .getEncoded(), "AES");

        return new DeenObj(key, iv, vaultStr);

    }

    public static String genSaltWithIV() {
        var rand = new SecureRandom();
        byte[] iv = new byte[16];
        rand.nextBytes(iv);
        byte[] salt = new byte[32];
        rand.nextBytes(salt);
        var en = Base64.getEncoder();
        return en.encodeToString(salt) +
                ":" +
                en.encodeToString(iv);
    }

    public static String getFullPassword(String rawPassword) {
        var rand = new SecureRandom();
        byte[] salt = new byte[DEFAULT_SALT1_LENGTH];
        rand.nextBytes(salt);
        return String.format("%s:%s", rawPassword, Base64.getEncoder().encodeToString(salt));
    }
}
