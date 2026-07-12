package com.sleeptimer.tv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Minimal ADB protocol client.
 * Connects to the local ADB daemon over TCP and sends shell commands.
 * No authentication support — budget TVs with ADB TCP don't require pairing.
 */
public class AdbClient {

    // ADB protocol command constants (little-endian ASCII)
    private static final int A_CNXN = 0x4e584e43;
    private static final int A_AUTH = 0x48545541;
    private static final int A_OPEN = 0x4e45504f;
    private static final int A_OKAY = 0x59414b4f;

    private static final int A_VERSION = 0x01000000;
    private static final int MAX_PAYLOAD = 4096;
    private static final int TIMEOUT_MS = 3000;

    /**
     * Sends KEYCODE_SLEEP (223) via ADB shell on the given port.
     * Falls back to KEYCODE_POWER (26) if sleep keycode fails.
     * @return true if the command was sent successfully
     */
    public static boolean sendSleepCommand(int port) {
        // Try KEYCODE_SLEEP first
        if (sendShellCommand(port, "input keyevent 223")) {
            return true;
        }
        // Fallback to KEYCODE_POWER
        return sendShellCommand(port, "input keyevent 26");
    }

    /**
     * Sends an arbitrary shell command via ADB protocol.
     */
    public static boolean sendShellCommand(int port, String command) {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress("127.0.0.1", port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Step 1: Send CNXN
            byte[] identity = "host::\0".getBytes("UTF-8");
            writeMessage(out, A_CNXN, A_VERSION, MAX_PAYLOAD, identity);

            // Step 2: Read server CNXN
            int[] header = readHeader(in);
            if (header[0] == A_AUTH) {
                return false; // Auth required, not supported
            }
            if (header[0] != A_CNXN) {
                return false;
            }
            if (header[3] > 0) {
                skipBytes(in, header[3]); // skip CNXN payload
            }

            // Step 3: OPEN shell stream
            byte[] cmd = ("shell:" + command + "\0").getBytes("UTF-8");
            writeMessage(out, A_OPEN, 1, 0, cmd);

            // Step 4: Read OKAY
            header = readHeader(in);
            if (header[3] > 0) {
                skipBytes(in, header[3]);
            }

            // Give the command time to execute
            Thread.sleep(500);

            return header[0] == A_OKAY;

        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
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
        int read = 0;
        while (read < 24) {
            int n = in.read(buf, read, 24 - read);
            if (n < 0) throw new IOException("EOF reading ADB header");
            read += n;
        }
        return new int[]{
                getLE32(buf, 0),   // command
                getLE32(buf, 4),   // arg0
                getLE32(buf, 8),   // arg1
                getLE32(buf, 12),  // data_length
                getLE32(buf, 16),  // data_check
                getLE32(buf, 20)   // magic
        };
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
