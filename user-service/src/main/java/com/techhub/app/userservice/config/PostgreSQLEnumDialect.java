package com.techhub.app.userservice.config;

import org.hibernate.dialect.PostgreSQL10Dialect;

import java.sql.Types;

public class PostgreSQLEnumDialect extends PostgreSQL10Dialect {

    public PostgreSQLEnumDialect() {
        super();
        // Register cho PostgreSQL ENUM types
        registerColumnType(Types.OTHER, "permission_method");
        registerColumnType(Types.OTHER, "user_status");
        // Thêm các enum khác nếu cần: registerColumnType(Types.OTHER, "your_enum_name");
    }
}