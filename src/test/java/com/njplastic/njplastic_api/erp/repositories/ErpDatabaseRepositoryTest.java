package com.njplastic.njplastic_api.erp.repositories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Types;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import com.njplastic.njplastic_api.erp.repositories.rowmappers.ErpOrderRowMapper;
import com.njplastic.njplastic_api.erp.repositories.rows.ErpOrderRow;

@ExtendWith(MockitoExtension.class)
class ErpDatabaseRepositoryTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private ErpOrderRowMapper erpOrderRowMapper;

  private static final String FIND_OPEN_ORDERS = "SELECT * FROM orders WHERE status='OPEN'";
  private static final String INSERT_CYCLE = "INSERT INTO cycles VALUES (?, ?, ?, ?, ?)";
  private static final String INSERT_PAUSE = "INSERT INTO pauses VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
  private static final String PING = "SELECT 1";

  private ErpDatabaseRepository repository() {
    return new ErpDatabaseRepository(jdbcTemplate, erpOrderRowMapper,
        FIND_OPEN_ORDERS, INSERT_CYCLE, INSERT_PAUSE, PING);
  }

  @Test
  void validateQueries_failsWhenFindOpenOrdersIsBlank() {
    ErpDatabaseRepository repo = new ErpDatabaseRepository(jdbcTemplate, erpOrderRowMapper,
        "", INSERT_CYCLE, INSERT_PAUSE, PING);
    assertThatThrownBy(repo::validateQueries)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("find-open-orders");
  }

  @Test
  void validateQueries_failsWhenInsertCycleIsBlank() {
    ErpDatabaseRepository repo = new ErpDatabaseRepository(jdbcTemplate, erpOrderRowMapper,
        FIND_OPEN_ORDERS, "  ", INSERT_PAUSE, PING);
    assertThatThrownBy(repo::validateQueries)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("insert-cycle");
  }

  @Test
  void validateQueries_failsWhenInsertPauseIsBlank() {
    ErpDatabaseRepository repo = new ErpDatabaseRepository(jdbcTemplate, erpOrderRowMapper,
        FIND_OPEN_ORDERS, INSERT_CYCLE, "", PING);
    assertThatThrownBy(repo::validateQueries)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("insert-pause");
  }

  @Test
  void validateQueries_passesWhenAllConfigured() {
    repository().validateQueries();
    verifyNoInteractions(jdbcTemplate);
  }

  @Test
  void findOpenOrders_executesConfiguredSelectWithRowMapper() {
    ErpOrderRow row = new ErpOrderRow("OS-1", "MAQ-01", "SKU", 10, "OPEN", "{}");
    when(jdbcTemplate.query(FIND_OPEN_ORDERS, erpOrderRowMapper)).thenReturn(List.of(row));

    List<ErpOrderRow> result = repository().findOpenOrders();

    assertThat(result).containsExactly(row);
  }

  @Test
  void insertCycle_executesConfiguredStatementWithPositionalParams() {
    Object[] params = new Object[] { "id", "MAQ-01", null, 1L, 2000 };
    when(jdbcTemplate.update(INSERT_CYCLE, params)).thenReturn(1);

    int affected = repository().insertCycle(params);

    assertThat(affected).isEqualTo(1);
  }

  @Test
  void insertPause_executesConfiguredStatementWithTypeHints() {
    Object[] params = new Object[] { "id", "MAQ-01", "PAUSED", null, null, null, null, 3 };
    int[] types = new int[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
        Types.VARCHAR, Types.TIMESTAMP, Types.TIMESTAMP, Types.INTEGER };
    when(jdbcTemplate.update(INSERT_PAUSE, params, types)).thenReturn(1);

    int affected = repository().insertPause(params, types);

    assertThat(affected).isEqualTo(1);
  }

  @Test
  void ping_runsConfiguredPingQueryAsInteger() {
    when(jdbcTemplate.queryForObject(PING, Integer.class)).thenReturn(1);

    repository().ping();

    verify(jdbcTemplate).queryForObject(eq(PING), eq(Integer.class));
  }
}
