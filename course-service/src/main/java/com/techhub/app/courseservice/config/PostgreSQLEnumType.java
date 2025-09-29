package com.techhub.app.courseservice.config;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.*;
import java.util.Properties;

public class PostgreSQLEnumType implements UserType, ParameterizedType {

    private Class<? extends Enum<?>> enumClass;

    public PostgreSQLEnumType() {}

    @Override
    public void setParameterValues(Properties parameters) {
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
    public int[] sqlTypes() { return new int[]{Types.OTHER}; }

    @Override
    public Class<?> returnedClass() { return enumClass; }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException { return x == y; }

    @Override
    public int hashCode(Object x) throws HibernateException { return x != null ? x.hashCode() : 0; }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        if (enumClass == null) throw new HibernateException("enumClass not initialized");
        Object obj = rs.getObject(names[0]);
        if (rs.wasNull() || obj == null) return null;
        String columnValue = obj.toString();
        @SuppressWarnings("unchecked") Class<? extends Enum> rawEnumClass = (Class<? extends Enum>) enumClass;
        return Enum.valueOf(rawEnumClass, columnValue);
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

    @Override public Object deepCopy(Object value) throws HibernateException { return value; }
    @Override public boolean isMutable() { return false; }
    @Override public Serializable disassemble(Object value) throws HibernateException { return (Serializable) value; }
    @Override public Object assemble(Serializable cached, Object owner) throws HibernateException { return cached; }
    @Override public Object replace(Object original, Object target, Object owner) throws HibernateException { return original; }
}

