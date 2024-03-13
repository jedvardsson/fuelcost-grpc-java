package io.github.jedvardsson.fuelcost.grpc;

import io.github.jedvardsson.fuelcost.ApplicationTestConfig;
import io.grpc.Status;
import io.grpc.reflection.v1alpha.ErrorResponse;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.reflection.v1alpha.ServiceResponse;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = ApplicationTestConfig.class)
@ActiveProfiles({"test"})
class GrpcReflectionServiceTest {

    private final ServerReflectionGrpc.ServerReflectionStub reflectionStub;

    @Autowired
    public GrpcReflectionServiceTest(GrpcChannelWrapper channelWrapper) {
        reflectionStub = ServerReflectionGrpc.newStub(channelWrapper.getChannel());
    }


    private ServerReflectionResponse getServerReflectionInfo(ServerReflectionRequest request) {
        CompletableFuture<ServerReflectionResponse> future = new CompletableFuture<>();
        StreamObserver<ServerReflectionResponse> responseObserver = new StreamObserver<>() {
            private ServerReflectionResponse response = null;

            @Override
            public void onNext(ServerReflectionResponse value) {
                if (response != null) {
                    throw new IllegalStateException("response already received");
                }
                response = value;
            }

            @Override
            public void onError(Throwable t) {
                future.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                future.complete(response);
            }
        };

        StreamObserver<ServerReflectionRequest> requestObserver = null;
        try {
            requestObserver = reflectionStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .serverReflectionInfo(responseObserver);
            requestObserver.onNext(request);
        } catch (Throwable t) {
            if (requestObserver != null) {
                requestObserver.onError(t);
            }
            throw t;
        }
        requestObserver.onCompleted();

        try {
            return future.join();
        } catch (RuntimeException e) {
            throw Status.fromThrowable(e).asRuntimeException();
        }
    }

    private List<String> getServerServiceNames() {
        ServerReflectionResponse response = getServerReflectionInfo(ServerReflectionRequest.newBuilder().setListServices("").build());
        if (response.hasErrorResponse()) {
            ErrorResponse errorResponse = response.getErrorResponse();
            Status status = Status.fromCodeValue(errorResponse.getErrorCode()).withDescription(errorResponse.getErrorMessage());
            throw status.asRuntimeException();
        }
        if (!response.hasListServicesResponse()) {
            throw new RuntimeException("Expected ListServicesResponse");
        }
        List<String> serviceNames = response.getListServicesResponse().getServiceList().stream().map(ServiceResponse::getName).toList();
        return serviceNames;
    }

    @Test
    void testGetServiceNames() {
        Assertions.assertEquals(List.of("grpc.reflection.v1alpha.ServerReflection"), getServerServiceNames());
    }
}