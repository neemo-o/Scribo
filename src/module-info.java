module Scribo {
	requires javafx.controls;
	requires javafx.fxml;
	requires javafx.graphics;
	requires java.prefs;
	
	opens application to javafx.graphics, javafx.fxml;
}
