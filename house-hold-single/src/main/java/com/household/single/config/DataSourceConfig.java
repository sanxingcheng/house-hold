package com.household.single.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * DataSource configuration for the monolithic JAR.
 * <p>
 * Primary datasource: household_auth (auth-user entities)<br>
 * Secondary datasource: household_wealth (wealth entities, without ShardingSphere)
 * </p>
 */
@Configuration
@EnableTransactionManagement
public class DataSourceConfig {

    // ==================== Auth datasource (primary) ====================

    @Primary
    @Bean(name = "authDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.auth")
    public DataSourceProperties authDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Primary
    @Bean(name = "authDataSource")
    public DataSource authDataSource() {
        return authDataSourceProperties().initializeDataSourceBuilder();
    }

    @Primary
    @Bean(name = "authEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean authEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("authDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.household.authuser.entity")
                .persistenceUnit("auth")
                .properties(jpaProperties())
                .build();
    }

    @Primary
    @Bean(name = "authTransactionManager")
    public PlatformTransactionManager authTransactionManager(
            @Qualifier("authEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // ==================== Wealth datasource (secondary) ====================

    @Bean(name = "wealthDataSourceProperties")
    @ConfigurationProperties(prefix = "spring.datasource.wealth")
    public DataSourceProperties wealthDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "wealthDataSource")
    public DataSource wealthDataSource() {
        return wealthDataSourceProperties().initializeDataSourceBuilder();
    }

    @Bean(name = "wealthEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean wealthEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("wealthDataSource") DataSource dataSource) {
        return builder
                .dataSource(dataSource)
                .packages("com.household.wealth.entity")
                .persistenceUnit("wealth")
                .properties(jpaProperties())
                .build();
    }

    @Bean(name = "wealthTransactionManager")
    public PlatformTransactionManager wealthTransactionManager(
            @Qualifier("wealthEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    // ==================== JPA repos configuration ====================

    @Configuration
    @EnableJpaRepositories(
            basePackages = "com.household.authuser.repository",
            entityManagerFactoryRef = "authEntityManagerFactory",
            transactionManagerRef = "authTransactionManager"
    )
    static class AuthJpaRepositoriesConfig {
    }

    @Configuration
    @EnableJpaRepositories(
            basePackages = "com.household.wealth.repository",
            entityManagerFactoryRef = "wealthEntityManagerFactory",
            transactionManagerRef = "wealthTransactionManager"
    )
    static class WealthJpaRepositoriesConfig {
    }

    private Map<String, Object> jpaProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        props.put("hibernate.show_sql", "false");
        props.put("hibernate.format_sql", "false");
        return props;
    }

    /**
     * Simple wrapper for DataSourceProperties to avoid import conflicts.
     */
    public static class DataSourceProperties {
        private String url;
        private String username;
        private String password;
        private String driverClassName;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }

        public HikariDataSource initializeDataSourceBuilder() {
            HikariDataSource ds = new HikariDataSource();
            ds.setJdbcUrl(url);
            ds.setUsername(username);
            ds.setPassword(password);
            if (driverClassName != null && !driverClassName.isEmpty()) {
                ds.setDriverClassName(driverClassName);
            }
            ds.setMaximumPoolSize(10);
            return ds;
        }
    }
}
