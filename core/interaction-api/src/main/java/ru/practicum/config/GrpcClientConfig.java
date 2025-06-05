package ru.practicum.config;

import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.grpc.stats.UserActionControllerGrpc;

@Configuration
public class GrpcClientConfig {

    @Bean
    public UserActionControllerGrpc.UserActionControllerBlockingStub collectorClient(
            @GrpcClient("collector-service") UserActionControllerGrpc.UserActionControllerBlockingStub stub) {
        return stub;
    }
}
