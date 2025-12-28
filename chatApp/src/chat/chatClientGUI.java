package chat;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Base64;

import static java.lang.System.out;

public class chatClientGUI extends JFrame {

    // fields
    private JTextArea chatArea;
    private JTextField inputField;
    private JButton sendButton;
    private static final String AES_KEY = "1234567890123456";

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public chatClientGUI() {
        // constructor code
        super("ChatApp Desktop");

        setSize(400, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

// Center: chat area
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

// Bottom: input + send button
        inputField = new JTextField();
        sendButton = new JButton("Send");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> {
            try {
                sendCurrentText();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        inputField.addActionListener(e -> {
            try {
                sendCurrentText();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
        connectToServer();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);   // use your real port
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            chatArea.append("Connected to server");

            Thread readerThread = new Thread(() -> {
                try {
                    String resp;
                    while ((resp = in.readLine()) != null) {

                        String decrypted;
                        try {
                            decrypted = decrypt(resp);   // your existing decrypt()
                        } catch (Exception ex) {
                            decrypted = resp;
                        }

                        String finalDecrypted = decrypted;

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                chatArea.append(finalDecrypted + "\n\n");
                            }
                        });
                }
                }
                catch (Exception e) {
                    SwingUtilities.invokeLater(() ->
                            chatArea.append("Connection closed.")
                            );
                }
            });
            readerThread.start();

        } catch (Exception e) {
            chatArea.append("Could not connect.");
        }
    }

    private void sendCurrentText() throws Exception {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        chatArea.append("Me: " + text + " ");

        if (out != null) {
            String enc = encrypt(text);   // your old encrypt()
            out.println(enc);
        }

        inputField.setText("");
    }

    private String encrypt(String plainText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes()));
    }

    private String decrypt(String encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec key = new SecretKeySpec(AES_KEY.getBytes(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        return new String(cipher.doFinal(decoded));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            chatClientGUI window = new chatClientGUI();
            window.setVisible(true);
        });
    }
}