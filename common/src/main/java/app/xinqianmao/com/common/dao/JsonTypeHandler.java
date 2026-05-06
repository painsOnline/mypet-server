/**
 * File: JsonTypeHandler.java
 * Author: system
 * Date: 2026-05-05
 *
 * MyBatis TypeHandler for String ↔ PostgreSQL JSON.
 */
package app.xinqianmao.com.common.dao;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.postgresql.util.PGobject;

import java.sql.*;

/**
 * Wraps a JSON string in a PGobject with type "json" so PostgreSQL
 * accepts it for json/jsonb columns.
 */
@MappedTypes(String.class)
public class JsonTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        PGobject pgo = new PGobject();
        pgo.setType("json");
        pgo.setValue(parameter);
        ps.setObject(i, pgo);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }
}
