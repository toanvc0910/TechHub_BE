package com.techhub.app.courseservice.util;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.EnumType;
import org.postgresql.util.PGobject;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public class PostgreSQLEnumType extends EnumType {

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            PGobject pgObject = new PGobject();
            String enumValue = ((Enum) value).name();
            // Use explicit DB enum type name. Adjust this if your DB enum name differs.
            pgObject.setType("skill_category");
            pgObject.setValue(enumValue);
            st.setObject(index, pgObject);
        }
    }
}
