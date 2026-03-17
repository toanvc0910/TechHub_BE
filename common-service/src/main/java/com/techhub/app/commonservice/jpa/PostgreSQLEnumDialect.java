package com.techhub.app.commonservice.jpa;

import org.hibernate.dialect.PostgreSQL10Dialect;

import java.sql.Types;

/**
 * Custom PostgreSQL dialect that maps Types.OTHER (used by PostgreSQL ENUMs)
 * to varchar. This handles ALL PostgreSQL enum types generically without
 * needing to register each one individually.
 */
public class PostgreSQLEnumDialect extends PostgreSQL10Dialect {

    public PostgreSQLEnumDialect() {
        super();
        // Map all Types.OTHER columns (PostgreSQL ENUMs) as varchar
        registerColumnType(Types.OTHER, "varchar");
    }
}
