package com.blog.util;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class PasswordUtil {
    public static String hash(String plainPassword) {
        return BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray());
    }

    public static boolean verify(String plainPassword, String storedHash) {
        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), storedHash);
        return result.verified;
    }
}

