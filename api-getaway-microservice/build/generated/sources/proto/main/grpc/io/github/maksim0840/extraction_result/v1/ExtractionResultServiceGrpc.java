package io.github.maksim0840.extraction_result.v1;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@io.grpc.stub.annotations.GrpcGenerated
public final class ExtractionResultServiceGrpc {

  private ExtractionResultServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "extraction_result.v1.ExtractionResultService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest,
      io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse> getCreateMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Create",
      requestType = io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest.class,
      responseType = io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest,
      io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse> getCreateMethod() {
    io.grpc.MethodDescriptor<io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest, io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse> getCreateMethod;
    if ((getCreateMethod = ExtractionResultServiceGrpc.getCreateMethod) == null) {
      synchronized (ExtractionResultServiceGrpc.class) {
        if ((getCreateMethod = ExtractionResultServiceGrpc.getCreateMethod) == null) {
          ExtractionResultServiceGrpc.getCreateMethod = getCreateMethod =
              io.grpc.MethodDescriptor.<io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest, io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Create"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ExtractionResultServiceMethodDescriptorSupplier("Create"))
              .build();
        }
      }
    }
    return getCreateMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest,
      io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse> getGetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Get",
      requestType = io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest.class,
      responseType = io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest,
      io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse> getGetMethod() {
    io.grpc.MethodDescriptor<io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest, io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse> getGetMethod;
    if ((getGetMethod = ExtractionResultServiceGrpc.getGetMethod) == null) {
      synchronized (ExtractionResultServiceGrpc.class) {
        if ((getGetMethod = ExtractionResultServiceGrpc.getGetMethod) == null) {
          ExtractionResultServiceGrpc.getGetMethod = getGetMethod =
              io.grpc.MethodDescriptor.<io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest, io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Get"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ExtractionResultServiceMethodDescriptorSupplier("Get"))
              .build();
        }
      }
    }
    return getGetMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest,
      io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse> getGetListMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetList",
      requestType = io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest.class,
      responseType = io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest,
      io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse> getGetListMethod() {
    io.grpc.MethodDescriptor<io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest, io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse> getGetListMethod;
    if ((getGetListMethod = ExtractionResultServiceGrpc.getGetListMethod) == null) {
      synchronized (ExtractionResultServiceGrpc.class) {
        if ((getGetListMethod = ExtractionResultServiceGrpc.getGetListMethod) == null) {
          ExtractionResultServiceGrpc.getGetListMethod = getGetListMethod =
              io.grpc.MethodDescriptor.<io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest, io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetList"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ExtractionResultServiceMethodDescriptorSupplier("GetList"))
              .build();
        }
      }
    }
    return getGetListMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest,
      io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse> getDeleteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Delete",
      requestType = io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest.class,
      responseType = io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest,
      io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse> getDeleteMethod() {
    io.grpc.MethodDescriptor<io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest, io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse> getDeleteMethod;
    if ((getDeleteMethod = ExtractionResultServiceGrpc.getDeleteMethod) == null) {
      synchronized (ExtractionResultServiceGrpc.class) {
        if ((getDeleteMethod = ExtractionResultServiceGrpc.getDeleteMethod) == null) {
          ExtractionResultServiceGrpc.getDeleteMethod = getDeleteMethod =
              io.grpc.MethodDescriptor.<io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest, io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Delete"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ExtractionResultServiceMethodDescriptorSupplier("Delete"))
              .build();
        }
      }
    }
    return getDeleteMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ExtractionResultServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExtractionResultServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExtractionResultServiceStub>() {
        @java.lang.Override
        public ExtractionResultServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExtractionResultServiceStub(channel, callOptions);
        }
      };
    return ExtractionResultServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports all types of calls on the service
   */
  public static ExtractionResultServiceBlockingV2Stub newBlockingV2Stub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExtractionResultServiceBlockingV2Stub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExtractionResultServiceBlockingV2Stub>() {
        @java.lang.Override
        public ExtractionResultServiceBlockingV2Stub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExtractionResultServiceBlockingV2Stub(channel, callOptions);
        }
      };
    return ExtractionResultServiceBlockingV2Stub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ExtractionResultServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExtractionResultServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExtractionResultServiceBlockingStub>() {
        @java.lang.Override
        public ExtractionResultServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExtractionResultServiceBlockingStub(channel, callOptions);
        }
      };
    return ExtractionResultServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ExtractionResultServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ExtractionResultServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ExtractionResultServiceFutureStub>() {
        @java.lang.Override
        public ExtractionResultServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ExtractionResultServiceFutureStub(channel, callOptions);
        }
      };
    return ExtractionResultServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void create(io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest request,
        io.grpc.stub.StreamObserver<io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateMethod(), responseObserver);
    }

    /**
     */
    default void get(io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest request,
        io.grpc.stub.StreamObserver<io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetMethod(), responseObserver);
    }

    /**
     */
    default void getList(io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest request,
        io.grpc.stub.StreamObserver<io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetListMethod(), responseObserver);
    }

    /**
     */
    default void delete(io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest request,
        io.grpc.stub.StreamObserver<io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ExtractionResultService.
   */
  public static abstract class ExtractionResultServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ExtractionResultServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ExtractionResultService.
   */
  public static final class ExtractionResultServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ExtractionResultServiceStub> {
    private ExtractionResultServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExtractionResultServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ExtractionResultServiceStub(channel, callOptions);
    }

    /**
     */
    public void create(io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest request,
        io.grpc.stub.StreamObserver<io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void get(io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest request,
        io.grpc.stub.StreamObserver<io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getList(io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest request,
        io.grpc.stub.StreamObserver<io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void delete(io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest request,
        io.grpc.stub.StreamObserver<io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ExtractionResultService.
   */
  public static final class ExtractionResultServiceBlockingV2Stub
      extends io.grpc.stub.AbstractBlockingStub<ExtractionResultServiceBlockingV2Stub> {
    private ExtractionResultServiceBlockingV2Stub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExtractionResultServiceBlockingV2Stub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ExtractionResultServiceBlockingV2Stub(channel, callOptions);
    }

    /**
     */
    public io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse create(io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getCreateMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse get(io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse getList(io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getGetListMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse delete(io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest request) throws io.grpc.StatusException {
      return io.grpc.stub.ClientCalls.blockingV2UnaryCall(
          getChannel(), getDeleteMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do limited synchronous rpc calls to service ExtractionResultService.
   */
  public static final class ExtractionResultServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ExtractionResultServiceBlockingStub> {
    private ExtractionResultServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExtractionResultServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ExtractionResultServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse create(io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse get(io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse getList(io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetListMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse delete(io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ExtractionResultService.
   */
  public static final class ExtractionResultServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ExtractionResultServiceFutureStub> {
    private ExtractionResultServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ExtractionResultServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ExtractionResultServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse> create(
        io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse> get(
        io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse> getList(
        io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetListMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse> delete(
        io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CREATE = 0;
  private static final int METHODID_GET = 1;
  private static final int METHODID_GET_LIST = 2;
  private static final int METHODID_DELETE = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_CREATE:
          serviceImpl.create((io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest) request,
              (io.grpc.stub.StreamObserver<io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse>) responseObserver);
          break;
        case METHODID_GET:
          serviceImpl.get((io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest) request,
              (io.grpc.stub.StreamObserver<io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse>) responseObserver);
          break;
        case METHODID_GET_LIST:
          serviceImpl.getList((io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest) request,
              (io.grpc.stub.StreamObserver<io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse>) responseObserver);
          break;
        case METHODID_DELETE:
          serviceImpl.delete((io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest) request,
              (io.grpc.stub.StreamObserver<io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getCreateMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.github.maksim0840.extraction_result.v1.CreateExtractionResultRequest,
              io.github.maksim0840.extraction_result.v1.CreateExtractionResultResponse>(
                service, METHODID_CREATE)))
        .addMethod(
          getGetMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.github.maksim0840.extraction_result.v1.GetExtractionResultRequest,
              io.github.maksim0840.extraction_result.v1.GetExtractionResultResponse>(
                service, METHODID_GET)))
        .addMethod(
          getGetListMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.github.maksim0840.extraction_result.v1.GetListExtractionResultRequest,
              io.github.maksim0840.extraction_result.v1.GetListExtractionResultResponse>(
                service, METHODID_GET_LIST)))
        .addMethod(
          getDeleteMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              io.github.maksim0840.extraction_result.v1.DeleteExtractionResultRequest,
              io.github.maksim0840.extraction_result.v1.DeleteExtractionResultResponse>(
                service, METHODID_DELETE)))
        .build();
  }

  private static abstract class ExtractionResultServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ExtractionResultServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.github.maksim0840.extraction_result.v1.ExtractionResult.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ExtractionResultService");
    }
  }

  private static final class ExtractionResultServiceFileDescriptorSupplier
      extends ExtractionResultServiceBaseDescriptorSupplier {
    ExtractionResultServiceFileDescriptorSupplier() {}
  }

  private static final class ExtractionResultServiceMethodDescriptorSupplier
      extends ExtractionResultServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ExtractionResultServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ExtractionResultServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ExtractionResultServiceFileDescriptorSupplier())
              .addMethod(getCreateMethod())
              .addMethod(getGetMethod())
              .addMethod(getGetListMethod())
              .addMethod(getDeleteMethod())
              .build();
        }
      }
    }
    return result;
  }
}
