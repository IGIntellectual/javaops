package ru.javaops.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Repository;
import ru.javaops.SqlResult;
import ru.javaops.to.UserMailImpl;

import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * gkislin
 * 30.10.2016
 */

@Repository
public class SqlRepository {
    private static final BeanPropertyRowMapper<UserMailImpl> ROW_MAPPER = BeanPropertyRowMapper.newInstance(UserMailImpl.class);

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public List<UserMailImpl> getUsersMail(String sql) {
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public SqlResult execute(String sql, Map<String, ?> params) {
        return jdbcTemplate.query(sql, params, rs -> {
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            List<String> headers = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                headers.add(JdbcUtils.lookupColumnName(rsmd, i));
            }
            List<Object[]> rows = new ArrayList<>();
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = JdbcUtils.getResultSetValue(rs, i);
                }
                rows.add(row);
            }
            return new SqlResult(headers, rows);
        });
    }
}