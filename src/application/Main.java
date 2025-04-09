package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Parent;

public class Main extends Application {
	@Override
	public void start(Stage primaryStage) throws Exception {
	    Parent root = FXMLLoader.load(getClass().getResource("MainLayout.fxml"));
	    Scene scene = new Scene(root, 750, 600);

	    scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());

	    primaryStage.setTitle("Scribo Studio");
	    primaryStage.setScene(scene);
	    primaryStage.show();
	}

    public static void main(String[] args) {
        launch(args);
    }
}
