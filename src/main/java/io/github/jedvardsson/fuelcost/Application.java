package io.github.jedvardsson.fuelcost;

import io.github.jedvardsson.fuelcost.grpc.GrpcProperties;
import io.grpc.BindableService;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties({GrpcProperties.class})
public class Application {
    @Bean
    public BindableService reflectionService() {
        return ProtoReflectionService.newInstance();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
