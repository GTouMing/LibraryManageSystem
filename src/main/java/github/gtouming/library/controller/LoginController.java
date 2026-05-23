package github.gtouming.library.controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import github.gtouming.library.util.DatabaseUtil;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class LoginController {

    @FXML private Pane rootPane;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Button minBtn;
    @FXML private Button maxBtn;
    @FXML private Button closeBtn;
    @FXML private HBox confirmBar;
    @FXML private Label confirmLabel;
    @FXML private Button confirmOkBtn;
    @FXML private Button confirmCancelBtn;
    @FXML private Region dragRegion;
    private double dragStartX;
    private double dragStartY;

    public void initialize() {
        loginButton.setOnAction(e -> handleLogin());
        registerButton.setOnAction(e -> handleRegister());

        setupWindowControls();
        setupDragRegion();
    }

    private void setupDragRegion() {
        dragRegion.setOnMousePressed(event -> {
            Stage stage = (Stage) dragRegion.getScene().getWindow();
            dragStartX = event.getScreenX() - stage.getX();
            dragStartY = event.getScreenY() - stage.getY();
        });

        dragRegion.setOnMouseDragged(event -> {
            Stage stage = (Stage) dragRegion.getScene().getWindow();
            stage.setX(event.getScreenX() - dragStartX);
            stage.setY(event.getScreenY() - dragStartY);
        });
    }

    private void setupWindowControls() {
        minBtn.setOnAction(e -> {
            Stage stage = (Stage) minBtn.getScene().getWindow();
            stage.setIconified(true);
        });

        maxBtn.setOnAction(e -> {
            Stage stage = (Stage) maxBtn.getScene().getWindow();
            stage.setMaximized(!stage.isMaximized());
        });

        closeBtn.setOnAction(e -> Platform.exit());
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showMessage("请输入所有必需项");
            return;
        }

        // 管理员硬编码（调试用）
        if ("admin".equals(username) && "admin".equals(password)) {
            navigateToMain(true);
            return;
        }

        Map<String, Object> user = DatabaseUtil.login(username, password);
        if (user != null) {
            navigateToMain(false);
        } else {
            showMessage("账号或密码错误");
        }
    }

    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/fxml/page.fxml")));
            Parent root = loader.load();

            Stage registerStage = new Stage();
            Scene scene = new Scene(root, 480, 600);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/css/style.css")).toExternalForm());
            scene.setFill(Color.TRANSPARENT);

            registerStage.initStyle(StageStyle.TRANSPARENT);
            registerStage.setTitle("读者注册");
            registerStage.setScene(scene);
            registerStage.setResizable(false);
            registerStage.show();

//            Stage loginStage = (Stage) registerButton.getScene().getWindow();
//            loginStage.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void showMessage(String text) {
        confirmLabel.setText(text);
        confirmOkBtn.setVisible(false); confirmOkBtn.setManaged(false);
        confirmCancelBtn.setVisible(false); confirmCancelBtn.setManaged(false);
        confirmBar.setVisible(true); confirmBar.setManaged(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> {
            confirmBar.setVisible(false);
            confirmBar.setManaged(false);
        });
        pause.play();
    }

    private void navigateToMain(boolean isAdmin) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/main.fxml")));
            Parent root = loader.load();

            String account = usernameField.getText().trim();
            MainController mainController = loader.getController();
            mainController.setUserType(isAdmin, account);

            Stage mainStage = new Stage();
            Scene scene = new Scene(root, 1000, 600);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
            scene.setFill(Color.TRANSPARENT);

            mainStage.initStyle(StageStyle.TRANSPARENT);
            mainStage.setTitle("图书馆管理系统");
            mainStage.setScene(scene);
            mainStage.setResizable(false);
            mainStage.show();

            // 关闭登录窗口
            Stage loginStage = (Stage) loginButton.getScene().getWindow();
            loginStage.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}