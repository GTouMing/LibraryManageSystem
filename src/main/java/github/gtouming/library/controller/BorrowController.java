package github.gtouming.library.controller;

import javafx.fxml.FXML;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import github.gtouming.library.util.DatabaseUtil;
import static github.gtouming.library.util.Db.*;

public class BorrowController {

    @FXML private Label headerSubtitle;
    @FXML private TextField searchField;
    @FXML private Button borrowBtn;
    @FXML private Button deleteBtn;
    @FXML private Button returnBtn;
    @FXML private TableView<Map<String, Object>> borrowTable;
    @FXML private TableColumn<Map<String, Object>, String> bookIdCol;
    @FXML private TableColumn<Map<String, Object>, String> titleCol;
    @FXML private TableColumn<Map<String, Object>, String> authorCol;
    @FXML private TableColumn<Map<String, Object>, String> publisherCol;
    @FXML private TableColumn<Map<String, Object>, String> copyCountCol;
    @FXML private TableColumn<Map<String, Object>, String> availableCountCol;
    @FXML private TabPane borrowTabPane;
    @FXML private Tab borrowTab;
    @FXML private TableView<Map<String, Object>> historyTable;
    @FXML private TableColumn<Map<String, Object>, String> historyReaderCol;
    @FXML private TableColumn<Map<String, Object>, String> historyBookIdCol;
    @FXML private TableColumn<Map<String, Object>, String> historyTitleCol;
    @FXML private TableColumn<Map<String, Object>, String> historyBorrowDateCol;
    @FXML private TableColumn<Map<String, Object>, String> historyReturnDateCol;
    @FXML private TableColumn<Map<String, Object>, String> historyStatusCol;

    private static final String STATUS_RETURNED = "已归还";
    private static final String STATUS_BORROWED = "未归还";

    private boolean isAdmin;
    private String readerAccount;
    private MainController mainCtrl;

    public void setAdmin(boolean isAdmin, String readerAccount) {
        this.isAdmin = isAdmin;
        this.readerAccount = readerAccount;
        this.mainCtrl = MainController.getInstance();
        applyRole();
    }

    private void applyRole() {
        if (isAdmin) {
            headerSubtitle.setText("管理所有读者的借阅和归还");
            returnBtn.setText("强制归还");
        } else {
            headerSubtitle.setText("浏览图书并借阅");
            returnBtn.setText("归还选中");
        }
        deleteBtn.setVisible(isAdmin);
        deleteBtn.setManaged(isAdmin);
        borrowBtn.setVisible(!isAdmin);
        borrowBtn.setManaged(!isAdmin);
        historyReaderCol.setVisible(isAdmin);

        if (isAdmin) {
            borrowTabPane.getTabs().remove(borrowTab);
        } else if (!borrowTabPane.getTabs().contains(borrowTab)) {
            borrowTabPane.getTabs().addFirst(borrowTab);
        }

        loadBooks();
        loadHistory();
    }

    public void initialize() {
        borrowTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        bookIdCol.setCellValueFactory(data -> cell(data.getValue(), Book.BOOK_ID));
        titleCol.setCellValueFactory(data -> cell(data.getValue(), Book.TITLE));
        authorCol.setCellValueFactory(data -> cell(data.getValue(), Book.AUTHOR));
        publisherCol.setCellValueFactory(data -> cell(data.getValue(), Book.PUBLISHER_NAME));
        copyCountCol.setCellValueFactory(data -> cell(data.getValue(), Book.COPY_COUNT));
        availableCountCol.setCellValueFactory(data -> cell(data.getValue(), Book.AVAILABLE_COUNT));

        historyReaderCol.setCellValueFactory(data -> cell(data.getValue(), BorrowRecord.READER_NAME));
        historyBookIdCol.setCellValueFactory(data -> cell(data.getValue(), BorrowRecord.BOOK_ID));
        historyTitleCol.setCellValueFactory(data -> cell(data.getValue(), BorrowRecord.TITLE));
        historyBorrowDateCol.setCellValueFactory(data -> cell(data.getValue(), BorrowRecord.BORROW_DATE));
        historyReturnDateCol.setCellValueFactory(data -> cell(data.getValue(), BorrowRecord.RETURN_DATE));
        historyStatusCol.setCellValueFactory(data -> cell(data.getValue(), "borrow_status"));

        bookIdCol.setComparator(Comparator.comparingInt(Integer::parseInt));

        searchField.textProperty().addListener((obs, old, val) -> loadBooks());

        borrowBtn.setOnAction(e -> handleBorrow());
        deleteBtn.setOnAction(e -> handleDeleteRecord());
        returnBtn.setOnAction(e -> handleReturn());
    }

