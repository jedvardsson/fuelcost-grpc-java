package io.github.jedvardsson.fuelcost.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.function.Executable;

public class GrpcAssertions {

    @SuppressWarnings("UnusedReturnValue")
    public static StatusRuntimeException assertThrows(Status.Code expectedStatusCode, Executable executable) {
        StatusRuntimeException e = Assertions.assertThrows(StatusRuntimeException.class, executable);
        if (!e.getStatus().getCode().equals(expectedStatusCode)) {
            throw e;
        }
//        Assertions.assertEquals(expectedStatusCode, e.getStatus().getCode(), "status.code");
        return e;
    }
}
