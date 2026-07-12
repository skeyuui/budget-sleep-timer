package com.sleeptimer.tv;

import android.util.Base64;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.Cipher;
import java.util.Arrays;

/**
 * Minimal ADB protocol client.
 * Connects to the local ADB daemon over TCP and sends shell commands.
 * Handles ADB RSA authentication to trigger the permission dialog on the TV.
 */
public class AdbClient {

    private static final int A_CNXN = 0x4e584e43;
    private static final int A_AUTH = 0x48545541;
    private static final int A_OPEN = 0x4e45504f;
    private static final int A_OKAY = 0x59414b4f;

    private static final int A_VERSION = 0x01000000;
    private static final int MAX_PAYLOAD = 4096;
    private static final int TIMEOUT_MS = 3000;

    // Hardcoded ADB PKCS8 Private Key (generated locally for loopback auth)
    private static final String PRIVATE_KEY_B64 = 
        "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDnFUNUgG6O8mIL" +
        "a2VciRCVp6ut/0Hj4XjpvjrScmuYz+fuUHdcb1qsVuqzhZacbmvpdVJXqmDqpgBd" +
        "aszmKgx0ZRAwV7IcPEhXjqSu9AQ1uiPTvFRSxlymMa4RqkI+8gcsQx2X+eaK20dT" +
        "KLeee+fq+MNgUviRwhtNPSuUY0ZTGclXNnDtfGrvBUA2YN9vCZt6IKV5OmqSpKM7" +
        "/5Pqz64kXMDffMJD9oojRgu6WCYhdFUT8R17da7AX2wmN36aM7wlhZVrhZHEU6ZZ" +
        "6xy0QgFOK3dqd9LYhlfaBirZ+S5w1KC1vvxV4IK8bYOYcueykGAL78CiaqubFt7o" +
        "r4eP12NTAgMBAAECggEATWEz4iR9oRVuGmfUkdd9+7chSIsMy9uxKwhd1yGkgQ/B" +
        "mO5OAeBx6vrR7dqCdBCPxyXXtG5jtL9wqNd+FGapNAKxmJaNGGJhURx9be5dSIZz" +
        "v8+1JuCwnqBNKzIpq71NBJfW3ZC7j+zcpe6hCgtwviCd/+/Yt6pZjRQ6Nx7FKkb5" +
        "3fTIdf6KnyiwOfCn93sqztc8aUtGKqXG3ZqLRUw3DZs7oON1mgqmgSFwpiZ67/2x" +
        "j4tN4Lba6+dL5Cpyltn8jSI2ndTiw5+vI+2PkQRILV6SmWHjq5HZxFQJ0eh8Y/cK" +
        "fNCYZYycgRNTWU1ev+qTP/spYcMpZ6tEfJGcsnqx9QKBgQDztXsJkhdW+ytFG2eC" +
        "Gpu29TarH9YWh4sPLCM0gvzqQ2mWAsJMIAm5Aso72KmuTUEzRsS++kry5Z+22wVA" +
        "T4PSeeKw2hUyggFd+e1v+ZGnjZCe966HPKWOCstNjv2Yas+5bceP+x7Il/7P4DmP" +
        "mqNehBjHNVQKJbJIuN12vYfllQKBgQDyvMU3Friud+ATyxv2/qQw6jbvwbxKaKTx" +
        "bp5qAtlVnWqU/EslVcYL/4knYbNO4lkhisO6EiDgBQVvED9sDpHA3Szyw6Pv8XVb" +
        "aU8iIKael9fxUzhiDaer2F25bb3eExdD3rc4lRg5cu+QL40eGiNvJIFXWw8puPNE" +
        "vGu6pjAbRwKBgHEFU9b/alWLS+jTqbAbmOVDWSQJMqbmGyZhKL58lMArnTbVdrgJ" +
        "D5k+Yv1YIHDWIQufoTSULNfyh5wsfIXzmkWtAuVTbgsrYWjstCF+0v6qV0xxvv22" +
        "sWxrMxd33cGmn0j2UVtDcWZDnwdWjDs90s/NYRRZdezyyHiTSxl7Eh/1AoGAXMQT" +
        "wqfmUJbzhdz7hPtwGFmKQTqTEQTI9JPH6s/H61ZLo0CAH3aWR4OTEP/fnOgYFB+K" +
        "CZRgB/0jRHy7IDq5LTHZubVW854dsZ+fZHWB994j4tBhHegGCkCYIQN1qmc1XvTA" +
        "pNkl9t6b+0iQ784heJpE6/Oa6eEK51vy99QHUoUCgYB2FCAwdQg2dV6I2ruadcem" +
        "fx2M9G5MK8GhPNu4vNj4ifP0pJeNoyiwUO08vC24jjnZL5cCfp24D7wPzH4JTlB1" +
        "HJEOv2HLgUpKdUiMB14D58EFUe77R82wwF9gUA/8eAy3e4K+0JRKXqjyWQU1+pTe" +
        "w4bMeMSREYStL1jH4XMHqw==";

