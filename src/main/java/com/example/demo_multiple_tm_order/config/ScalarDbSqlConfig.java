package com.example.demo_multiple_tm_order.config;

import com.scalar.db.sql.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

@Configuration
public class ScalarDbSqlConfig {

    @Value("${scalardb_sql.config.file:scalardb_sql.properties}")
    private String scalarDbSqlConfigFile;

    @Bean
    public SqlSessionFactory sqlSessionFactory() throws IOException {
        return SqlSessionFactory.builder()
                .withPropertiesFile(scalarDbSqlConfigFile)
                .build();
    }
}
