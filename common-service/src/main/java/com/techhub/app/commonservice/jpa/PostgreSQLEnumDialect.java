package com.techhub.app.commonservice.jpa;

import org.hibernate.dialect.PostgreSQL10Dialect;

import java.sql.Types;
import java.util.Arrays;
import java.util.List;

public class PostgreSQLEnumDialect extends PostgreSQL10Dialect {

    private static final List<String> DEFAULT_ENUM_TYPES = Arrays.asList(
            "permission_method",
            "user_status",
            "user_role",
            "blog_status",
            "comment_target"
    );

    public PostgreSQLEnumDialect() {
        super();
        DEFAULT_ENUM_TYPES.forEach(enumName -> registerColumnType(Types.OTHER, enumName));
    }
}
