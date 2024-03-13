package io.github.jedvardsson.fuelcost;

import io.github.jedvardsson.fuelcost.grpc.GrpcChannelWrapper;
import io.github.jedvardsson.fuelcost.grpc.GrpcServer;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.time.Duration;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = {Application.class})
public class ApplicationTestConfig {

    @Bean
    public GrpcChannelWrapper grpcChannelWrapper(GrpcServer grpcServer) {
        String target = grpcServer.getLocalTarget();
        Duration awaitTermination = Duration.ofSeconds(2);
        return GrpcChannelWrapper.create(target, awaitTermination);
    }
}
