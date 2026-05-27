package com.njplastic.njplastic_api.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableJpaRepositories(basePackages = {
    "com.njplastic.njplastic_api.auth.repository",
    "com.njplastic.njplastic_api.audit.repository",
    "com.njplastic.njplastic_api.production.repository"
}, entityManagerFactoryRef = "postgresEntityManagerFactory", transactionManagerRef = "postgresTransactionManager")
public class PostgresDataSourceConfig {

  @Bean
  @Primary
  @FlywayDataSource
  @ConfigurationProperties("app.datasource.postgres")
  public DataSource postgresDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  @Primary
  public LocalContainerEntityManagerFactoryBean postgresEntityManagerFactory(
      EntityManagerFactoryBuilder builder,
      DataSource postgresDataSource) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    properties.put("hibernate.hbm2ddl.auto", "validate");
    properties.put("hibernate.order_by.default_null_ordering", "last");

    return builder
        .dataSource(postgresDataSource)
        .packages(
            "com.njplastic.njplastic_api.auth.domain",
            "com.njplastic.njplastic_api.audit.domain",
            "com.njplastic.njplastic_api.production.domain")
        .persistenceUnit("postgres")
        .properties(properties)
        .build();
  }

  @Bean
  @Primary
  public PlatformTransactionManager postgresTransactionManager(
      EntityManagerFactory postgresEntityManagerFactory) {
    return new JpaTransactionManager(postgresEntityManagerFactory);
  }
}