package com.cinestream.tv;

import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;

/** Independent test-only child fixture for host/Android. Never packaged in the application APK. */
public final class FixtureCore {
    public static void main(String[] args) throws Exception {
        String instance = args[0], mode = args[1];
        String secret = System.getenv("CINEDBKTV_CORE_TOKEN");
        try (FileOutputStream file = new FileOutputStream("fixture-owned-file")) { file.write(1); }
        if (mode.equals("exit")) return;
        if (mode.equals("timeout")) { new CountDownLatch(1).await(); return; }
        try (ServerSocket server = new ServerSocket(0, 8, InetAddress.getByName("127.0.0.1"))) {
            if (mode.equals("flood")) {
                for (int i = 0; i < 100000; i++) System.out.print('x');
                System.out.println();
            }
            System.out.println("HONGGUO_TV_READY " + server.getLocalPort() + " " + instance);
            System.out.flush();
            while (true) try (Socket socket = server.accept()) {
                socket.setSoTimeout(2000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "US-ASCII"));
                String request = reader.readLine();
                boolean authenticated = mode.equals("no-auth");
                for (int i = 0; i < 64; i++) {
                    String line = reader.readLine();
                    if (line == null || line.isEmpty()) break;
                    if (line.equalsIgnoreCase("Authorization: Bearer " + secret)) authenticated = true;
                }
                byte[] body; int status = 200;
                if (!authenticated) { status = 403; body = new byte[0]; }
                else if (request.startsWith("GET /api/v1/status ")) {
                    body = ("{\"ok\":true,\"service\":\"hongguo-tv-core\",\"instance_id\":\"" + instance +
                        "\",\"version\":\"" + (mode.equals("wrong-version") ? "wrong" : "fixture-v1") + "\"}").getBytes("UTF-8");
                } else if (request.startsWith("GET /fixture.wav ")) {
                    // One second of independently synthesized unprotected 440 Hz PCM audio.
                    ByteBuffer wav = ByteBuffer.allocate(16044).order(ByteOrder.LITTLE_ENDIAN);
                    wav.put("RIFF".getBytes("US-ASCII")).putInt(16036).put("WAVEfmt ".getBytes("US-ASCII")).putInt(16);
                    wav.putShort((short)1).putShort((short)1).putInt(8000).putInt(16000).putShort((short)2).putShort((short)16);
                    wav.put("data".getBytes("US-ASCII")).putInt(16000);
                    for (int i = 0; i < 8000; i++) wav.putShort((short)(Math.sin(i * 2 * Math.PI * 440 / 8000) * 4000));
                    body = wav.array();
                } else { status = 404; body = new byte[0]; }
                OutputStream output = socket.getOutputStream();
                output.write(("HTTP/1.1 " + status + " Fixture\r\nContent-Length: " + body.length + "\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));
                output.write(body); output.flush();
            }
        }
    }
}
