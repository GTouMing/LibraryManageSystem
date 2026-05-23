package github.gtouming.library.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;

import static github.gtouming.library.util.Db.*;

/**
 * 数据库工具类 —— 连接管理、建表、各实体 CRUD
 */
public class DatabaseUtil {

    private static String url;
    private static String baseUrl;
    private static String dbName;
    private static String username;
    private static String password;

    static {
        try {
            loadConfig();
        } catch (Exception e) {
            System.err.println("加载数据库配置失败: " + e.getMessage());
        }
    }

    private static void loadConfig() throws Exception {
        Properties props = new Properties();
        try (InputStream in = DatabaseUtil.class.getResourceAsStream("/config/db.properties")) {
            if (in == null) {
                throw new RuntimeException("未找到 /config/db.properties");
            }
            props.load(in);
        }
        url = props.getProperty("db.url");
        username = props.getProperty("db.username");
        password = props.getProperty("db.password");

        int schemeEnd = url.indexOf("://");
        int dbStart = url.indexOf('/', schemeEnd + 3);
        int paramStart = url.indexOf('?', dbStart);
        baseUrl = url.substring(0, dbStart + 1)
                + (paramStart > 0 ? url.substring(paramStart) : "");
        dbName = url.substring(dbStart + 1, paramStart > 0 ? paramStart : url.length());

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL 驱动未找到", e);
        }
    }

    // ==================== 连接管理 ====================

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public static void close(AutoCloseable... resources) {
        for (AutoCloseable r : resources) {
            if (r != null) {
                try {
                    r.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ==================== 建表 ====================

    /** 创建数据库（不存在时） */
    private static void createDatabaseIfNotExists() {
        try (Connection conn = DriverManager.getConnection(baseUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + dbName
                    + "` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (SQLException e) {
            System.err.println("创建数据库失败: " + e.getMessage());
        }
    }

    /** 初始化数据库及所有表（不存在则创建），应用启动时调用一次 */
    public static void initTables() {
        createDatabaseIfNotExists();

        String[] sqls = {
            "CREATE TABLE IF NOT EXISTS publisher ("
                + "publisher_id INT PRIMARY KEY AUTO_INCREMENT,"
                + "publisher_name VARCHAR(100) NOT NULL UNIQUE,"
                + "address VARCHAR(200),"
                + "phone VARCHAR(20),"
                + "contact_person VARCHAR(50)"
                + ")",

            "CREATE TABLE IF NOT EXISTS region ("
                + "region_code INT PRIMARY KEY,"
                + "region_name VARCHAR(50) NOT NULL UNIQUE"
                + ")",

            "CREATE TABLE IF NOT EXISTS book ("
                + "book_id VARCHAR(20) PRIMARY KEY,"
                + "category VARCHAR(30),"
                + "title VARCHAR(100) NOT NULL,"
                + "author VARCHAR(50),"
                + "publisher_id INT,"
                + "FOREIGN KEY (publisher_id) REFERENCES publisher(publisher_id)"
                + ")",

            "CREATE TABLE IF NOT EXISTS book_copy ("
                + "book_id VARCHAR(20),"
                + "copy_id INT NOT NULL,"
                + "status ENUM('available','borrowed','maintenance','lost') DEFAULT 'available',"
                + "PRIMARY KEY (book_id, copy_id),"
                + "FOREIGN KEY (book_id) REFERENCES book(book_id)"
                + ")",

            "CREATE TABLE IF NOT EXISTS reader ("
                + "reader_account VARCHAR(20) PRIMARY KEY,"
                + "password VARCHAR(100) NOT NULL,"
                + "reader_name VARCHAR(50) NOT NULL,"
                + "gender ENUM('男','女'),"
                + "region_code INT,"
                + "FOREIGN KEY (region_code) REFERENCES region(region_code)"
                + ")",

            "CREATE TABLE IF NOT EXISTS borrow_record ("
                + "record_id INT PRIMARY KEY AUTO_INCREMENT,"
                + "reader_account VARCHAR(20),"
                + "book_id VARCHAR(20),"
                + "copy_id INT,"
                + "borrow_date DATE NOT NULL,"
                + "due_date DATE NOT NULL,"
                + "return_date DATE,"
                + "renewal_count INT DEFAULT 0,"
                + "FOREIGN KEY (reader_account) REFERENCES reader(reader_account),"
                + "FOREIGN KEY (book_id, copy_id) REFERENCES book_copy(book_id, copy_id)"
                + ")"
        };

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            for (String sql : sqls) {
                stmt.executeUpdate(sql);
            }
            // 插入默认地区数据
            stmt.executeUpdate(
                "INSERT IGNORE INTO region (region_code, region_name) VALUES "
                + "(110000,'北京市'),(310000,'上海市'),(440000,'广东省'),"
                + "(320000,'江苏省'),(330000,'浙江省'),(510000,'四川省'),"
                + "(420000,'湖北省'),(370000,'山东省'),(210000,'辽宁省'),"
                + "(350000,'福建省')");
        } catch (SQLException e) {
            System.err.println("建表失败: " + e.getMessage());
        } finally {
            close(stmt, conn);
        }
    }

    // ==================== Reader / 用户 ====================

    /** 登录验证，返回用户信息；失败返回 null */
    public static Map<String, Object> login(String account, String rawPassword) {
        String sql = "SELECT reader_account, reader_name, gender, region_code "
                   + "FROM reader WHERE reader_account = ? AND password = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, account);
            ps.setString(2, rawPassword);
            rs = ps.executeQuery();
            if (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put(Reader.READER_ACCOUNT, rs.getString(Reader.READER_ACCOUNT));
                user.put(Reader.READER_NAME, rs.getString(Reader.READER_NAME));
                user.put(Reader.GENDER, rs.getString(Reader.GENDER));
                user.put(Reader.REGION_CODE, rs.getObject(Reader.REGION_CODE));
                return user;
            }
        } catch (SQLException e) {
            System.err.println("登录查询失败: " + e.getMessage());
        } finally {
            close(rs, ps, conn);
        }
        return null;
    }

    /** 读者注册 */
    public static boolean register(String account, String rawPassword, String name,
                                   String gender, Integer regionCode) {
        String sql = "INSERT INTO reader (reader_account, password, reader_name, gender, region_code) "
                   + "VALUES (?, ?, ?, ?, ?)";
        return executeUpdate(sql, account, rawPassword, name, gender, regionCode) > 0;
    }

    /** 根据账号查询读者 */
    public static Map<String, Object> getReaderByAccount(String account) {
        String sql = "SELECT r.reader_account, r.password, r.reader_name, r.gender, r.region_code, "
                   + "rg.region_name "
                   + "FROM reader r LEFT JOIN region rg ON r.region_code = rg.region_code "
                   + "WHERE r.reader_account = ?";
        return queryOne(sql, account);
    }

    /** 查询所有读者 */
    public static List<Map<String, Object>> getAllReaders() {
        String sql = "SELECT r.reader_account, r.reader_name, r.gender, rg.region_name, "
                   + "(SELECT COUNT(*) FROM borrow_record br "
                   + " WHERE br.reader_account = r.reader_account AND br.return_date IS NULL) "
                   + "AS borrow_count "
                   + "FROM reader r LEFT JOIN region rg ON r.region_code = rg.region_code";
        return queryList(sql);
    }

    /** 按关键词搜索读者 */
    public static List<Map<String, Object>> searchReaders(String keyword) {
        String sql = "SELECT r.reader_account, r.reader_name, r.gender, rg.region_name, "
                   + "(SELECT COUNT(*) FROM borrow_record br "
                   + " WHERE br.reader_account = r.reader_account AND br.return_date IS NULL) "
                   + "AS borrow_count "
                   + "FROM reader r LEFT JOIN region rg ON r.region_code = rg.region_code "
                   + "WHERE r.reader_account LIKE ? OR r.reader_name LIKE ?";
        String kw = "%" + keyword + "%";
        return queryList(sql, kw, kw);
    }

    /** 修改读者信息 */
    public static boolean updateReader(String account, String name, String gender, Integer regionCode) {
        String sql = "UPDATE reader SET reader_name = ?, gender = ?, region_code = ? "
                   + "WHERE reader_account = ?";
        return executeUpdate(sql, name, gender, regionCode, account) > 0;
    }

    /** 修改密码 */
    public static boolean changePassword(String account, String oldPassword, String newPassword) {
        String sql = "UPDATE reader SET password = ? WHERE reader_account = ? AND password = ?";
        return executeUpdate(sql, newPassword, account, oldPassword) > 0;
    }

    /** 管理员注销读者 */
    public static boolean deleteReaderByAdmin(String account) {
        String sql = "DELETE FROM reader WHERE reader_account = ?";
        return executeUpdate(sql, account) > 0;
    }

    // ==================== Book / 图书 ====================

    /** 根据出版社名查找 ID，不存在则自动创建 */
    private static int resolvePublisherId(String publisherName) {
        if (publisherName == null || publisherName.isBlank()) return 0;
        String name = publisherName.trim();
        String sql = "SELECT publisher_id FROM publisher WHERE publisher_name = ?";
        Map<String, Object> existing = queryOne(sql, name);
        if (existing != null) {
            return ((Number) existing.get(Publisher.PUBLISHER_ID)).intValue();
        }
        return addPublisher(name, null, null, null);
    }

    /** 添加图书，copyCount 为 null 时不创建复本 */
    public static boolean addBook(String bookId, String category, String title,
                                  String author, String publisherName, Integer copyCount) {
        int pubId = resolvePublisherId(publisherName);
        String sql = "INSERT INTO book (book_id, category, title, author, publisher_id) "
                   + "VALUES (?, ?, ?, ?, ?)";
        int rows = executeUpdate(sql, bookId, category, title, author, pubId > 0 ? pubId : null);
        if (rows > 0) {
            int n = copyCount != null ? copyCount : 0;
            for (int i = 0; i < n; i++) {
                addCopy(bookId);
            }
            return true;
        }
        return false;
    }

    /** 修改图书信息，copyCount 为 null 时不修改复本数量 */
    public static boolean updateBook(String bookId, String category, String title,
                                     String author, String publisherName, Integer copyCount) {
        int pubId = resolvePublisherId(publisherName);
        String sql = "UPDATE book SET category = ?, title = ?, author = ?, publisher_id = ? "
                   + "WHERE book_id = ?";
        int rows = executeUpdate(sql, category, title, author, pubId > 0 ? pubId : null, bookId);
        if (rows > 0) {
            if (copyCount != null) {
                List<Map<String, Object>> copies = getCopiesByBookId(bookId);
                int current = copies.size();
                int n = copyCount;
                if (n > current) {
                    for (int i = current; i < n; i++) {
                        addCopy(bookId);
                    }
                } else if (n < current) {
                    for (int i = current - 1; i >= n; i--) {
                        Map<String, Object> c = copies.get(i);
                        deleteCopy(bookId, ((Number) c.get(BookCopy.COPY_ID)).intValue());
                    }
                }
            }
            return true;
        }
        return false;
    }

    /** 删除图书（同时级联删除关联复本和借阅记录） */
    public static boolean deleteBook(String bookId) {
        Connection conn = null;
        PreparedStatement ps1 = null, ps2 = null, ps3 = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            ps1 = conn.prepareStatement("DELETE FROM borrow_record WHERE book_id = ?");
            ps1.setString(1, bookId);
            ps1.executeUpdate();

            ps2 = conn.prepareStatement("DELETE FROM book_copy WHERE book_id = ?");
            ps2.setString(1, bookId);
            ps2.executeUpdate();

            ps3 = conn.prepareStatement("DELETE FROM book WHERE book_id = ?");
            ps3.setString(1, bookId);
            int rows = ps3.executeUpdate();

            conn.commit();
            return rows > 0;
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ignored) {
            }
            System.err.println("删除图书失败: " + e.getMessage());
            return false;
        } finally {
            close(ps1, ps2, ps3, conn);
        }
    }

    /** 按 ID 查询单本图书 */
    public static Map<String, Object> getBookById(String bookId) {
        String sql = "SELECT b.book_id, b.category, b.title, b.author, b.publisher_id, "
                   + "p.publisher_name, "
                   + "(SELECT COUNT(*) FROM book_copy bc WHERE bc.book_id = b.book_id) AS copy_count, "
                   + "(SELECT COUNT(*) FROM book_copy bc WHERE bc.book_id = b.book_id "
                   + " AND bc.status = 'available') AS available_count "
                   + "FROM book b LEFT JOIN publisher p ON b.publisher_id = p.publisher_id "
                   + "WHERE b.book_id = ?";
        return queryOne(sql, bookId);
    }

    /** 查询全部图书 */
    public static List<Map<String, Object>> getAllBooks() {
        String sql = "SELECT b.book_id, b.category, b.title, b.author, p.publisher_name, "
                   + "(SELECT COUNT(*) FROM book_copy bc WHERE bc.book_id = b.book_id) AS copy_count, "
                   + "(SELECT COUNT(*) FROM book_copy bc WHERE bc.book_id = b.book_id "
                   + " AND bc.status = 'available') AS available_count "
                   + "FROM book b LEFT JOIN publisher p ON b.publisher_id = p.publisher_id";
        return queryList(sql);
    }

    /** 按关键词搜索图书（匹配图书号、书名、作者） */
    public static List<Map<String, Object>> searchBooks(String keyword) {
        String sql = "SELECT b.book_id, b.category, b.title, b.author, p.publisher_name, "
                   + "(SELECT COUNT(*) FROM book_copy bc WHERE bc.book_id = b.book_id) AS copy_count, "
                   + "(SELECT COUNT(*) FROM book_copy bc WHERE bc.book_id = b.book_id "
                   + " AND bc.status = 'available') AS available_count "
                   + "FROM book b LEFT JOIN publisher p ON b.publisher_id = p.publisher_id "
                   + "WHERE b.book_id LIKE ? OR b.title LIKE ? OR b.author LIKE ?";
        String kw = "%" + keyword + "%";
        return queryList(sql, kw, kw, kw);
    }

    // ==================== BookCopy / 复本 ====================

    /** 为指定图书添加一个复本，返回新复本号 */
    public static int addCopy(String bookId) {
        String sql = "INSERT INTO book_copy (book_id, copy_id) "
                   + "SELECT ?, COALESCE(MAX(copy_id), 0) + 1 FROM book_copy WHERE book_id = ?";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            ps.setString(1, bookId);
            ps.setString(2, bookId);
            int rows = ps.executeUpdate();
            if (rows > 0) {
                ps = conn.prepareStatement(
                    "SELECT MAX(copy_id) FROM book_copy WHERE book_id = ?");
                ps.setString(1, bookId);
                rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("添加复本失败: " + e.getMessage());
        } finally {
            close(rs, ps, conn);
        }
        return -1;
    }

    /** 删除指定复本（先解除所有借阅记录的 FK 引用，正在借阅的强制归还） */
    public static boolean deleteCopy(String bookId, int copyId) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 强制归还正在借阅的记录
            ps = conn.prepareStatement(
                "UPDATE borrow_record SET return_date = ?, book_id = NULL, copy_id = NULL "
                + "WHERE book_id = ? AND copy_id = ? AND return_date IS NULL");
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.setString(2, bookId);
            ps.setInt(3, copyId);
            ps.executeUpdate();
            ps.close();

            // 解除已归还记录的 FK 引用
            ps = conn.prepareStatement(
                "UPDATE borrow_record SET book_id = NULL, copy_id = NULL "
                + "WHERE book_id = ? AND copy_id = ?");
            ps.setString(1, bookId);
            ps.setInt(2, copyId);
            ps.executeUpdate();
            ps.close();

            // 删除复本
            ps = conn.prepareStatement(
                "DELETE FROM book_copy WHERE book_id = ? AND copy_id = ?");
            ps.setString(1, bookId);
            ps.setInt(2, copyId);
            int rows = ps.executeUpdate();

            conn.commit();
            return rows > 0;
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ignored) {}
            System.err.println("删除复本失败: " + e.getMessage());
            return false;
        } finally {
            close(ps, conn);
        }
    }

    /** 查询某图书的所有复本 */
    public static List<Map<String, Object>> getCopiesByBookId(String bookId) {
        String sql = "SELECT book_id, copy_id, status FROM book_copy WHERE book_id = ?";
        return queryList(sql, bookId);
    }

    /** 更新复本状态 */
    public static boolean updateCopyStatus(String bookId, int copyId, String status) {
        String sql = "UPDATE book_copy SET status = ? WHERE book_id = ? AND copy_id = ?";
        return executeUpdate(sql, status, bookId, copyId) > 0;
    }

    // ==================== Borrow / 借阅 ====================

    /** 借阅：复本状态置为 borrowed，新增借阅记录（借期 30 天） */
    public static boolean borrowBook(String readerAccount, String bookId, int copyId) {
        Connection conn = null;
        PreparedStatement psCheck = null, psBorrow = null, psUpdate = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            // 检查复本是否可借
            psCheck = conn.prepareStatement(
                "SELECT status FROM book_copy WHERE book_id = ? AND copy_id = ?");
            psCheck.setString(1, bookId);
            psCheck.setInt(2, copyId);
            rs = psCheck.executeQuery();
            if (!rs.next() || !"available".equals(rs.getString(BookCopy.STATUS))) {
                conn.rollback();
                return false;
            }

            // 插入借阅记录
            LocalDate today = LocalDate.now();
            psBorrow = conn.prepareStatement(
                "INSERT INTO borrow_record (reader_account, book_id, copy_id, borrow_date, due_date) "
                + "VALUES (?, ?, ?, ?, ?)");
            psBorrow.setString(1, readerAccount);
            psBorrow.setString(2, bookId);
            psBorrow.setInt(3, copyId);
            psBorrow.setDate(4, Date.valueOf(today));
            psBorrow.setDate(5, Date.valueOf(today.plusDays(30)));
            psBorrow.executeUpdate();

            // 更新复本状态
            psUpdate = conn.prepareStatement(
                "UPDATE book_copy SET status = 'borrowed' WHERE book_id = ? AND copy_id = ?");
            psUpdate.setString(1, bookId);
            psUpdate.setInt(2, copyId);
            psUpdate.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ignored) {
            }
            System.err.println("借阅失败: " + e.getMessage());
            return false;
        } finally {
            close(rs, psCheck, psBorrow, psUpdate, conn);
        }
    }

    /** 归还：复本状态恢复，借阅记录写归还日期 */
    public static boolean returnBook(int recordId) {
        Connection conn = null;
        PreparedStatement psRecord = null, psUpdate = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psRecord = conn.prepareStatement(
                "SELECT book_id, copy_id, return_date FROM borrow_record WHERE record_id = ?");
            psRecord.setInt(1, recordId);
            rs = psRecord.executeQuery();
            if (!rs.next() || rs.getDate(BorrowRecord.RETURN_DATE) != null) {
                conn.rollback();
                return false;
            }
            String bookId = rs.getString(BorrowRecord.BOOK_ID);
            int copyId = rs.getInt(BorrowRecord.COPY_ID);

            // 更新借阅记录
            PreparedStatement psReturn = conn.prepareStatement(
                "UPDATE borrow_record SET return_date = ? WHERE record_id = ?");
            psReturn.setDate(1, Date.valueOf(LocalDate.now()));
            psReturn.setInt(2, recordId);
            psReturn.executeUpdate();
            psReturn.close();

            // 恢复复本状态
            psUpdate = conn.prepareStatement(
                "UPDATE book_copy SET status = 'available' WHERE book_id = ? AND copy_id = ?");
            psUpdate.setString(1, bookId);
            psUpdate.setInt(2, copyId);
            psUpdate.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ignored) {
            }
            System.err.println("归还失败: " + e.getMessage());
            return false;
        } finally {
            close(rs, psRecord, psUpdate, conn);
        }
    }

    /** 查询读者当前借阅（未归还） */
    public static List<Map<String, Object>> getCurrentBorrows(String readerAccount) {
        String sql = "SELECT br.record_id, br.book_id, b.title, b.author, "
                   + "br.copy_id, br.borrow_date, br.due_date, bc.status "
                   + "FROM borrow_record br "
                   + "JOIN book b ON br.book_id = b.book_id "
                   + "JOIN book_copy bc ON br.book_id = bc.book_id AND br.copy_id = bc.copy_id "
                   + "WHERE br.reader_account = ? AND br.return_date IS NULL";
        return queryList(sql, readerAccount);
    }

    /** 查询读者借阅历史（已归还） */
    public static List<Map<String, Object>> getBorrowHistory(String readerAccount) {
        String sql = "SELECT br.record_id, br.book_id, b.title, "
                   + "br.borrow_date, br.return_date "
                   + "FROM borrow_record br "
                   + "JOIN book b ON br.book_id = b.book_id "
                   + "WHERE br.reader_account = ? AND br.return_date IS NOT NULL "
                   + "ORDER BY br.return_date DESC";
        return queryList(sql, readerAccount);
    }

    /** 删除借阅记录并恢复复本状态 */
    public static boolean deleteBorrowRecord(int recordId) {
        Connection conn = null;
        PreparedStatement psRecord = null, psUpdate = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);

            psRecord = conn.prepareStatement(
                "SELECT book_id, copy_id, return_date FROM borrow_record WHERE record_id = ?");
            psRecord.setInt(1, recordId);
            rs = psRecord.executeQuery();
            if (!rs.next()) { conn.rollback(); return false; }

            String bookId = rs.getString(BorrowRecord.BOOK_ID);
            int copyId = rs.getInt(BorrowRecord.COPY_ID);

            // 删除借阅记录
            psUpdate = conn.prepareStatement("DELETE FROM borrow_record WHERE record_id = ?");
            psUpdate.setInt(1, recordId);
            psUpdate.executeUpdate();
            psUpdate.close();

            // 恢复复本状态
            psUpdate = conn.prepareStatement(
                "UPDATE book_copy SET status = 'available' WHERE book_id = ? AND copy_id = ?");
            psUpdate.setString(1, bookId);
            psUpdate.setInt(2, copyId);
            psUpdate.executeUpdate();

            conn.commit();
            return true;
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ignored) {}
            System.err.println("删除借阅记录失败: " + e.getMessage());
            return false;
        } finally {
            close(rs, psRecord, psUpdate, conn);
        }
    }

    /** 管理员查询全部借阅记录 */
    public static List<Map<String, Object>> getAllBorrowRecords() {
        String sql = "SELECT br.record_id, br.reader_account, r.reader_name, "
                   + "br.book_id, b.title, br.copy_id, "
                   + "br.borrow_date, br.due_date, br.return_date, bc.status "
                   + "FROM borrow_record br "
                   + "JOIN reader r ON br.reader_account = r.reader_account "
                   + "JOIN book b ON br.book_id = b.book_id "
                   + "JOIN book_copy bc ON br.book_id = bc.book_id AND br.copy_id = bc.copy_id "
                   + "ORDER BY br.borrow_date DESC";
        return queryList(sql);
    }

    // ==================== Publisher / 出版社 ====================

    /** 添加出版社，返回生成的 ID */
    public static int addPublisher(String name, String address, String phone, String contactPerson) {
        String sql = "INSERT INTO publisher (publisher_name, address, phone, contact_person) "
                   + "VALUES (?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, address);
            ps.setString(3, phone);
            ps.setString(4, contactPerson);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("添加出版社失败: " + e.getMessage());
        } finally {
            close(rs, ps, conn);
        }
        return -1;
    }

    /** 查询全部出版社 */
    public static List<Map<String, Object>> getAllPublishers() {
        String sql = "SELECT publisher_id, publisher_name, address, phone, contact_person "
                   + "FROM publisher";
        return queryList(sql);
    }

    // ==================== Region / 地区 ====================

    /** 新增地区 */
    public static boolean addRegion(int code, String name) {
        String sql = "INSERT INTO region (region_code, region_name) VALUES (?, ?)";
        return executeUpdate(sql, code, name) > 0;
    }

    /** 查询全部地区 */
    public static List<Map<String, Object>> getAllRegions() {
        String sql = "SELECT region_code, region_name FROM region";
        return queryList(sql);
    }

    // ==================== 通用查询辅助 ====================

    /** 执行更新（INSERT / UPDATE / DELETE），返回影响行数 */
    private static int executeUpdate(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            setParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            if (!e.getMessage().contains("Duplicate entry")) {
                System.err.println("SQL 执行失败: " + e.getMessage() + " [" + sql + "]");
            }
            return -1;
        } finally {
            close(ps, conn);
        }
    }

    /** 查询单行 */
    private static Map<String, Object> queryOne(String sql, Object... params) {
        List<Map<String, Object>> list = queryList(sql, params);
        return list.isEmpty() ? null : list.getFirst();
    }

    /** 查询多行 */
    private static List<Map<String, Object>> queryList(String sql, Object... params) {
        List<Map<String, Object>> list = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            conn = getConnection();
            ps = conn.prepareStatement(sql);
            setParams(ps, params);
            rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                list.add(row);
            }
        } catch (SQLException e) {
            System.err.println("SQL 执行失败: " + e.getMessage() + " [" + sql + "]");
        } finally {
            close(rs, ps, conn);
        }
        return list;
    }

    /** 设置 PreparedStatement 参数 */
    private static void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
