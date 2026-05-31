package com.njplastic.njplastic_api.erp.repositories.rowmappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.njplastic.njplastic_api.erp.repositories.rows.ErpOrderRow;

/**
 * Maps one row of the {@code app.datasource.erp.query.find-open-orders} result
 * set into {@link ErpOrderRow}. The configured SELECT must return six columns
 * in this order: erp_order_id, machine_code, product_code, target_quantity,
 * status, payload_json. Kept separate from {@code ErpDatabaseRepository} so the
 * repository file only contains query execution.
 */
@Component
@ConditionalOnProperty(prefix = "app.datasource.erp", name = "enabled", havingValue = "true")
public class ErpOrderRowMapper implements RowMapper<ErpOrderRow> {

  @Override
  public ErpOrderRow mapRow(ResultSet rs, int rowNum) throws SQLException {
    Integer targetQuantity = rs.getObject(4) == null ? null : Integer.valueOf(rs.getInt(4));
    return new ErpOrderRow(
        rs.getString(1),
        rs.getString(2),
        rs.getString(3),
        targetQuantity,
        rs.getString(5),
        rs.getString(6));
  }
}
