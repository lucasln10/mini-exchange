package com.exchange.v1.initiator.config;

import com.exchange.v1.initiator.fix.FixClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.*;

import java.io.InputStream;

@Configuration
public class FixClientConfig {

    public final FixClient fixClient;

    public FixClientConfig(FixClient fixClient) {
        this.fixClient = fixClient;
    }

    @Bean
    public SessionSettings clientSettings() throws ConfigError {
        InputStream config = getClass()
                .getClassLoader()
                .getResourceAsStream("fix/client.cfg");
        return new SessionSettings(config);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public SocketInitiator socketInitiator(
            FixClient fixClient,
            @Qualifier("clientSettings") SessionSettings clientSettings) throws ConfigError {

        return new SocketInitiator(
                fixClient,
                new FileStoreFactory(clientSettings),
                clientSettings,
                new FileLogFactory(clientSettings),
                new DefaultMessageFactory()
        );
    }
}