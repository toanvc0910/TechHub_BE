package com.techhub.app.commonservice.jpa;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;
import java.util.Properties;

public class PostgreSQLEnumType implements UserType, ParameterizedType {

    private static final String ENUM_CLASS_PARAM = "enumClass";

    private Class<? extends Enum<?>> enumClass;

    @Override
    public void setParameterValues(Properties parameters) {
        String enumClassName = parameters.getProperty(ENUM_CLASS_PARAM);
        if (enumClassName == null) {
            throw new HibernateException("Enum class not specified for PostgreSQLEnumType. Use @TypeDef parameter 'enumClass'.");
        }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> loadedClass = (Class<? extends Enum<?>>) Class.forName(enumClassName);
            this.enumClass = loadedClass;
        } catch (ClassNotFoundException ex) {
            throw new HibernateException("Enum class not found", ex);
        }
    }

    @Override
    public int[] sqlTypes() {
        return new int[]{Types.OTHER};
    }

    @Override
    public Class<? extends Enum<?>> returnedClass() {
        return enumClass;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return Objects.equals(x, y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return Objects.hashCode(x);
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        String value = rs.getString(names[0]);
        if (value == null || enumClass == null) {
            return null;
        }
        return Enum.valueOf(enumClass.asSubclass(Enum.class), value);
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
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return value == null ? null : ((Enum<?>) value).name();
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        if (cached == null || enumClass == null) {
            return null;
        }
        return Enum.valueOf(enumClass.asSubclass(Enum.class), cached.toString());
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
}
