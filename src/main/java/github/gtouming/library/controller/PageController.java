package github.gtouming.library.controller;

import github.gtouming.library.util.Db;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;

import github.gtouming.library.util.DatabaseUtil;
import static github.gtouming.library.util.Db.*;

public class PageController {

    public enum FormMode {
        REGISTER, ADD_READER, UPDATE_READER, ADD_BOOK, UPDATE_BOOK, CHANGE_PASSWORD
    }

    @FXML private Label formTitle;
    @FXML private GridPane formGrid;
    @FXML private Button submitBtn;
    @FXML private HBox confirmBar;
    @FXML private Label confirmLabel;
    @FXML private Button confirmOkBtn;
    @FXML private Button confirmCancelBtn;

    @FXML private Button closeBtn;
    @FXML private Pane dragRegion;

    private double dragStartX, dragStartY;
    private Runnable onSuccess;
    private final Map<String, Control> fields = new LinkedHashMap<>();
    private RadioButton maleRadio;
    private ComboBox<String> regionCombo;

    public void initialize() {
        setupWindowControls();
        buildForm(FormMode.REGISTER, null);
    }

    /**
     * 配置表单模式。在 load() 之后、显示窗口之前调用。
     */
    public void setMode(FormMode mode, Map<String, String> initialData, Runnable onSuccess) {
        this.onSuccess = onSuccess;
        buildForm(mode, initialData);
    }

    // ---- form builder ----

    private void buildForm(FormMode mode, Map<String, String> data) {
        formGrid.getChildren().clear();
        fields.clear();
        int row = 0;

        switch (mode) {
            case REGISTER -> {
                formTitle.setText("读者注册");
                submitBtn.setText("注册");

                addField(row++, "账号：", Reader.READER_ACCOUNT, new TextField(), true);
                addField(row++, "昵称：", Reader.READER_NAME, new TextField(), true);
                addField(row++, "密码：", Reader.PASSWORD, new PasswordField(), true);
                addField(row++, "确认密码：", "confirm", new PasswordField(), true);
                addGenderRow(row++);
                addRegionRow(row);
                submitBtn.setOnAction(e -> handleRegister());
            }
            case ADD_READER -> {
                formTitle.setText("添加读者");
                submitBtn.setText("添加");

                addField(row++, "账号：", Reader.READER_ACCOUNT, new TextField(), true);
                addField(row++, "昵称：", Reader.READER_NAME, new TextField(), true);
                addField(row++, "密码：", Reader.PASSWORD, new PasswordField(), true);
                addGenderRow(row++);
                addRegionRow(row);
                submitBtn.setOnAction(e -> handleAddReader());
            }
            case UPDATE_READER -> {
                formTitle.setText("修改读者");
                submitBtn.setText("保存");

                String account = data != null ? data.getOrDefault(Reader.READER_ACCOUNT, "") : "";

                addField(row++, "账号：", Reader.READER_ACCOUNT, new TextField(), true);
                addField(row++, "昵称：", Reader.READER_NAME, new TextField(), true);
                addField(row++, "密码：", Reader.PASSWORD, new TextField(), true);
                addGenderRow(row++);
                addRegionRow(row);
                submitBtn.setOnAction(e -> handleUpdateReader(account));
            }
            case ADD_BOOK -> {
                formTitle.setText("添加图书");
                submitBtn.setText("添加");

                addField(row++, "图书号：", Book.BOOK_ID, new TextField(), true);
                addField(row++, "书名：", Book.TITLE, new TextField(), true);
                addField(row++, "作者：", Book.AUTHOR, new TextField(), false);
                addField(row++, "分类：", Book.CATEGORY, new TextField(), false);
                addField(row++, "出版社", Book.PUBLISHER_NAME, new TextField(), false);
                addField(row, "复本数", Book.COPY_COUNT, new TextField(), false);
                submitBtn.setOnAction(e -> handleAddBook());
            }
            case UPDATE_BOOK -> {
                formTitle.setText("修改图书");
                submitBtn.setText("保存");

                String bookId = data != null ? data.getOrDefault(Book.BOOK_ID, "") : "";
                addReadonlyRow(row++, "图书号：", bookId);

                addField(row++, "书名：", Book.TITLE, new TextField(), true);
                addField(row++, "作者：", Book.AUTHOR, new TextField(), false);
                addField(row++, "分类：", Book.CATEGORY, new TextField(), false);
                addField(row++, "出版社", Book.PUBLISHER_NAME, new TextField(), false);
                addField(row, "复本数", Book.COPY_COUNT, new TextField(), false);
                submitBtn.setOnAction(e -> handleUpdateBook(bookId));
            }
            case CHANGE_PASSWORD -> {
                formTitle.setText("修改密码");
                submitBtn.setText("确认");

                String account = data != null ? data.get(Reader.READER_ACCOUNT) : "";
                addReadonlyRow(row++, "账号：", account);

                addField(row++, "旧密码：", "old_password", new PasswordField(), true);
                addField(row++, "新密码：", "new_password", new PasswordField(), true);
                addField(row, "确认密码：", "confirm", new PasswordField(), true);
                submitBtn.setOnAction(e -> handleChangePassword(account));
            }
        }

        if (data != null) {
            for (Map.Entry<String, Control> entry : fields.entrySet()) {
                String val = data.get(entry.getKey());
                if (val != null) {
                    if (entry.getValue() instanceof TextField tf) tf.setText(val);
                }
            }
        }
    }

