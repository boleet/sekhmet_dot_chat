package nl.utwente.sekhmet;

import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.dialect.MySQL55Dialect;;import java.sql.Types;
// import org.hibernate.dialect.MySQL5InnoDBDialect; // Deprecated

// public class MySQL5InnoDBDialectUtf8mb4 extends MySQL5InnoDBDialect {
public class MySQL55DialectUtf8mb4 extends MariaDB103Dialect {
    @Override
    public String getTableTypeString() {
        return "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE utf8mb4_unicode_ci";
    }
     public MySQL55DialectUtf8mb4() {
        super();

        // Use Unicode Characters
        registerColumnType(Types.VARCHAR, 255, "nvarchar($l)");
        registerColumnType(Types.CHAR, "nchar(1)");
        registerColumnType(Types.CLOB, "nvarchar(max)");

        // Microsoft SQL Server 2000 supports bigint and bit
        registerColumnType(Types.BIGINT, "bigint");
        registerColumnType(Types.BIT, "bit");


    }
}
