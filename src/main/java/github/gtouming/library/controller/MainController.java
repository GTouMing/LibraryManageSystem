package github.gtouming.library.controller;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.util.Duration;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

public class MainController {

    @FXML private Button borrowBtn;
    @FXML private Button personalBtn;
    @FXML private Button readerBtn;
    @FXML private Button bookBtn;
    @FXML private Button logoutBtn;
    @FXML private StackPane contentPane;
    @FXML private HBox confirmBar;
    @FXML private Label confirmLabel;
    @FXML private Button confirmOkBtn;
    @FXML private Button confirmCancelBtn;
    @FXML private Button minBtn;
    @FXML private Button maxBtn;
    @FXML private Button closeBtn;

    private Button activeButton = null;

    @FXML private Region dragRegion;

    private double dragStartX;
    private double dragStartY;
    private boolean isAdmin;
    private String readerAccount;
    private Runnable confirmAction;
    private PauseTransition messageTimer;

    private static MainController instance;

    public static MainController getInstance() { return instance; }

    /**
     * 由 LoginController 在加载后调用，根据角色控制侧边栏按钮显隐
     */
    public void setUserType(boolean isAdmin, String readerAccount) {
        this.isAdmin = isAdmin;
        this.readerAccount = readerAccount;
        if (isAdmin) {
            personalBtn.setVisible(false);
            personalBtn.setManaged(false);
            switchPage("borrow", borrowBtn);
        } else {
            readerBtn.setVisible(false);
            readerBtn.setManaged(false);
            bookBtn.setVisible(false);
            bookBtn.setManaged(false);
            switchPage("borrow", borrowBtn);
        }
    }

    public void initialize() {
        instance = this;
        // 默认页面：initialize 时 role 尚未注入，先加载 borrow，
        // setUserType 调用后再根据角色切换到对应默认页
        loadPage("borrow");
        setActiveButton(borrowBtn);

        borrowBtn.setOnAction(e -> switchPage("borrow", borrowBtn));
        personalBtn.setOnAction(e -> switchPage("personal", personalBtn));
        readerBtn.setOnAction(e -> switchPage("reader", readerBtn));
        bookBtn.setOnAction(e -> switchPage("book", bookBtn));
        logoutBtn.setOnAction(e -> logout());

        confirmOkBtn.setOnAction(e -> {
            Runnable action = confirmAction;
            hideConfirm();
            if (action != null) action.run();
        });
        confirmCancelBtn.setOnAction(e -> hideConfirm());

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

    private void switchPage(String pageName, Button button) {
        loadPage(pageName);
        setActiveButton(button);
    }

    private void loadPage(String pageName) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/fxml/" + pageName + ".fxml")));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof BorrowController bc) {
                bc.setAdmin(isAdmin, readerAccount);
            } else if (controller instanceof PersonalController pc) {
                pc.setUser(readerAccount);
            }

            contentPane.getChildren().clear();
            contentPane.getChildren().add(root);
        } catch (IOException e) {
           System.err.println(e.getMessage());
        }
    }

    private void setActiveButton(Button button) {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("active");
        }
        activeButton = button;
        button.getStyleClass().add("active");
    }

    /**
     * 显示纯消息（3 秒后自动消失）。
     * 与确认模式互斥：调用此方法会取消当前确认操作。
     */
    public void showMessage(String text) {
        cancelMessageTimer();
        confirmAction = null;

        confirmLabel.setText(text);
        confirmOkBtn.setVisible(false); confirmOkBtn.setManaged(false);
        confirmCancelBtn.setVisible(false); confirmCancelBtn.setManaged(false);
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);

        messageTimer = new PauseTransition(Duration.seconds(3));
        messageTimer.setOnFinished(e -> hideConfirm());
        messageTimer.play();
    }

    /**
     * 显示确认栏（含确定 / 取消按钮）。
     * 与消息模式互斥：调用此方法会取消当前消息计时器。
     */
    public void showConfirm(String text, Runnable onOk) {
        cancelMessageTimer();
        confirmAction = onOk;

        confirmLabel.setText(text);
        confirmOkBtn.setVisible(true); confirmOkBtn.setManaged(true);
        confirmCancelBtn.setVisible(true); confirmCancelBtn.setManaged(true);
        confirmBar.setVisible(true);
        confirmBar.setManaged(true);
    }

    private void hideConfirm() {
        cancelMessageTimer();
        confirmBar.setVisible(false);
        confirmBar.setManaged(false);
        confirmLabel.setText("");
        confirmAction = null;
    }

    private void cancelMessageTimer() {
        if (messageTimer != null) {
            messageTimer.stop();
            messageTimer = null;
        }
    }

    private void logout() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource("/fxml/login.fxml")));
            Parent root = loader.load();

            Stage mainStage = new Stage();
            Scene scene = new Scene(root, 600, 400);
            scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/style.css")).toExternalForm());
            scene.setFill(Color.TRANSPARENT);

            mainStage.initStyle(StageStyle.TRANSPARENT);
            mainStage.setTitle("图书馆管理系统");
            mainStage.setScene(scene);
            mainStage.setResizable(false);
            mainStage.show();

            // 关闭登录窗口
            Stage loginStage = (Stage) logoutBtn.getScene().getWindow();
            loginStage.close();
        } catch (IOException e) {
           System.err.println(e.getMessage());
        }
    }
}