    // ---- row helpers ----

    private void addField(int row, String labelText, String key, Control input, boolean required) {
        Label label = new Label(labelText);
        label.getStyleClass().add("input-label");

        if (required && input instanceof TextInputControl tic) {
            tic.setPromptText("必填");
        }
        input.getStyleClass().add(input instanceof PasswordField ? "password-field" : "text-field");

        formGrid.add(label, 0, row);
        formGrid.add(input, 1, row);
        fields.put(key, input);
    }

    private void addReadonlyRow(int row, String labelText, String value) {
        Label label = new Label(labelText);
        label.getStyleClass().add("input-label");
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: white;");

        formGrid.add(label, 0, row);
        formGrid.add(valueLabel, 1, row);
    }

    private void addGenderRow(int row) {
        Label label = new Label("性别：");
        label.getStyleClass().add("input-label");

        ToggleGroup genderGroup = new ToggleGroup();
        maleRadio = new RadioButton("男");
        maleRadio.setToggleGroup(genderGroup);
        maleRadio.setSelected(true);
        RadioButton femaleRadio = new RadioButton("女");
        femaleRadio.setToggleGroup(genderGroup);

        HBox box = new HBox(12, maleRadio, femaleRadio);
        box.setAlignment(Pos.CENTER_LEFT);

        formGrid.add(label, 0, row);
        formGrid.add(box, 1, row);
    }

    private void addRegionRow(int row) {
        Label label = new Label("地区：");
        label.getStyleClass().add("input-label");

        regionCombo = new ComboBox<>();
        regionCombo.getStyleClass().add("combo-box");
        regionCombo.setPromptText("请选择地区");
        loadRegions();

        formGrid.add(label, 0, row);
        formGrid.add(regionCombo, 1, row);
    }

    private void loadRegions() {
        List<Map<String, Object>> regions = DatabaseUtil.getAllRegions();
        regionCombo.getItems().clear();
        for (Map<String, Object> r : regions) {
            regionCombo.getItems().add(r.get(Db.Region.REGION_NAME).toString());
        }
        if (!regions.isEmpty()) {
            regionCombo.getSelectionModel().selectFirst();
        }
    }

    // ---- submit handlers ----

    private void handleRegister() {
        String account = getText(Reader.READER_ACCOUNT);
        String name = getText(Reader.READER_NAME);
        String password = getPassword(Reader.PASSWORD);
        String confirm = getPassword("confirm");

        if (account.isEmpty() || name.isEmpty() || password.isEmpty()) {
            showMessage("请填写所有必填项"); return;
        }
        if (account.length() < 3) { showMessage("账号至少 3 位"); return; }
        if (password.length() < 4) { showMessage("密码至少 4 位"); return; }
        if (!password.equals(confirm)) { showMessage("两次密码不一致"); return; }

        String gender = maleRadio.isSelected() ? "男" : "女";
        Integer regionCode = null;
        int idx = regionCombo.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            List<Map<String, Object>> regions = DatabaseUtil.getAllRegions();
            regionCode = (Integer) regions.get(idx).get(Db.Region.REGION_CODE);
        }

