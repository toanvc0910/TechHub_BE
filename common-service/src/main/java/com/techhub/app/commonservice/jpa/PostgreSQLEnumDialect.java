package com.techhub.app.commonservice.jpa;

import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.type.StandardBasicTypes;

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
        // Khi Hibernate auto-discover type của result column trả về OTHER (1111),
        // map về String để tránh "No Dialect mapping for JDBC type: 1111".
        registerHibernateType(Types.OTHER, StandardBasicTypes.STRING.getName());
    }
}
