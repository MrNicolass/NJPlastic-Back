package com.njplastic.njplastic_api.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * : ERP access uses native JDBC (no JPA), vendor-agnostic so the same
 * configuration works against SQL Server, Oracle or PostgreSQL ERP backends.
 * The bean is only created when app.datasource.erp.enabled=true so the
 * application can boot in dev/CI environments without a real ERP database.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.datasource.erp", name = "enabled", havingValue = "true")
public class ErpDataSourceConfig {

  @Bean(name = "erpDataSource")
  @ConfigurationProperties("app.datasource.erp")
  public DataSource erpDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean(name = "erpJdbcTemplate")
  public JdbcTemplate erpJdbcTemplate(@Qualifier("erpDataSource") DataSource erpDataSource) {
    return new JdbcTemplate(erpDataSource);
  }
}