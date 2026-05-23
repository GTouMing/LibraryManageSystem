package github.gtouming.library;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import github.gtouming.library.util.DatabaseUtil;
import java.util.Objects;

public class App extends Application {

	@Override
	public void init() {
		DatabaseUtil.initTables();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		System.setProperty("file.encoding", "UTF-8");

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
		Parent root = loader.load();

		Scene scene = new Scene(root, 600, 400);
		scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
		scene.setFill(Color.TRANSPARENT);

		primaryStage.initStyle(StageStyle.TRANSPARENT);
		primaryStage.setTitle("图书馆管理系统");
		primaryStage.setScene(scene);
		primaryStage.setResizable(false);
		primaryStage.setOnCloseRequest(e -> {
			System.out.println("借阅记录总数: " + DatabaseUtil.getAllBorrowRecords().size());
		});
		primaryStage.show();
	}

	@Override
	public void stop() {
		System.out.println("应用正常退出，借阅记录总数: " + DatabaseUtil.getAllBorrowRecords().size());
	}
}