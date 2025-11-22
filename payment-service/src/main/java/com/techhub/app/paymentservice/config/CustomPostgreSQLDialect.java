package com.techhub.app.paymentservice.config;

import org.hibernate.dialect.PostgreSQL10Dialect;
import java.sql.Types;

public class CustomPostgreSQLDialect extends PostgreSQL10Dialect {

    public CustomPostgreSQLDialect() {
        super();
        // Register PostgreSQL ENUM types as VARCHAR so Hibernate can handle them
        this.registerColumnType(Types.JAVA_OBJECT, "varchar");
    }
}

