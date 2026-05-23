package github.gtouming.library.controller;

import github.gtouming.library.util.DatabaseUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static github.gtouming.library.util.Db.*;

public class ReaderController {

    @FXML private TextField searchField;
    @FXML private Button addReaderBtn;
    @FXML public Button updateReaderBtn;
    @FXML private Button deleteReaderBtn;
    @FXML private TableView<Map<String, Object>> readerTable;
    @FXML private TableColumn<Map<String, Object>, String> accountCol;
    @FXML private TableColumn<Map<String, Object>, String> nameCol;
    @FXML private TableColumn<Map<String, Object>, String> genderCol;
    @FXML private TableColumn<Map<String, Object>, String> regionCol;
    @FXML private TableColumn<Map<String, Object>, String> borrowCountCol;

    public void initialize() {
        readerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        accountCol.setCellValueFactory(data -> cell(data.getValue(), Reader.READER_ACCOUNT));
        nameCol.setCellValueFactory(data -> cell(data.getValue(), Reader.READER_NAME));
        genderCol.setCellValueFactory(data -> cell(data.getValue(), Reader.GENDER));
        regionCol.setCellValueFactory(data -> cell(data.getValue(), Reader.REGION_NAME));
        borrowCountCol.setCellValueFactory(data -> cell(data.getValue(), Reader.BORROW_COUNT));

        accountCol.setComparator(Comparator.comparingInt(Integer::parseInt));

        loadData();

        searchField.textProperty().addListener((obs, old, val) -> loadData());
        addReaderBtn.setOnAction(e -> addReader());
        updateReaderBtn.setOnAction(e -> updateReader());
        deleteReaderBtn.setOnAction(e -> deleteReader());
    }

    private void loadData() {
        String keyword = searchField.getText().trim();
        List<Map<String, Object>> list = keyword.isEmpty()
                ? DatabaseUtil.getAllReaders()
                : DatabaseUtil.searchReaders(keyword);
        readerTable.getItems().setAll(list);
    }

    private void addReader() {
        PageController.open(PageController.FormMode.ADD_READER, null, this::loadData);
    }

    private void updateReader() {
        Map<String, Object> selected = readerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MainController.getInstance().showMessage("请先选择要修改的读者");
            return;
        }

        Map<String, String> data = new HashMap<>();
        data.put(Reader.READER_ACCOUNT, (String) selected.get(Reader.READER_ACCOUNT));
        data.put(Reader.READER_NAME, (String) selected.get(Reader.READER_NAME));
        data.put(Reader.GENDER, (String) selected.get(Reader.GENDER));
        data.put(Reader.REGION_NAME, (String) selected.get(Reader.REGION_NAME));

        PageController.open(PageController.FormMode.UPDATE_READER, data, this::loadData);
    }
    private void deleteReader() {
        Map<String, Object> selected = readerTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MainController.getInstance().showMessage("请先选择要删除的读者");
            return;
        }
        final String account = (String) selected.get(Reader.READER_ACCOUNT);
        MainController.getInstance().showConfirm("确定要删除读者 " + account + " 吗？", () -> {
            if (DatabaseUtil.deleteReaderByAdmin(account)) {
                loadData();
            } else {
                MainController.getInstance().showMessage("删除失败");
            }
        });
    }

    private static SimpleStringProperty cell(Map<String, Object> row, String key) {
        Object val = row.get(key);
        return new SimpleStringProperty(val != null ? val.toString() : "");
    }
}
