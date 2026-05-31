package com.njplastic.njplastic_api.erp.repositories;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.njplastic.njplastic_api.erp.repositories.rowmappers.ErpOrderRowMapper;
import com.njplastic.njplastic_api.erp.repositories.rows.ErpOrderRow;

import jakarta.annotation.PostConstruct;

/**
 * Sole component that talks to the customer ERP database (RNF13). Uses
 * {@code JdbcTemplate} against the {@code erpDataSource} bean exposed by
 * {@link com.njplastic.njplastic_api.config.ErpDataSourceConfig}, so no JPA
 * mapping leaks into the ERP. Bean is only created when
 * {@code app.datasource.erp.enabled=true}; the four SQL statements are loaded
 * from properties so the same binary serves SQL Server, Oracle and PostgreSQL
 * ERPs without recompilation. This class contains only query execution:
 * conversions, {@code try/catch} translation and orchestration live in
 * {@code ErpSyncService}.
 */
@Repository
@ConditionalOnProperty(prefix = "app.datasource.erp", name = "enabled", havingValue = "true")
public class ErpDatabaseRepository {

  private final JdbcTemplate erp;
  private final ErpOrderRowMapper erpOrderRowMapper;
  private final String findOpenOrdersQuery;
  private final String insertCycleQuery;
  private final String insertPauseQuery;
  private final String pingQuery;

  public ErpDatabaseRepository(
      @Qualifier("erpJdbcTemplate") JdbcTemplate erp,
      ErpOrderRowMapper erpOrderRowMapper,
      @Value("${app.datasource.erp.query.find-open-orders:}") String findOpenOrdersQuery,
      @Value("${app.datasource.erp.query.insert-cycle:}") String insertCycleQuery,
      @Value("${app.datasource.erp.query.insert-pause:}") String insertPauseQuery,
      @Value("${app.datasource.erp.query.ping:SELECT 1}") String pingQuery) {
    this.erp = erp;
    this.erpOrderRowMapper = erpOrderRowMapper;
    this.findOpenOrdersQuery = findOpenOrdersQuery;
    this.insertCycleQuery = insertCycleQuery;
    this.insertPauseQuery = insertPauseQuery;
    this.pingQuery = pingQuery;
  }

  /**
   * Fail fast at startup when the operator enabled ERP without configuring the
   * three required statements. Avoids the situation where the scheduler keeps
   * recording empty failures every cycle and is impossible to diagnose without
   * looking at {@code erp_sync_run}.
   */
  @PostConstruct
  void validateQueries() {
    if (!StringUtils.hasText(findOpenOrdersQuery)
        || !StringUtils.hasText(insertCycleQuery)
        || !StringUtils.hasText(insertPauseQuery)) {
      throw new IllegalStateException(
          "ERP datasource is enabled but one or more queries are blank: "
              + "app.datasource.erp.query.find-open-orders, "
              + "app.datasource.erp.query.insert-cycle, "
              + "app.datasource.erp.query.insert-pause must all be configured");
    }
  }

  /**
   * Execute the configured open-orders SELECT. The query must return six
   * columns in this order: erp_order_id, machine_code, product_code,
   * target_quantity, status, payload_json.
   *
   * @return open orders read from the ERP, possibly empty
   */
  public List<ErpOrderRow> findOpenOrders() {
    return erp.query(findOpenOrdersQuery, erpOrderRowMapper);
  }

  /**
   * Execute the configured insert-cycle statement with positional parameters.
   * Caller is responsible for converting domain types into JDBC-compatible
   * values in the order expected by the statement.
   *
   * @param params positional parameters of the configured insert-cycle query
   * @return number of rows affected (1 on insert, 0 when the ERP already had it)
   */
  public int insertCycle(Object[] params) {
    return erp.update(insertCycleQuery, params);
  }

  /**
   * Execute the configured insert-pause statement with positional parameters
   * and the matching SQL type hints. Type hints are needed because nullable
   * timestamps require explicit JDBC types.
   *
   * @param params positional parameters of the configured insert-pause query
   * @param types  matching {@link java.sql.Types} hints, same length as params
   * @return number of rows affected (1 on insert, 0 when the ERP already had it)
   */
  public int insertPause(Object[] params, int[] types) {
    return erp.update(insertPauseQuery, params, types);
  }

  /**
   * Execute the configured ping query. Used by the GET /erp/sync/status
   * endpoint to confirm the ERP datasource is reachable.
   */
  public void ping() {
    erp.queryForObject(pingQuery, Integer.class);
  }
}
