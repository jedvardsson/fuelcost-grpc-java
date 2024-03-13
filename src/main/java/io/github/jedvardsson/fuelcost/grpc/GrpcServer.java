package io.github.jedvardsson.fuelcost.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.inprocess.InProcessSocketAddress;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class GrpcServer {
    private static final Logger LOGGER = getLogger(GrpcServer.class);

    private final Server server;
    private final Duration awaitTermination;

    public GrpcServer(GrpcProperties grpcProperties, List<ServerInterceptor> interceptors, List<BindableService> services) {
        awaitTermination = grpcProperties.getAwaitTermination();

        int port = grpcProperties.getPort();
        ServerBuilder<?> serverBuilder = port < 0 ? InProcessServerBuilder.forName(InProcessServerBuilder.generateName()) : ServerBuilder.forPort(port);
        for (ServerInterceptor interceptor : interceptors) {
            serverBuilder.intercept(interceptor);
        }
        for (BindableService service : services) {
            serverBuilder.addService(service);
        }
        server = serverBuilder.build();
    }

    public String getLocalTarget() {
        for (SocketAddress socketAddress : server.getListenSockets()) {
            if (socketAddress instanceof InProcessSocketAddress x) {
                return x.getName();
            }
        }
        return "localhost:" + server.getPort();
    }

    @PostConstruct
    public void start() throws IOException {
        server.start();
        LOGGER.info("Grpc server started: {}", server.getListenSockets());
    }

    @PreDestroy
    public void shutdown() {
        server.shutdown();
        LOGGER.info("Grpc server shutting down: " + server.getPort());
        try {
            if (awaitTermination != null) {
                server.awaitTermination(awaitTermination.getSeconds(), TimeUnit.SECONDS);
            } else {
                server.awaitTermination();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.warn("Interrupted while waiting for grpc server to terminate: " + server.getPort());
        }
    }
}
