package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.FlowStatusRepository;
import com.danveloper.ratpack.workflow.GroovyWorkChain;
import com.danveloper.ratpack.workflow.WorkStatusRepository;
import com.danveloper.ratpack.workflow.server.GroovyRatpackWorkflow;
import com.danveloper.ratpack.workflow.server.RatpackWorkflow;
import com.danveloper.ratpack.workflow.server.RatpackWorkflowServerSpec;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GroovySystem;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.groovy.Groovy;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.groovy.internal.GroovyVersionCheck;
import ratpack.groovy.internal.StandaloneScriptBacking;
import ratpack.guice.Guice;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerSpec;
import ratpack.server.ServerConfig;
import ratpack.server.ServerConfigBuilder;
import ratpack.util.Exceptions;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class StandaloneWorkflowScriptBacking implements Action<Closure<?>> {
  private final static AtomicReference<Action<? super RatpackServer>> CAPTURE_ACTION = new AtomicReference<>(null);
  private final ThreadLocal<RatpackServer> running = new ThreadLocal<>();

  public static void captureNext(Action<? super RatpackServer> action) {
    CAPTURE_ACTION.set(action);
  }

  public void execute(final Closure<?> closure) throws Exception {
    GroovyVersionCheck.ensureRequiredVersionUsed(GroovySystem.getVersion());

    Optional.ofNullable(running.get()).ifPresent(s -> Exceptions.uncheck(s::stop));

    RatpackServer ratpackServer;
    Path scriptFile = ClosureUtil.findScript(closure);
    if (scriptFile == null) {
      ratpackServer = RatpackWorkflow.of(server -> ClosureUtil.configureDelegateFirst(new RatpackBacking(server), closure));
    } else {
      ratpackServer = RatpackWorkflow.of(GroovyRatpackWorkflow.Script.app(scriptFile));
      Action<? super RatpackServer> action = CAPTURE_ACTION.getAndSet(null);
      if (action != null) {
        action.execute(ratpackServer);
      }
    }

    ratpackServer.start();
    running.set(ratpackServer);
  }

  private static class RatpackBacking implements GroovyRatpackWorkflow.Ratpack {
    private final RatpackWorkflowServerSpec server;

    public RatpackBacking(RatpackWorkflowServerSpec server) {
      this.server = server;
    }

    @Override
    public void bindings(Closure<?> configurer) {
      server.registry(Guice.registry(ClosureUtil.delegatingAction(configurer)));
    }

    @Override
    public void handlers(Closure<?> configurer) {
      Exceptions.uncheck(() -> server.handlers(Groovy.chainAction(configurer)));
    }

    @Override
    public void serverConfig(Closure<?> configurer) {
      ServerConfigBuilder builder = ServerConfig.builder().development(true);
      ClosureUtil.configureDelegateFirst(builder, configurer);
      server.serverConfig(builder);
    }

    @Override
    public void workflow(Closure<?> configurer) {
      Exceptions.uncheck(() -> server.workflow(ClosureUtil.delegatingAction(configurer)));
    }

    @Override
    public void workRepo(WorkStatusRepository workRepo) {
      server.workRepo(workRepo);
    }

    @Override
    public void flowRepo(Function<WorkStatusRepository, FlowStatusRepository> flowRepoFunction) {
      server.flowRepo(flowRepoFunction);
    }
  }
}
