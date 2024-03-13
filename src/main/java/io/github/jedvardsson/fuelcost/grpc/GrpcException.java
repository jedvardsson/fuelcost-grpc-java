package io.github.jedvardsson.fuelcost.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class GrpcException {

    // https://github.com/grpc/grpc/blob/master/doc/statuscodes.md

    public static StatusRuntimeException etagNotMatching(String resource) {
        return Status.ABORTED.withDescription(resource + ": etag not matching").asRuntimeException();
    }

    public static StatusRuntimeException alreadyExists(String resource) {
        return Status.ALREADY_EXISTS.withDescription(resource).asRuntimeException();
    }

    public static StatusRuntimeException notFound(String resource) {
        return Status.NOT_FOUND.withDescription(resource).asRuntimeException();
    }

    public static StatusRuntimeException invalidArgument(String field, String message) {
        return Status.INVALID_ARGUMENT.withDescription(field + ": " + message).asRuntimeException();
    }

    public static StatusRuntimeException requiredArgument(String field) {
        return Status.INVALID_ARGUMENT.withDescription(field + ": required").asRuntimeException();
    }
}
