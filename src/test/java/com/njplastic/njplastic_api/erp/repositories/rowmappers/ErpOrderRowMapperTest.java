package com.njplastic.njplastic_api.erp.repositories.rowmappers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.njplastic.njplastic_api.erp.repositories.rows.ErpOrderRow;

class ErpOrderRowMapperTest {

  private final ErpOrderRowMapper mapper = new ErpOrderRowMapper();

  @Test
  void mapRow_readsAllSixColumnsInOrder() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getString(1)).thenReturn("OS-2026-00123");
    when(rs.getString(2)).thenReturn("MAQ-01");
    when(rs.getString(3)).thenReturn("PVC-100ML-BR");
    when(rs.getObject(4)).thenReturn(5000);
    when(rs.getInt(4)).thenReturn(5000);
    when(rs.getString(5)).thenReturn("OPEN");
    when(rs.getString(6)).thenReturn("{\"cliente\":\"Acme\"}");

    ErpOrderRow row = mapper.mapRow(rs, 0);

    assertThat(row.erpOrderId()).isEqualTo("OS-2026-00123");
    assertThat(row.machineCode()).isEqualTo("MAQ-01");
    assertThat(row.productCode()).isEqualTo("PVC-100ML-BR");
    assertThat(row.targetQuantity()).isEqualTo(5000);
    assertThat(row.status()).isEqualTo("OPEN");
    assertThat(row.payloadJson()).isEqualTo("{\"cliente\":\"Acme\"}");
  }

  @Test
  void mapRow_returnsNullTargetQuantityWhenColumnIsNull() throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    when(rs.getString(1)).thenReturn("OS-1");
    when(rs.getString(2)).thenReturn(null);
    when(rs.getString(3)).thenReturn(null);
    when(rs.getObject(4)).thenReturn(null);
    when(rs.getString(5)).thenReturn(null);
    when(rs.getString(6)).thenReturn(null);

    ErpOrderRow row = mapper.mapRow(rs, 0);

    assertThat(row.targetQuantity()).isNull();
    assertThat(row.machineCode()).isNull();
    assertThat(row.productCode()).isNull();
    assertThat(row.status()).isNull();
    assertThat(row.payloadJson()).isNull();
  }
}
