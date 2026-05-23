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

public class BookManageController {


    @FXML private TextField searchField;
    @FXML private Button addBookBtn;
    @FXML private Button updateBookBtn;
    @FXML private Button deleteBookBtn;
    @FXML private TableView<Map<String, Object>> bookTable;
    @FXML private TableColumn<Map<String, Object>, String> bookIdCol;
    @FXML private TableColumn<Map<String, Object>, String> titleCol;
    @FXML private TableColumn<Map<String, Object>, String> authorCol;
    @FXML private TableColumn<Map<String, Object>, String> categoryCol;
    @FXML private TableColumn<Map<String, Object>, String> publisherCol;
    @FXML private TableColumn<Map<String, Object>, String> copyCountCol;
    @FXML private TableColumn<Map<String, Object>, String> availableCountCol;

    @SuppressWarnings("deprecation")
    public void initialize() {
        bookTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        bookIdCol.setCellValueFactory(data -> cell(data.getValue(), Book.BOOK_ID));
        titleCol.setCellValueFactory(data -> cell(data.getValue(), Book.TITLE));
        authorCol.setCellValueFactory(data -> cell(data.getValue(), Book.AUTHOR));
        categoryCol.setCellValueFactory(data -> cell(data.getValue(), Book.CATEGORY));
        publisherCol.setCellValueFactory(data -> cell(data.getValue(), Book.PUBLISHER_NAME));
        copyCountCol.setCellValueFactory(data -> cell(data.getValue(), Book.COPY_COUNT));
        availableCountCol.setCellValueFactory(data -> cell(data.getValue(), Book.AVAILABLE_COUNT));

        bookIdCol.setComparator(Comparator.comparingInt(Integer::parseInt));

        loadData();

        searchField.textProperty().addListener((obs, old, val) -> loadData());
        addBookBtn.setOnAction(e -> addBook());
        updateBookBtn.setOnAction(e -> updateBook());
        deleteBookBtn.setOnAction(e -> deleteBook());
    }

    private void loadData() {
        String keyword = searchField.getText().trim();
        List<Map<String, Object>> list = keyword.isEmpty()
                ? DatabaseUtil.getAllBooks()
                : DatabaseUtil.searchBooks(keyword);
        bookTable.getItems().setAll(list);
    }

    private void addBook() {
        PageController.open(PageController.FormMode.ADD_BOOK, null, this::loadData);
    }

    private void updateBook() {
        Map<String, Object> selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MainController.getInstance().showMessage("请先选择要修改的图书");
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put(Book.BOOK_ID, (String) selected.get(Book.BOOK_ID));
        data.put(Book.TITLE, (String) selected.get(Book.TITLE));
        data.put(Book.CATEGORY, (String) selected.get(Book.CATEGORY));
        data.put(Book.AUTHOR, (String) selected.get(Book.AUTHOR));
        data.put(Book.PUBLISHER_NAME, (String) selected.get(Book.PUBLISHER_NAME));

        PageController.open(PageController.FormMode.UPDATE_BOOK, data, this::loadData);
    }

    private void deleteBook() {
        Map<String, Object> selected = bookTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            MainController.getInstance().showMessage("请先选择要删除的图书");
            return;
        }
        final String id = (String) selected.get(Book.BOOK_ID);
        MainController.getInstance().showConfirm("确定要删除图书 " + id + " 吗？", () -> {
            DatabaseUtil.deleteBook(id);
            loadData();
        });
    }

    private static SimpleStringProperty cell(Map<String, Object> row, String key) {
        Object val = row.get(key);
        return new SimpleStringProperty(val != null ? val.toString() : "");
    }
}