        if (DatabaseUtil.register(account, password, name, gender, regionCode)) {
            showMessage("注册成功，请返回登录");
        } else {
            showMessage("注册失败，账号可能已存在");
        }
    }

    private void handleAddReader() {
        String account = getText(Reader.READER_ACCOUNT);
        String name = getText(Reader.READER_NAME);
        String password = getPassword(Reader.PASSWORD);

        if (account.isEmpty() || name.isEmpty() || password.isEmpty()) {
            showMessage("请填写所有必填项"); return;
        }


        String gender = maleRadio.isSelected() ? "男" : "女";
        Integer regionCode = null;
        int idx = regionCombo.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            List<Map<String, Object>> regions = DatabaseUtil.getAllRegions();
            regionCode = (Integer) regions.get(idx).get(Db.Region.REGION_CODE);
        }
        if (DatabaseUtil.register(account, password, name, gender, regionCode)) {
            showMessage("添加成功");
            if (onSuccess != null) onSuccess.run();
        } else {
            showMessage("添加失败，账号可能已存在");
        }
    }

    private void handleUpdateReader(String account) {
        String name = getText(Reader.READER_NAME);
        String password = getText(Reader.PASSWORD);

        if (account.isEmpty() || name.isEmpty() || password.isEmpty()) {
            showMessage("请填写所有必填项"); return;
        }
        String gender = maleRadio.isSelected() ? "男" : "女";
        Integer regionCode = null;
        int idx = regionCombo.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            List<Map<String, Object>> regions = DatabaseUtil.getAllRegions();
            regionCode = (Integer) regions.get(idx).get(Db.Region.REGION_CODE);
        }
        Integer count = parseCopyCount();
        if (count != null && count < 0) return;
        DatabaseUtil.updateReader(account, name, gender, regionCode);
        DatabaseUtil.changePassword(account, (String) DatabaseUtil.getReaderByAccount(account).get(Reader.PASSWORD), password);
        showMessage("修改成功");
        if (onSuccess != null) onSuccess.run();
    }

    private void handleAddBook() {
        String bookId = getText(Book.BOOK_ID);
        String title = getText(Book.TITLE);
        if (bookId.isEmpty() || title.isEmpty()) {
            showMessage("图书号和书名必填"); return;
        }
        Integer count = parseCopyCount();
        if (count != null && count < 0) return;
        if (DatabaseUtil.addBook(bookId, getText(Book.CATEGORY), title, getText(Book.AUTHOR), getText(Book.PUBLISHER_NAME), count)) {
            showMessage("添加成功");
            if (onSuccess != null) onSuccess.run();
        } else {
            showMessage("添加失败，图书可能已存在");
        }
    }

    private void handleUpdateBook(String bookId) {
        String title = getText(Book.TITLE);
        if (title.isEmpty()) { showMessage("书名不能为空"); return; }
        Integer count = parseCopyCount();
        if (count != null && count < 0) return;
        DatabaseUtil.updateBook(bookId, getText(Book.CATEGORY), title, getText(Book.AUTHOR), getText(Book.PUBLISHER_NAME), count);
        showMessage("修改成功");
        if (onSuccess != null) onSuccess.run();
    }

    private Integer parseCopyCount() {
        String s = getText(Book.COPY_COUNT);
        if (s.isEmpty()) return null;
        try {
            int n = Integer.parseInt(s);
            if (n < 0) {
                showMessage("复本数不能为负数");
                return -1;
            }
            return n;
        } catch (NumberFormatException e) {
            showMessage("复本数必须为数字");
            return -1;
        }
    }

    private void handleChangePassword(String account) {
        String oldPwd = getPassword("old_password");
        String newPwd = getPassword("new_password");
        String confirm = getPassword("confirm");

        if (oldPwd.isEmpty() || newPwd.isEmpty() || confirm.isEmpty()) {
            showMessage("请填写所有必填项"); return;
        }
        if (!newPwd.equals(confirm)) { showMessage("两次密码不一致"); return; }
        if (DatabaseUtil.changePassword(account, oldPwd, newPwd)) {
            showMessage("密码修改成功");
            if (onSuccess != null) onSuccess.run();
        } else {
            showMessage("旧密码错误");
        }
    }

    // ---- helpers ----

    private String getText(String key) {
        Control c = fields.get(key);
        return c instanceof TextField tf ? tf.getText().trim() : "";
    }

    private String getPassword(String key) {
        Control c = fields.get(key);
        return c instanceof PasswordField pf ? pf.getText() : "";
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

    // ---- window ----

    private void setupWindowControls() {
        closeBtn.setOnAction(e -> ((Stage) closeBtn.getScene().getWindow()).close());

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

    // ---- static factory ----

    /**
     * 以指定模式打开表单窗口。
     * @param mode        表单模式
     * @param initialData 预填数据（可为 null）
     * @param onSuccess   提交成功后回调（可为 null）
     */
    public static void open(FormMode mode, Map<String, String> initialData, Runnable onSuccess) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                    PageController.class.getResource("/fxml/page.fxml")));
            Parent root = loader.load();
            PageController ctrl = loader.getController();
            ctrl.setMode(mode, initialData, onSuccess);

            Stage stage = new Stage();
            Scene scene = new Scene(root, 480, 480);
            scene.getStylesheets().add(Objects.requireNonNull(
                    PageController.class.getResource("/css/style.css")).toExternalForm());
            scene.setFill(Color.TRANSPARENT);

            stage.initStyle(StageStyle.TRANSPARENT);
            stage.setResizable(false);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
