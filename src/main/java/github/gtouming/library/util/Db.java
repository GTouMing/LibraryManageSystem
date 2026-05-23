package github.gtouming.library.util;

/**
 * 数据库表字段名常量。用于替代项目中的硬编码字符串。
 */
public final class Db {

    public static final class Book {
        public static final String BOOK_ID = "book_id";
        public static final String CATEGORY = "category";
        public static final String TITLE = "title";
        public static final String AUTHOR = "author";
        public static final String PUBLISHER_ID = "publisher_id";
        public static final String PUBLISHER_NAME = "publisher_name";
        public static final String COPY_COUNT = "copy_count";
        public static final String AVAILABLE_COUNT = "available_count";
    }

    public static final class BookCopy {
        public static final String BOOK_ID = "book_id";
        public static final String COPY_ID = "copy_id";
        public static final String STATUS = "status";
    }

    public static final class Reader {
        public static final String READER_ACCOUNT = "reader_account";
        public static final String READER_NAME = "reader_name";
        public static final String PASSWORD = "password";
        public static final String GENDER = "gender";
        public static final String REGION_CODE = "region_code";
        public static final String REGION_NAME = "region_name";
        public static final String BORROW_COUNT = "borrow_count";
    }

    public static final class BorrowRecord {
        public static final String RECORD_ID = "record_id";
        public static final String READER_ACCOUNT = "reader_account";
        public static final String READER_NAME = "reader_name";
        public static final String BOOK_ID = "book_id";
        public static final String COPY_ID = "copy_id";
        public static final String TITLE = "title";
        public static final String BORROW_DATE = "borrow_date";
        public static final String RETURN_DATE = "return_date";
        public static final String STATUS = "status";
    }

    public static final class Publisher {
        public static final String PUBLISHER_ID = "publisher_id";
        public static final String PUBLISHER_NAME = "publisher_name";
        public static final String ADDRESS = "address";
        public static final String PHONE = "phone";
        public static final String CONTACT_PERSON = "contact_person";
    }

    public static final class Region {
        public static final String REGION_CODE = "region_code";
        public static final String REGION_NAME = "region_name";
    }
}
