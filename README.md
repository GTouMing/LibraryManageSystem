# 图书馆管理系统

JavaFX 21 + MySQL 8 开发的桌面图书管理应用，支持管理员和读者两种角色，覆盖借阅管理、图书管理、读者管理、个人信息维护等完整业务流程。

## 技术栈

| 层级 | 技术 |
|------|------|
| UI | JavaFX 21 (FXML + CSS)，自定义深色主题 "Nocturnal Library" |
| 数据库 | MySQL 8.0，JDBC 直连 |
| 构建 | Maven，Java 21 |

## 项目结构

```
src/main/
├── java/github/gtouming/library/
│   ├── App.java                    # JavaFX Application 入口 (init/start/stop)
│   ├── Launcher.java               # 启动类 (解决模块路径问题)
│   ├── controller/
│   │   ├── LoginController.java    # 登录页
│   │   ├── MainController.java     # 主界面框架 + 侧边栏 + 消息/确认栏
│   │   ├── BorrowController.java   # 借阅管理 (读者借阅/归还, 管理员管理)
│   │   ├── BookManageController.java # 图书管理 (管理员增删改)
│   │   ├── ReaderController.java   # 读者管理 (管理员查看/删除)
│   │   ├── PersonalController.java # 个人信息 (读者查看/修改/注销)
│   │   └── PageController.java     # 通用表单弹窗 (注册/添加读者/图书CRUD/改密码)
│   └── util/
│       ├── DatabaseUtil.java       # JDBC 工具: 连接管理、建表、全部 CRUD
│       └── Db.java                 # 表字段名常量 (Book/Reader/BorrowRecord/...)
└── resources/
    ├── fxml/
    │   ├── login.fxml              # 登录界面
    │   ├── main.fxml               # 主界面 (侧边栏 + 内容区)
    │   ├── borrow.fxml             # 借阅管理 (图书列表 + 借阅记录标签页)
    │   ├── book.fxml               # 图书管理
    │   ├── reader.fxml             # 读者管理
    │   ├── personal.fxml           # 个人信息
    │   └── page.fxml               # 复用表单弹窗
    ├── css/
    │   └── style.css               # 全局样式 (Nocturnal Library 深色主题)
    └── config/
        └── db.properties           # 数据库连接配置
```

## 数据库设计

6 张表，ER 关系：

```
publisher ──1:N── book ──1:N── book_copy ──1:N── borrow_record ──N:1── reader ──N:1── region
```

### 建表语句

```sql
-- 出版社
CREATE TABLE publisher (
    publisher_id   INT PRIMARY KEY AUTO_INCREMENT,
    publisher_name VARCHAR(100) NOT NULL UNIQUE,
    address        VARCHAR(200),
    phone          VARCHAR(20),
    contact_person VARCHAR(50)
);

-- 地区
CREATE TABLE region (
    region_code INT PRIMARY KEY,
    region_name VARCHAR(50) NOT NULL UNIQUE
);

-- 图书
CREATE TABLE book (
    book_id      VARCHAR(20) PRIMARY KEY,
    category     VARCHAR(30),
    title        VARCHAR(100) NOT NULL,
    author       VARCHAR(50),
    publisher_id INT,
    FOREIGN KEY (publisher_id) REFERENCES publisher(publisher_id)
);

-- 复本
CREATE TABLE book_copy (
    book_id VARCHAR(20),
    copy_id INT NOT NULL,
    status  ENUM('available','borrowed','maintenance','lost') DEFAULT 'available',
    PRIMARY KEY (book_id, copy_id),
    FOREIGN KEY (book_id) REFERENCES book(book_id)
);

-- 读者
CREATE TABLE reader (
    reader_account VARCHAR(20) PRIMARY KEY,
    password       VARCHAR(100) NOT NULL,
    reader_name    VARCHAR(50) NOT NULL,
    gender         ENUM('男','女'),
    region_code    INT,
    FOREIGN KEY (region_code) REFERENCES region(region_code)
);

-- 借阅记录
CREATE TABLE borrow_record (
    record_id     INT PRIMARY KEY AUTO_INCREMENT,
    reader_account VARCHAR(20),
    book_id       VARCHAR(20),
    copy_id       INT,
    borrow_date   DATE NOT NULL,
    due_date      DATE NOT NULL,
    return_date   DATE,
    renewal_count INT DEFAULT 0,
    FOREIGN KEY (reader_account) REFERENCES reader(reader_account),
    FOREIGN KEY (book_id, copy_id) REFERENCES book_copy(book_id, copy_id)
);
```

## 快速开始

### 1. 环境要求

- JDK 21+
- MySQL 8.0+
- Maven 3.8+ (或使用 IDE 内置 Maven)

### 2. 配置数据库

编辑 `src/main/resources/config/db.properties`：

```properties
db.url=jdbc:mysql://localhost:3306/library_db?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
db.username=root
db.password=your_password
```

首次启动应用会自动建库建表并插入默认地区数据，无需手动执行 SQL。

### 3. 运行

**Maven:**
```bash
mvn javafx:run
```

**IntelliJ IDEA:** 右键 `Launcher.java` → Run / Debug

### 4. 默认管理员

账号 `admin`，密码 `admin`（硬编码在 `LoginController` 中）。

## 功能清单

### 管理员

| 模块 | 功能 |
|------|------|
| 图书管理 | 添加/修改/删除图书，设复本数（可选），自动同步出版社 |
| 借阅管理 | 查看全部借阅记录，强制归还，删除记录 |
| 读者管理 | 查看所有读者（含借阅数），搜索，添加/删除读者 |

### 读者

| 模块 | 功能 |
|------|------|
| 借阅管理 | 浏览可借图书（仅显可用数>0），借阅/归还，查看全部借阅历史及状态 |
| 个人信息 | 查看/修改个人信息，修改密码，注销账号 |

### 复用表单弹窗

`PageController` 统一处理五种表单模式：读者注册、管理员添加读者、添加图书、修改图书、修改密码。

## 设计要点

- **复本机制**：图书的复本数由管理员设定，借阅时选中可用复本；可用数为 0 的图书不出现在可借列表中
- **借阅记录保留**：归还后记录不删除，仅标记 `return_date`，历史表显示"已归还"/"未归还"状态
- **提示系统**：`MainController` 统一管理纯消息（3秒自动消失）和确认对话框，两者互斥，互不干扰
- **字段常量**：`Db.java` 中定义所有表字段名常量，全项目统一引用，消除硬编码字符串
- **主键冲突**：`executeUpdate` 静默处理 `Duplicate entry` 错误，前端显示友好提示