    // Hardcoded ADB formatted Public Key
    private static final String PUBLIC_KEY_ADB = 
        "QAAAACUnr4BTY9ePh6/o3habq2qiwO8LYJCy53KYg228guBV/L61oNRwLvnZKgbaV4bY0ndqdytOAUK0HOtZplPEkYVrlYUlvDOafjcmbF/ArnV7HfETVXQhJli6C0YjivZDwnzfwFwkrs/qk/87o6SSajp5pSB6mwlv32A2QAXvanztcDZXyRlTRmOUKz1NG8KR+FJgw/jq53uetyhTR9uK5vmXHUMsB/I+QqoRrjGmXMZSVLzTI7o1BPSupI5XSDwcslcwEGV0DCrmzGpdAKbqYKpXUnXpa26cloWz6lasWm9cd1Du58+Ya3LSOr7peOHjQf+tq6eVEIlcZWsLYvKOboBUQxXnLnYgNO1tQsZR6JcOJLKPNC5ZcpEGpdEWX4Oeei8rYA99K+h5bzA/TWA5nEvWmWid4pVZzH/sHQakKftSAKoClnrCYQZfLa5euCz9x6YXgXHG4wa67sL9OZAtdK1ceazoQ+WfjSqaNmvYaQs08M60ahae/01qqzlWBen/J2SwnVQfT0CSXVMAffv19YVEP5pI018JVZ6uj0CmfUyRdjKhEhDbvvR3bu0X1Owk4fqK8y/aykO/RLUdBu7s3FdKhDd9Y4GR1GJWnB+mVRB545LdZ0y2YBHI34BzinY7auNh1JLN20O9FcbnrSCNqalWVIiObwXS2tGd1JeJ21DwLuobOAEAAQA= r2@HOTARUMARU\0";
    public static final int STATUS_OK = 0;
    public static final int STATUS_AUTH_PENDING = 1;
    public static final int STATUS_ERROR = 2;

    // SHA-1 DigestInfo prefix (DER encoded: SEQUENCE { SEQUENCE { OID sha1, NULL }, OCTET STRING })
    // This matches what OpenSSL's RSA_sign(NID_sha1, ...) prepends to the data.
    private static final byte[] SHA1_DIGEST_INFO_PREFIX = {
        0x30, 0x21, 0x30, 0x09, 0x06, 0x05, 0x2b, 0x0e,
        0x03, 0x02, 0x1a, 0x05, 0x00, 0x04, 0x14
    };

    /**
     * Signs an ADB auth token exactly the way the ADB protocol expects:
     * - Prepend the SHA-1 DigestInfo header (the token IS the digest)
     * - PKCS#1 v1.5 sign with RSA (type-1 padding, NOT encryption)
     */
    private static byte[] signAdbToken(PrivateKey privKey, byte[] token) throws Exception {
        int keySizeBytes = 256; // 2048-bit key
        byte[] padded = new byte[keySizeBytes];
        
        padded[0] = 0x00;
        padded[1] = 0x01;
        
        int prefixLen = SHA1_DIGEST_INFO_PREFIX.length;
        int tokenLen = token.length;
        int padLen = keySizeBytes - 3 - prefixLen - tokenLen;
        
        for (int i = 0; i < padLen; i++) {
            padded[2 + i] = (byte) 0xFF;
        }
        
        padded[2 + padLen] = 0x00;
        
        System.arraycopy(SHA1_DIGEST_INFO_PREFIX, 0, padded, 2 + padLen + 1, prefixLen);
        System.arraycopy(token, 0, padded, 2 + padLen + 1 + prefixLen, tokenLen);

        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
        try {
            cipher.init(Cipher.DECRYPT_MODE, privKey);
        } catch (Exception e) {
            cipher.init(Cipher.ENCRYPT_MODE, privKey);
        }
        return cipher.doFinal(padded);
    }

    public static int testConnection(int port) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", port), 2000);
            socket.setSoTimeout(2000);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            byte[] identity = "host::\0".getBytes("UTF-8");
            writeMessage(out, A_CNXN, A_VERSION, MAX_PAYLOAD, identity);

