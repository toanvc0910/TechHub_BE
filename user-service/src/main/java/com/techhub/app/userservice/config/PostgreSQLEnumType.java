package com.techhub.app.userservice.config;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.util.Properties;

public class PostgreSQLEnumType implements UserType, ParameterizedType {

    private Class<? extends Enum<?>> enumClass;

    public PostgreSQLEnumType() {
        // No-arg constructor for Hibernate instantiation
    }

    @Override
    public void setParameterValues(Properties parameters) {
        // Set enumClass from @Parameter in @TypeDef or @Type
        if (parameters != null && parameters.containsKey("enumClass")) {
            String enumClassName = parameters.getProperty("enumClass");
            try {
                this.enumClass = (Class<? extends Enum<?>>) Class.forName(enumClassName);
            } catch (ClassNotFoundException e) {
                throw new HibernateException("Cannot resolve enum class: " + enumClassName, e);
            }
        } else {
            throw new HibernateException("Missing required parameter: enumClass");
        }
    }

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.OTHER};
    }

    @Override
    public Class<?> returnedClass() {
        return enumClass;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y;
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return x != null ? x.hashCode() : 0;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        if (enumClass == null) {
            throw new HibernateException("enumClass not initialized. Ensure @Parameter(name=\"enumClass\", value=\"...\") is set.");
        }
        Object obj = rs.getObject(names[0]);
        if (rs.wasNull() || obj == null) {
            return null;  // Handle NULL an toàn
        }
        String columnValue = obj.toString();
        if (columnValue == null || columnValue.isEmpty()) {
            return null;
        }
        try {
            // FIXED: Use proper generic casting with wildcard types
            @SuppressWarnings("unchecked")
            Class<? extends Enum> rawEnumClass = (Class<? extends Enum>) enumClass;
            return Enum.valueOf(rawEnumClass, columnValue);
        } catch (IllegalArgumentException e) {
            throw new HibernateException("Invalid enum value from DB: '" + columnValue + "' for enum: " + enumClass.getSimpleName(), e);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.OTHER);
        } else {
            st.setObject(index, ((Enum<?>) value).name(), Types.OTHER);
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;  // Enum immutable
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}