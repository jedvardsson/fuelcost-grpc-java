package io.github.jedvardsson.fuelcost.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.slf4j.Logger;

import java.io.Closeable;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

public class GrpcChannelWrapper implements Closeable {

    private static final Logger LOGGER = getLogger(GrpcChannelWrapper.class);

    private final Duration awaitTermination;
    private final ManagedChannel channel;

    private GrpcChannelWrapper(ManagedChannel channel, Duration awaitTermination) {
        this.channel = channel;
        this.awaitTermination = awaitTermination;
    }

    public static GrpcChannelWrapper create(String target, Duration awaitTermination) {
        ManagedChannel channel;
        if (hasPort(target)) {
            channel = ManagedChannelBuilder
                    .forTarget(target)
                    .defaultLoadBalancingPolicy("round_robin")
                    .usePlaintext()
                    .build();
        } else {
            channel = InProcessChannelBuilder.forName(target)
                    .directExecutor()
                    .propagateCauseWithStatus(true)
                    .build();
        }
        return new GrpcChannelWrapper(channel, awaitTermination);
    }

    private static boolean hasPort(String target) {
        URI uri = URI.create(target);
        return uri.getPort() >= 0;
    }

    public ManagedChannel getChannel() {
        return channel;
    }

    @Override
    public void close() {
        channel.shutdown();
        LOGGER.info("shutting down grpc channel: " + channel.authority());
        try {
            if (awaitTermination != null) {
                channel.awaitTermination(awaitTermination.getSeconds(), TimeUnit.SECONDS);
            } else {
                channel.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while waiting for grpc channel to terminate: " + channel.authority());
        }
    }
}
