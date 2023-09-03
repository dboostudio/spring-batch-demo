package kr.dboo.batchdemo.chunkOriented.entity;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;


@Component
public class PayRowMapper implements RowMapper<Pay> {

    @Override
    public Pay mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Pay(
                rs.getLong("id"),
                rs.getLong("amount"),
                rs.getString("tx_name"),
                rs.getTimestamp("tx_date_time").toLocalDateTime()
        );
    }
}
