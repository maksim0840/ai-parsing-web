package io.github.maksim0840.internalapi.extraction_result.v1.mapper;

import com.google.protobuf.Timestamp;

import java.time.Instant;

public class ProtoTimeMapper {

    public static Timestamp instantToTimestamp(Instant instant) {
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    public static Instant timestampToInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
