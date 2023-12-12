package com.zs.forex.common.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;


@Configuration
public class DataSourceConfig {

    @Autowired
    private Environment environment;

    public DataSource readDataSource() {
        String jdbcUrl = environment.getProperty("spring.datasource.read.jdbc-url");
        String driverClassName = environment.getProperty("spring.datasource.read.driver-class-name");
        String password = environment.getProperty("spring.datasource.read.password");
        String username = environment.getProperty("spring.datasource.read.username");

        return DataSourceBuilder.create().driverClassName(driverClassName)
                .url(jdbcUrl)
                .password(password)
                .username(username).build();
    }

    public DataSource writeDataSource() {

        String jdbcUrl = environment.getProperty("spring.datasource.write.jdbc-url");
        String driverClassName = environment.getProperty("spring.datasource.write.driver-class-name");
        String password = environment.getProperty("spring.datasource.write.password");
        String username = environment.getProperty("spring.datasource.write.username");

        return DataSourceBuilder.create().driverClassName(driverClassName)
                .url(jdbcUrl)
                .password(password)
                .username(username)
                .build();
    }

    public DataSource currentDataSource() {

        String jdbcUrl = environment.getProperty("spring.datasource.current.jdbc-url");
        String driverClassName = environment.getProperty("spring.datasource.current.driver-class-name");
        String password = environment.getProperty("spring.datasource.current.password");
        String username = environment.getProperty("spring.datasource.current.username");

        return DataSourceBuilder.create().driverClassName(driverClassName)
                .url(jdbcUrl)
                .password(password)
                .username(username)
                .build();
    }

    @Bean
    public JdbcTemplate readJdbcTemplate() {
        return new JdbcTemplate(readDataSource());
    }

    @Bean
    public JdbcTemplate writeJdbcTemplate() {
        return new JdbcTemplate(writeDataSource());
    }


    @Bean
    public JdbcTemplate currentJdbcTemplate() {
        return new JdbcTemplate(currentDataSource());
    }
}
