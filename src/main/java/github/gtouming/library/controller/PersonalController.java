package github.gtouming.library.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import github.gtouming.library.util.DatabaseUtil;
import static github.gtouming.library.util.Db.*;

public class PersonalController {

    @FXML private TextField accountField;
    @FXML private TextField nameField;
    @FXML private RadioButton maleRadio;
    @FXML private RadioButton femaleRadio;
    @FXML private ComboBox<String> regionCombo;
    @FXML private Button saveBtn;
    @FXML private Button changePwdBtn;
    @FXML private Button deleteBtn;
    @FXML private Label borrowCountLabel;
    @FXML private Label currentBorrowLabel;

    private String readerAccount;

    public void setUser(String account) {
        this.readerAccount = account;
        loadUserInfo();
        loadStats();
    }

    public void initialize() {
        ToggleGroup genderGroup = new ToggleGroup();
        maleRadio.setToggleGroup(genderGroup);
        femaleRadio.setToggleGroup(genderGroup);

        loadRegions();

        saveBtn.setOnAction(e -> saveUserInfo());
        changePwdBtn.setOnAction(e -> changePassword());
        deleteBtn.setOnAction(e -> deleteAccount());
    }

    private void loadRegions() {
        regionCombo.getItems().clear();
        for (Map<String, Object> r : DatabaseUtil.getAllRegions()) {
            regionCombo.getItems().add(r.get(Region.REGION_NAME).toString());
        }
    }

    private void loadUserInfo() {
        if (readerAccount == null) return;
        Map<String, Object> user = DatabaseUtil.getReaderByAccount(readerAccount);
        if (user == null) return;

        accountField.setText((String) user.get(Reader.READER_ACCOUNT));
        nameField.setText((String) user.get(Reader.READER_NAME));

        if ("男".equals(user.get(Reader.GENDER))) {
            maleRadio.setSelected(true);
        } else {
            femaleRadio.setSelected(true);
        }

        String regionName = (String) user.get(Reader.REGION_NAME);
        if (regionName != null) {
            regionCombo.getSelectionModel().select(regionName);
        }
    }

    private void loadStats() {
        if (readerAccount == null) return;

        List<Map<String, Object>> history = DatabaseUtil.getBorrowHistory(readerAccount);
        borrowCountLabel.setText(String.valueOf(history.size()));

        List<Map<String, Object>> current = DatabaseUtil.getCurrentBorrows(readerAccount);
        currentBorrowLabel.setText(String.valueOf(current.size()));
    }

    private void saveUserInfo() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            MainController.getInstance().showMessage("昵称不能为空");
            return;
        }
        String gender = maleRadio.isSelected() ? "男" : "女";
        int idx = regionCombo.getSelectionModel().getSelectedIndex();
        Integer rc = idx >= 0
                ? (Integer) DatabaseUtil.getAllRegions().get(idx).get(Region.REGION_CODE)
                : null;
        if (DatabaseUtil.updateReader(readerAccount, name, gender, rc)) {
            MainController.getInstance().showMessage("保存成功");
            loadUserInfo();
        } else {
            MainController.getInstance().showMessage("保存失败");
        }
    }

    private void changePassword() {
        Map<String, String> data = new HashMap<>();
        data.put(Reader.READER_ACCOUNT, readerAccount);
        PageController.open(PageController.FormMode.CHANGE_PASSWORD, data, null);
    }

    private void deleteAccount() {
        MainController.getInstance().showConfirm("确定要注销当前账号吗？此操作不可撤销。", () -> {
            if (DatabaseUtil.deleteReaderByAdmin(readerAccount)) {
                navigateToLogin();
            } else {
                MainController.getInstance().showMessage("注销失败");
            }
        });
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    getClass().getResource("/fxml/login.fxml")));
            Parent root = loader.load();

            Stage stage = new Stage();
            Scene scene = new Scene(root, 600, 400);
            scene.getStylesheets().add(Objects.requireNonNull(
                    getClass().getResource("/css/style.css")).toExternalForm());
            scene.setFill(Color.TRANSPARENT);

            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setTitle("图书馆管理系统");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();

            Stage currentStage = (Stage) deleteBtn.getScene().getWindow();
            currentStage.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
