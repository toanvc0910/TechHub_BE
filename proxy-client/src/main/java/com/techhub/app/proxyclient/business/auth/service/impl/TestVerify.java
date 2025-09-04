package com.techhub.app.proxyclient.business.auth.service.impl;

import java.security.*;
import java.util.Base64;
public class TestVerify {
    // Phương thức để tạo cặp khóa RSA (Private và Public)
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048); // Kích thước khóa (2048-bit)
        return keyGen.generateKeyPair();
    }
    // Phương thức để ký dữ liệu bằng Private Key
    public static String signData(String data, PrivateKey privateKey) throws Exception
    {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes());
        byte[] signedData = signature.sign();
        return Base64.getEncoder().encodeToString(signedData);
    }
    // Phương thức để xác thực chữ ký bằng Public Key
    public static boolean verifySignature(String data, String signatureStr, PublicKey
            publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes());
        byte[] signatureBytes = Base64.getDecoder().decode(signatureStr);
        return signature.verify(signatureBytes);
    }
    public static void main(String[] args) {
        try {
            // Tạo cặp khóa RSA
            KeyPair keyPair = generateRSAKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();
            PublicKey publicKey = keyPair.getPublic();
            // Dữ liệu cần ký
            String data = "Đây là dữ liệu cần ký.";
            // Ký dữ liệu
            String signature = signData(data, privateKey);
            System.out.println("Chữ ký: " + signature);
            // Xác thực chữ ký
            boolean isCorrect = verifySignature(data, signature, publicKey);
            System.out.println("Chữ ký hợp lệ: " + isCorrect);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
