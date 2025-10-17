package com.example.demo_multiple_tm_order.config;

import com.scalar.db.api.TwoPhaseCommitTransactionManager;
import com.scalar.db.service.TransactionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

@Configuration
public class ScalarDbTwoPCConfig {

    @Value("${scalardb.config.file:scalardb.properties}")
    private String scalarDbConfigFile;

    @Bean
    public TwoPhaseCommitTransactionManager twoPhaseCommitTransactionManager() throws IOException {
        TransactionFactory factory = TransactionFactory.create(scalarDbConfigFile);
        return factory.getTwoPhaseCommitTransactionManager();
    }
}
