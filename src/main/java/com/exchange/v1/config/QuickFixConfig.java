package com.exchange.v1.config;

import com.exchange.v1.acceptor.fix.EchoServer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import quickfix.*;

import java.io.InputStream;

@Configuration
public class QuickFixConfig {

    public QuickFixConfig(EchoServer echoServer) {
    }

    @Bean
    public SessionSettings sessionSettings() throws ConfigError {
        InputStream config = getClass()
                .getClassLoader()
                .getResourceAsStream("fix/server.cfg");
        return new SessionSettings(config);
    }

    @Bean
    public MessageStoreFactory messageStoreFactory(
            @Qualifier("sessionSettings") SessionSettings settings) throws ConfigError {
        return new FileStoreFactory(settings);
    }

    @Bean
    public LogFactory logFactory(
            @Qualifier("sessionSettings") SessionSettings settings) throws ConfigError {
        return new FileLogFactory(settings);
    }

    @Bean                            // ← esse estava faltando
    public MessageFactory messageFactory() {
        return new DefaultMessageFactory();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public SocketAcceptor socketAcceptor(
            EchoServer application,
            @Qualifier("sessionSettings") SessionSettings settings,
            MessageStoreFactory storeFactory,
            LogFactory logFactory,
            MessageFactory messageFactory) throws ConfigError {

        return new SocketAcceptor(
                application,
                storeFactory,
                settings,
                logFactory,
                messageFactory
        );
    }
}