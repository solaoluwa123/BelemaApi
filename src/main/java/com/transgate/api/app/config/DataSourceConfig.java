/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.transgate.api.app.config;

import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 *
 * @author USER
 */
// DataSourceConfig.java
@Configuration
public class DataSourceConfig {
  @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.second-datasource")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Primary
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate applicationDataConnection(){
        return new JdbcTemplate(primaryDataSource());
    }
    
    @Bean(name = "secondJdbcTemplate")
    public JdbcTemplate applicationDataConnection2(){
        return new JdbcTemplate(secondaryDataSource());
    }
}