    // ---- 借阅 ----

    private void handleBorrow() {
        Map<String, Object> selected = borrowTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainCtrl.showMessage("请先选择要借阅的图书");
            return;
        }
        final String bid = (String) selected.get(Book.BOOK_ID);
        final String t = (String) selected.get(Book.TITLE);

        mainCtrl.showConfirm("确认借阅《" + t + "》？", () -> {
            List<Map<String, Object>> copies = DatabaseUtil.getCopiesByBookId(bid);
            int copyId = -1;
            for (Map<String, Object> c : copies) {
                if ("available".equals(c.get(BookCopy.STATUS))) {
                    copyId = (int) c.get(BookCopy.COPY_ID);
                    break;
                }
            }
            if (copyId < 0) {
                mainCtrl.showMessage("无可借复本，请联系管理员添加");
                return;
            }
            if (DatabaseUtil.borrowBook(readerAccount, bid, copyId)) {
                mainCtrl.showMessage("借阅成功");
                loadBooks();
                loadHistory();
            } else {
                mainCtrl.showMessage("借阅失败，请重试");
            }
        });
    }

    // ---- 删除借阅记录（管理员：仅删记录，不复原复本）----

    private void handleDeleteRecord() {
        Map<String, Object> selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainCtrl.showMessage("请先在借阅记录中选择一条记录");
            return;
        }
        final int recordId = ((Number) selected.get(BorrowRecord.RECORD_ID)).intValue();
        mainCtrl.showConfirm("确定删除该借阅记录？", () -> {
            if (DatabaseUtil.deleteBorrowRecord(recordId)) {
                mainCtrl.showMessage("已删除");
                loadHistory();
                loadBooks();
            } else {
                mainCtrl.showMessage("删除失败");
            }
        });
    }

    // ---- 归还 ----

    private void handleReturn() {
        Map<String, Object> selected = historyTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            mainCtrl.showMessage("请先在借阅记录中选择一条记录");
            return;
        }
        if (selected.get(BorrowRecord.RETURN_DATE) != null) {
            mainCtrl.showMessage("该记录已归还");
            return;
        }
        final int recordId = ((Number) selected.get(BorrowRecord.RECORD_ID)).intValue();
        final String t = (String) selected.get(BorrowRecord.TITLE);
        mainCtrl.showConfirm("确认归还《" + t + "》？", () -> {
            if (DatabaseUtil.returnBook(recordId)) {
                mainCtrl.showMessage("归还成功");
                loadHistory();
                loadBooks();
            } else {
                mainCtrl.showMessage("归还失败");
            }
        });
    }

    // ---- 数据加载 ----

    private void loadBooks() {
        String keyword = searchField.getText().trim();
        List<Map<String, Object>> raw = keyword.isEmpty()
                ? DatabaseUtil.getAllBooks()
                : DatabaseUtil.searchBooks(keyword);
        List<Map<String, Object>> filtered = new ArrayList<>();
        for (Map<String, Object> book : raw) {
            Object avail = book.get(Book.AVAILABLE_COUNT);
            int count = 0;
            if (avail instanceof Number) {
                count = ((Number) avail).intValue();
            }
            if (count > 0) {
                filtered.add(book);
            }
        }
        borrowTable.getItems().setAll(filtered);
    }

    private void loadHistory() {
        List<Map<String, Object>> raw;
        if (isAdmin) {
            raw = DatabaseUtil.getAllBorrowRecords();
        } else {
            // 读者看到自己全部借阅记录（已归还 + 未归还）
            raw = new ArrayList<>();
            raw.addAll(DatabaseUtil.getCurrentBorrows(readerAccount));
            raw.addAll(DatabaseUtil.getBorrowHistory(readerAccount));
        }
        for (Map<String, Object> row : raw) {
            row.put("borrow_status",
                    row.get(BorrowRecord.RETURN_DATE) == null ? STATUS_BORROWED : STATUS_RETURNED);
        }
        historyTable.getItems().setAll(raw);
    }

    private static SimpleStringProperty cell(Map<String, Object> row, String key) {
        Object val = row.get(key);
        return new SimpleStringProperty(val != null ? val.toString() : "");
    }
}
