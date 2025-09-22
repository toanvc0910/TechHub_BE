package com.techhub.app.userservice.config;

import org.hibernate.dialect.PostgreSQL10Dialect;

import java.sql.Types;

public class PostgreSQLEnumDialect extends PostgreSQL10Dialect {

    public PostgreSQLEnumDialect() {
        super();
        this.registerHibernateType(Types.OTHER, "pg-enum");
    }
}
