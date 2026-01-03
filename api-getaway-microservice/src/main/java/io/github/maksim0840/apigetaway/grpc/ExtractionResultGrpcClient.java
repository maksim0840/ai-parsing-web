package io.github.maksim0840.apigetaway.grpc;

import io.github.maksim0840.extraction_result.v1.ExtractionResultServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
public class ExtractionResultGrpcClient {

    @GrpcClient("extractionResults")
    private ExtractionResultServiceGrpc.ExtractionResultServiceBlockingStub blockingStub;

    pu
}