            int[] header = readHeader(in);
            if (header[0] == A_AUTH) {
                if (header[1] == 1) { 
                    byte[] token = new byte[header[3]];
                    readFully(in, token);
                    
                    byte[] pk8 = Base64.decode(PRIVATE_KEY_B64, Base64.DEFAULT);
                    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pk8);
                    PrivateKey privKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
                    byte[] signature = signAdbToken(privKey, token);
                    
                    writeMessage(out, A_AUTH, 2, 0, signature);
                    header = readHeader(in);
                    if (header[0] == A_CNXN) {
                        return STATUS_OK;
                    } else if (header[0] == A_AUTH) {
                        // Key not yet trusted — send public key to trigger the dialog
                        byte[] pubKey = PUBLIC_KEY_ADB.getBytes("UTF-8");
                        writeMessage(out, A_AUTH, 3, 0, pubKey);

                        // Keep socket open and wait for the user to accept
                        // The TV dialog stays visible as long as this connection lives
                        socket.setSoTimeout(30000);
                        try {
                            header = readHeader(in);
                            if (header[0] == A_CNXN) {
                                return STATUS_OK;
                            }
                        } catch (java.net.SocketTimeoutException ste) {
                            // User didn't accept within 30s
                        }
                        return STATUS_AUTH_PENDING;
                    }
                }
                return STATUS_AUTH_PENDING;
            } else if (header[0] == A_CNXN) {
                return STATUS_OK;
            }
            return STATUS_ERROR;
        } catch (Exception e) {
            return STATUS_ERROR;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }
    public static boolean sendSleepCommand(int port) {
        if (sendShellCommand(port, "input keyevent 223")) {
            return true;
        }
        return sendShellCommand(port, "input keyevent 26");
    }

    public static boolean sendShellCommand(int port, String command) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", port), TIMEOUT_MS);
            
            // Allow more time for user to click the "Allow" dialog if needed
            socket.setSoTimeout(10000); 

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Step 1: Send CNXN
            byte[] identity = "host::\0".getBytes("UTF-8");
            writeMessage(out, A_CNXN, A_VERSION, MAX_PAYLOAD, identity);

            // Read responses
            while (true) {
                int[] header = readHeader(in);
                
                if (header[0] == A_AUTH) {
                    if (header[1] == 1) { // Token
                        byte[] token = new byte[header[3]];
                        readFully(in, token);
                        
                        // Sign the token
                        byte[] pk8 = Base64.decode(PRIVATE_KEY_B64, Base64.DEFAULT);
                        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(pk8);
                        PrivateKey privKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
                        byte[] signature = signAdbToken(privKey, token);
                        
                        // Send signature (type 2)
                        writeMessage(out, A_AUTH, 2, 0, signature);
                        
                    } else if (header[1] == 2 || header[1] == 3) {
                        // Signature failed or no signature provided, send public key (type 3)
                        byte[] pubKey = PUBLIC_KEY_ADB.getBytes("UTF-8");
                        writeMessage(out, A_AUTH, 3, 0, pubKey);
                        
                        // This triggers the TV's popup. The next packet from server will be CNXN or EOF
                    }
                } else if (header[0] == A_CNXN) {
                    // Authenticated successfully!
                    if (header[3] > 0) {
                        skipBytes(in, header[3]); // skip connection string
                    }
                    break;
                } else {
                    return false; // Unexpected packet
                }
            }

            // Step 3: OPEN shell stream
            byte[] cmd = ("shell:" + command + "\0").getBytes("UTF-8");
            writeMessage(out, A_OPEN, 1, 0, cmd);

            // Step 4: Read OKAY
            int[] header = readHeader(in);
            if (header[3] > 0) {
                skipBytes(in, header[3]);
            }

            // Give the command time to execute
            Thread.sleep(500);

            return header[0] == A_OKAY;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private static void writeMessage(OutputStream out, int cmd, int arg0, int arg1, byte[] data)
            throws IOException {
        byte[] header = new byte[24];
        putLE32(header, 0, cmd);
        putLE32(header, 4, arg0);
        putLE32(header, 8, arg1);
        putLE32(header, 12, data.length);
        putLE32(header, 16, checksum(data));
        putLE32(header, 20, cmd ^ 0xFFFFFFFF);
        out.write(header);
        out.write(data);
        out.flush();
    }

    private static int[] readHeader(InputStream in) throws IOException {
        byte[] buf = new byte[24];
        readFully(in, buf);
        return new int[]{
                getLE32(buf, 0),   // command
                getLE32(buf, 4),   // arg0
                getLE32(buf, 8),   // arg1
                getLE32(buf, 12),  // data_length
                getLE32(buf, 16),  // data_check
                getLE32(buf, 20)   // magic
        };
    }

    private static void readFully(InputStream in, byte[] buf) throws IOException {
        int read = 0;
        while (read < buf.length) {
            int n = in.read(buf, read, buf.length - read);
            if (n < 0) throw new IOException("EOF");
            read += n;
        }
    }

    private static void skipBytes(InputStream in, int count) throws IOException {
        byte[] buf = new byte[Math.min(count, MAX_PAYLOAD)];
        int remaining = count;
        while (remaining > 0) {
            int n = in.read(buf, 0, Math.min(remaining, buf.length));
            if (n < 0) throw new IOException("EOF skipping bytes");
            remaining -= n;
        }
    }

    private static int checksum(byte[] data) {
        int sum = 0;
        for (byte b : data) {
            sum += (b & 0xFF);
        }
        return sum;
    }

    private static void putLE32(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value);
        buf[offset + 1] = (byte) (value >> 8);
        buf[offset + 2] = (byte) (value >> 16);
        buf[offset + 3] = (byte) (value >> 24);
    }

    private static int getLE32(byte[] buf, int offset) {
        return (buf[offset] & 0xFF)
                | ((buf[offset + 1] & 0xFF) << 8)
                | ((buf[offset + 2] & 0xFF) << 16)
                | ((buf[offset + 3] & 0xFF) << 24);
    }
}
