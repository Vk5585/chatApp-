package chat;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.net.*;
import java.io.*;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import javafx.scene.layout.Priority;
import javafx.scene.control.ScrollPane;

public class client extends Application {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private TextArea chatArea;
    private TextField inputField, nicknameField;
    private static final String AES_KEY = "1234567890123456";

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage stage) {
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setPrefHeight(200);
        chatArea.setMaxHeight(200);
        VBox.setVgrow(chatArea, Priority.NEVER);
        inputField = new TextField();
        FlowPane emojiBar = new FlowPane(5, 5);
        emojiBar.setStyle("-fx-background-color: yellow;");
        emojiBar.setMinHeight(50);
        emojiBar.setPrefWrapLength(400);

        String[] emojis = {"😀", "😂", "😍", "👍", "😭", "💡",
                "🖐", "😀", "😃", "😄", "😁", "😆",
                "🥹", "😅", "😂", "🤣", "🥲", "☺",
                "😙", "😗", "😘", "🥰", "😍", "😌",
                "😉", "🙃", "🙂", "😇", "😊", "😚",
                "😋", "😛", "😝", "😜", "🤪", "🤨",
                "🧐"};

        for (String emoji : emojis) {
            Button btn = new Button(emoji);
            btn.setStyle("-fx-font-size: 24px; -fx-min-width:40px; -fx-min-height:40px;");
            btn.setOnAction(e -> inputField.appendText(emoji));
            emojiBar.getChildren().add(btn);
        }
        // emojiBar.getChildren().add(new Label("TEST"));

        ScrollPane emojiScrollPane = new ScrollPane(emojiBar);
        emojiScrollPane.setMaxHeight(150);
        emojiScrollPane.setFitToWidth(true);
        emojiScrollPane.setStyle("-fx-background-color: yellow;");

        Button sendBtn = new Button("Send");
        Button viewHistoryBtn = new Button("View History");
        HBox buttonBox = new HBox(10, sendBtn, viewHistoryBtn);

        VBox root = new VBox(10, chatArea, emojiScrollPane, inputField, buttonBox);
        root.setPadding(new Insets(10));

        nicknameField = new TextField();

        Button connectBtn = new Button("Connect");
        sendBtn = new Button("Send");
        connectBtn = new Button("Connect");
        sendBtn = new Button("Send");


        viewHistoryBtn = new Button(" View History");
        viewHistoryBtn.setOnAction(e -> {
            try {
                out.println("/history");
            } catch (Exception ex) {
                chatArea.appendText("Error requesting history. ");
            }
        });

        VBox topPane = new VBox(new Label("Nickname:"), nicknameField, connectBtn);

        topPane = new VBox(new Label("Nickname:"), nicknameField, connectBtn);
        buttonBox = new HBox(10, sendBtn, viewHistoryBtn);
        root = new VBox(10, chatArea, emojiBar, inputField, buttonBox);
        root.setPadding(new Insets(10));

        connectBtn.setOnAction(e -> connectToServer());
        sendBtn.setOnAction(e -> sendMessage());

        Scene scene = new Scene(new VBox(topPane, root), 400, 400);
        stage.setScene(scene);
        stage.setTitle("JavaFX Chat Client");
        stage.show();
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String prompt = in.readLine();
            chatArea.appendText(prompt + "\n");
            out.println(nicknameField.getText());

            chatArea.appendText("Connected as " + nicknameField.getText() + "\n");

            new Thread(() -> {
                String resp;
                try {
                    while ((resp = in.readLine()) != null) {
                        String decrypted;
                        try {
                            decrypted = decrypt(resp);
                        } catch (Exception ex) {
                            decrypted = resp;
                        }
                        String finalDecrypted = decrypted;
                        Platform.runLater(() -> chatArea.appendText(finalDecrypted + "\n"));
                    }
                } catch (Exception e) {
                    chatArea.appendText("Connection closed.\n");
                }
            }).start();
        } catch (Exception e) {
            chatArea.appendText("Could not connect.\n");
        }
    }

    private void sendMessage() {
        try {
            String msg = inputField.getText();
            if (!msg.isEmpty()) {
                out.println(encrypt(msg));
                inputField.clear();
            }
        } catch (Exception e) {
            chatArea.appendText("Failed to send.\n");
        }
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
}