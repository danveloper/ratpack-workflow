package com.danveloper.ratpack.workflow.server;

import com.danveloper.ratpack.workflow.handlers.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.handling.Handler;
import ratpack.handling.HandlerDecorator;
import ratpack.impose.Impositions;
import ratpack.registry.Registry;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfig;
import ratpack.server.internal.DefaultRatpackServer;
import ratpack.server.internal.NettyHandlerAdapter;
import ratpack.server.internal.RatpackServerDefinition;
import ratpack.server.internal.ServerCapturer;
import ratpack.service.internal.DefaultEvent;
import ratpack.service.internal.ServicesGraph;
import ratpack.util.Exceptions;

public interface RatpackWorkflow {

  class RegistryHolder {
    Registry registry;
  }

  static RatpackServer of(Action<? super RatpackWorkflowServerSpec> definition) throws Exception {
    RegistryHolder holder = new RegistryHolder();
    Action<RatpackServerSpec> definitionAction = d -> {
      RatpackWorkflowServerSpec spec = new RatpackWorkflowServerSpec((RatpackServerSpec) d);
      definition.execute(spec);
      holder.registry = spec.getRegistryDefaults();
    };

    RatpackServerDefinition serverDefinition = RatpackServerDefinition.build(definitionAction);

    RatpackServer server = new DefaultRatpackServer(definitionAction, Impositions.current()) {
      protected Channel buildChannel(final ServerConfig serverConfig, final ChannelHandler handlerAdapter) throws InterruptedException {
        serverRegistry = holder.registry.join(serverRegistry);
        Handler ratpackHandler = Exceptions.uncheck(() -> {
          Handler h = buildRatpackHandler(serverRegistry, serverDefinition.getHandler());
          h = decorateHandler(h, serverRegistry);
          return h;
        });

        servicesGraph = Exceptions.uncheck(() -> new ServicesGraph(serverRegistry));
        servicesGraph.start(new DefaultEvent(serverRegistry, reloading));
        return super.buildChannel(serverConfig, Exceptions.uncheck(() -> new NettyHandlerAdapter(serverRegistry, ratpackHandler)));
      }

      private Handler decorateHandler(Handler rootHandler, Registry serverRegistry) throws Exception {
        final Iterable<? extends HandlerDecorator> all = serverRegistry.getAll(HANDLER_DECORATOR_TYPE_TOKEN);
        for (HandlerDecorator handlerDecorator : all) {
          rootHandler = handlerDecorator.decorate(serverRegistry, rootHandler);
        }
        return rootHandler;
      }

      private Handler buildRatpackHandler(Registry serverRegistry, Function<? super Registry, ? extends Handler> handlerFactory) throws Exception {
        return handlerFactory.apply(serverRegistry);
      }
    };

    ServerCapturer.capture(server);
    return server;
  }

  static RatpackServer start(Action<? super RatpackWorkflowServerSpec> definition) throws Exception {
    RatpackServer server = of(definition);
    server.start();
    return server;
  }

  static FlowListHandler flowListHandler() {
    return new FlowListHandler();
  }

  static FlowStatusGetHandler flowStatusGetHandler() {
    return new FlowStatusGetHandler();
  }

  static FlowSubmissionHandler flowSubmissionHandler() {
    return new FlowSubmissionHandler();
  }

  static WorkListHandler workListHandler() {
    return new WorkListHandler();
  }

  static WorkStatusGetHandler workStatusGetHandler() {
    return new WorkStatusGetHandler();
  }

  static WorkSubmissionHandler workSubmissionHandler() {
    return new WorkSubmissionHandler();
  }
}
