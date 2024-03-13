package io.github.jedvardsson.fuelcost.grpc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "grpc")
public class GrpcProperties {

    private final int port;
    private final Duration awaitTermination;

    @ConstructorBinding
    public GrpcProperties(int port, @DefaultValue("30 s") Duration awaitTermination) {
        this.port = port;
        this.awaitTermination = awaitTermination;
    }

    public int getPort() {
        return port;
    }

    public Duration getAwaitTermination() {
        return awaitTermination;
    }
}
