package io.github.maksim0840.apigateway.grpc;

import io.github.maksim0840.apigateway.exception.DataNotFoundException;
import io.github.maksim0840.apigateway.exception.DataUnavailableException;
import io.github.maksim0840.apigateway.exception.InternalServiceException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class GrpcExceptionMapper {

    public static RuntimeException map(StatusRuntimeException e) {
        Status.Code code = e.getStatus().getCode();
        String description = e.getStatus().getDescription();

        return switch (code) {
            case INVALID_ARGUMENT -> new IllegalArgumentException(description, e);
            case INTERNAL -> new InternalServiceException(description, e);
            case NOT_FOUND -> new DataNotFoundException(description, e);
            case UNAVAILABLE -> new DataUnavailableException(description, e);
            default -> new RuntimeException(description, e);
        };
    }
}
