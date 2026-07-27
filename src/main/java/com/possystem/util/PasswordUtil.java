package com.possystem.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateSalt() {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(salt));
            byte[] hashed = digest.digest(password.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verify(String password, String salt, String expectedHash) {
        return hash(password, salt).equals(expectedHash);
    }

    /** Command-line helper: java -cp bin com.possystem.util.PasswordUtil somepassword */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: PasswordUtil <plaintext-password>");
            return;
        }
        String salt = generateSalt();
        String hash = hash(args[0], salt);
        System.out.println("Salt: " + salt);
        System.out.println("Hash: " + hash);
    }
}
