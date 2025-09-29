package com.techhub.app.courseservice.config;

import org.hibernate.dialect.PostgreSQL10Dialect;

import java.sql.Types;

public class PostgreSQLEnumDialect extends PostgreSQL10Dialect {
    public PostgreSQLEnumDialect() {
        super();
        registerColumnType(Types.OTHER, "course_status");
    }
}

