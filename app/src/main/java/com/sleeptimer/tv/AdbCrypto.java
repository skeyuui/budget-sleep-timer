package com.sleeptimer.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;

public class AdbCrypto {
    private static final String PREF_NAME = "AdbCryptoPrefs";
    private static final String KEY_PRIVATE = "private_key";
    private static final String KEY_PUBLIC = "public_key";

    public static class AdbKeyPair {
        public final PrivateKey privateKey;
        public final String adbPublicKeyString;

        public AdbKeyPair(PrivateKey privateKey, String adbPublicKeyString) {
            this.privateKey = privateKey;
            this.adbPublicKeyString = adbPublicKeyString;
        }
    }

    public static AdbKeyPair loadOrGenerateKeys(Context context) throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String privB64 = prefs.getString(KEY_PRIVATE, null);
        String pubStr = prefs.getString(KEY_PUBLIC, null);

        if (privB64 != null && pubStr != null) {
            try {
                byte[] pk8 = Base64.decode(privB64, Base64.DEFAULT);
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pk8);
                PrivateKey privKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
                return new AdbKeyPair(privKey, pubStr);
            } catch (Exception e) {
                // If parsing fails, fall through and generate new keys
            }
        }

        // Generate new 2048-bit RSA key pair
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair keyPair = kpg.generateKeyPair();
        
        PrivateKey privKey = keyPair.getPrivate();
        RSAPublicKey pubKey = (RSAPublicKey) keyPair.getPublic();

        // Compute ADB public key struct parameters
        BigInteger n = pubKey.getModulus();
        BigInteger e = pubKey.getPublicExponent();
        
        BigInteger r32 = BigInteger.ONE.shiftLeft(32);
        BigInteger n0inv = n.remainder(r32).modInverse(r32).negate();
        if (n0inv.compareTo(BigInteger.ZERO) < 0) {
            n0inv = n0inv.add(r32);
        }
        
        BigInteger r = BigInteger.ONE.shiftLeft(2048);
        BigInteger rr = r.multiply(r).mod(n);

        // Build the ADB RSAPublicKey struct
        ByteBuffer buf = ByteBuffer.allocate(524);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        
        buf.putInt(2048 / 32); // len
        buf.putInt(n0inv.intValue()); // n0inv
        
        // modulus (n) as little-endian array of 32-bit words
        byte[] nBytes = n.toByteArray();
        putBigEndianBytesAsLittleEndianWords(buf, nBytes, 256);
        
        // rr as little-endian array of 32-bit words
        byte[] rrBytes = rr.toByteArray();
        putBigEndianBytesAsLittleEndianWords(buf, rrBytes, 256);
        
        buf.putInt(e.intValue()); // exponent

        String pubB64 = Base64.encodeToString(buf.array(), Base64.NO_WRAP);
        String adbPublicKeyString = pubB64 + " sleeptimer@tv\0";
        
        String newPrivB64 = Base64.encodeToString(privKey.getEncoded(), Base64.NO_WRAP);

        prefs.edit()
            .putString(KEY_PRIVATE, newPrivB64)
            .putString(KEY_PUBLIC, adbPublicKeyString)
            .apply();

        return new AdbKeyPair(privKey, adbPublicKeyString);
    }

    private static void putBigEndianBytesAsLittleEndianWords(ByteBuffer buf, byte[] bytes, int targetLength) {
        // bytes is big-endian and might have a leading sign byte (0x00)
        int startOffset = 0;
        if (bytes.length > targetLength && bytes[0] == 0) {
            startOffset = 1;
        }
        
        byte[] padded = new byte[targetLength];
        int copyLen = Math.min(bytes.length - startOffset, targetLength);
        int destOffset = targetLength - copyLen;
        System.arraycopy(bytes, startOffset, padded, destOffset, copyLen);
        
        // Reverse array by 32-bit words, and reverse byte order within words
        for (int i = targetLength - 4; i >= 0; i -= 4) {
            buf.put(padded[i + 3]);
            buf.put(padded[i + 2]);
            buf.put(padded[i + 1]);
            buf.put(padded[i]);
        }
    }
}
