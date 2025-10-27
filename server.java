package chat;

import java.net.*;
import java.io.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class server {
    private static final int PORT = 12345;
    private final Map<String, ClientHandler> clients = new HashMap<>();
    private static final String AES_KEY = "1234567890123456";
    private static final String HISTORY_FILE = "chat_history.txt";
    private static final Set<String> onlineUsers = Collections.synchronizedSet(new HashSet<>());

    private void appendToHistory(String msg) {
        try (FileWriter fw = new FileWriter(HISTORY_FILE, true)) {
            fw.write(msg + " ");
        } catch (IOException e) { }
    }


    private void sendHistory(PrintWriter out) {
        try (BufferedReader br = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.println(line);
            }
        } catch (IOException e) { }
    }


    private void broadcastUserList() {
        String userListMsg = "USERLIST:" + String.join(",", onlineUsers);
        for (ClientHandler handler : clients.values()) {
            handler.out.println(userListMsg);
        }
    }


    public static void main(String[] args) throws IOException {
        new server().startServer();
    }

    public void startServer() throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Server started on port " + PORT);
        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(new ClientHandler(socket)).start();
        }
    }

    public String decrypt(String encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        return new String(cipher.doFinal(decoded));
    }

    public String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes()));
    }

    class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        }

        @Override
        public void run() {
            try {
                out.println("Enter nickname:");
                nickname = in.readLine();
                if (nickname == null || nickname.trim().isEmpty()) {
                    out.println("No nickname provided. Disconnecting.");
                    socket.close();
                    return;
                }
                System.out.println(nickname + " connected.");
                clients.put(nickname, this);
                broadcastLog(nickname + " connected.", null);
                onlineUsers.add(nickname);
                broadcastUserList();
                sendHistory(out);

                String input;
                while ((input = in.readLine()) != null) {
                    String message = decrypt(input);
                    String time = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                    String msgWithTime = "[" + time + "] " + nickname + ": " + message;
                    appendToHistory(msgWithTime);
                    broadcast(msgWithTime, null);

                    if (message.equals("/history")) {
                        sendHistory(out);
                    }

                    // Private chat: /w target message
                   else if (message.startsWith("/w ")) {
                        String[] parts = message.split(" ", 3);
                        if (parts.length >= 3) {
                            String target = parts[1];
                            String privateMsg = parts[2];
                            if (clients.containsKey(target)) {
                                clients.get(target).out.println(encrypt("[Private] " + nickname + ": " + privateMsg));
                            } else {
                                out.println(encrypt("[LOG] User '" + target + "' not found!"));
                            }
                        } else {
                            out.println(encrypt("[LOG] Usage: /w <nickname> <message>"));
                        }
                    } else {

                        broadcast(nickname + ": " + message, nickname);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("User left: " + nickname);
            } finally {
                clients.remove(nickname);
                try { broadcastLog(nickname + " disconnected.", null); } catch (Exception ignored) {}
                try { socket.close(); } catch (IOException ignored) {}
                onlineUsers.remove(nickname);
                broadcastUserList();
            }
        }

        private void broadcast(String msg, String exclude) throws Exception {
            String encryptedMsg = encrypt(msg);
            for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
                if (!entry.getKey().equals(exclude)) {
                    entry.getValue().out.println(encryptedMsg);
                }
            }
        }
        private void sendHistory(PrintWriter out) throws Exception {
            try (BufferedReader br = new BufferedReader(new FileReader(HISTORY_FILE))) {
                String line;
                out.println(encrypt("[SYSTEM] === Chat History ==="));
                while ((line = br.readLine()) != null) {
                    out.println(encrypt(line));
                }
                out.println(encrypt("[SYSTEM] === End of History ==="));
            } catch (Exception e) {
                try {
                    out.println(encrypt("[SYSTEM] No chat history available."));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        private void broadcastLog(String msg, String exclude) throws Exception {
            String encryptedMsg = encrypt("[LOG] " + msg);
            for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
                if (exclude == null || !entry.getKey().equals(exclude)) {
                    entry.getValue().out.println(encryptedMsg);
                }
            }
            System.out.println(msg);
        }
    }
}