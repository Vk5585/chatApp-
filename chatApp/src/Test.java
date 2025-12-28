import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class Test extends Application {
    public void start(Stage stage) {
        HBox emojiBar = new HBox(5);
        emojiBar.setStyle("-fx-background-color: yellow;");
        emojiBar.getChildren().add(new Label("TEST"));
        stage.setScene(new Scene(emojiBar, 500, 100));
        stage.show();
    }
    public static void main(String[] args) { launch(args); }
}