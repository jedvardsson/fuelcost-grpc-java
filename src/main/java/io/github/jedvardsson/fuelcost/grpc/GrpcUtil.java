package io.github.jedvardsson.fuelcost.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

import java.util.Objects;
import java.util.concurrent.Callable;

public class GrpcUtil {
    public static <T> void handleResponse(StreamObserver<T> responseObserver, Callable<T> callable) {
        try {
            responseObserver.onNext(callable.call());
            responseObserver.onCompleted();
        } catch (StatusRuntimeException e) {
            responseObserver.onError(e);
        } catch (Throwable t) {
            responseObserver.onError(getStatus(t).withCause(t).asRuntimeException());
        }
    }

    public static Status getStatus(Throwable throwable) {
        return switch (throwable) {
            case null -> throw new NullPointerException("throwable");
            case StatusRuntimeException e -> e.getStatus();
            case IllegalArgumentException e -> Status.INVALID_ARGUMENT.withDescription(e.getMessage());
            case InterruptedException e -> Status.ABORTED.withDescription(e.getMessage());
            default -> Status.INTERNAL.withDescription(throwable.getMessage());
        };
    }
}
