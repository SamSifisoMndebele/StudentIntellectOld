package com.avidco.studentintellect.security;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSACipher {
    private static KeyPair keyPair;

    public static KeyPair getKeyPair() {
        return keyPair;
    }

    public RSACipher() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        keyPair = keyPairGen.generateKeyPair();
    }

    public static String encrypt() throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {


        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());

        //Adding data to the cipher
        byte[] input = "Welcome to Tutorials point".getBytes();
        cipher.update(input);

        //encrypting the data
        byte[] cipherText = cipher.doFinal();
        System.out.println(new String(cipherText, StandardCharsets.UTF_8));
        return new String(cipherText, StandardCharsets.UTF_8);
    }
}
