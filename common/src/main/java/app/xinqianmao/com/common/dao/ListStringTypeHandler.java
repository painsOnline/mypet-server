/**
 * File: ListStringTypeHandler.java
 * Author: system
 * Date: 2026-05-04
 *
 * MyBatis TypeHandler for List<String> ↔ PostgreSQL VARCHAR[].
 */
package app.xinqianmao.com.common.dao;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts List<String> to PostgreSQL VARCHAR[] and vice versa.
 * Register via @TableField(typeHandler = ListStringTypeHandler.class) on entity fields.
 */
@MappedTypes(List.class)
public class ListStringTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType) throws SQLException {
        Array array = ps.getConnection().createArrayOf("varchar", parameter.toArray());
        ps.setArray(i, array);
        array.free();
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Array array = rs.getArray(columnName);
        return arrayToList(array);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array array = rs.getArray(columnIndex);
        return arrayToList(array);
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array array = cs.getArray(columnIndex);
        return arrayToList(array);
    }

    private List<String> arrayToList(Array array) throws SQLException {
        if (array == null) return new ArrayList<>();
        String[] values = (String[]) array.getArray();
        List<String> result = new ArrayList<>();
        for (String v : values) {
            if (v != null) result.add(v);
        }
        return result;
    }
}
