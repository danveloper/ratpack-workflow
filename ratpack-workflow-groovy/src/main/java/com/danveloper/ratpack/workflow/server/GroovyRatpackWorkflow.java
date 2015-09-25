package com.danveloper.ratpack.workflow.server;

import com.danveloper.ratpack.workflow.GroovyWorkChain;
import com.danveloper.ratpack.workflow.WorkChain;
import com.danveloper.ratpack.workflow.internal.DefaultGroovyWorkChain;
import com.danveloper.ratpack.workflow.internal.DefaultWorkChain;
import com.danveloper.ratpack.workflow.internal.StandaloneWorkflowScriptBacking;
import com.danveloper.ratpack.workflow.internal.capture.RatpackWorkflowDslBacking;
import com.danveloper.ratpack.workflow.internal.capture.RatpackWorkflowDslClosures;
import com.danveloper.ratpack.workflow.internal.capture.RatpackWorkflowDslScriptCapture;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import groovy.lang.GroovySystem;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.CharsetUtil;
import ratpack.file.FileSystemBinding;
import ratpack.func.Action;
import ratpack.func.Function;
import ratpack.groovy.Groovy;
import ratpack.groovy.handling.GroovyChain;
import ratpack.groovy.internal.ClosureUtil;
import ratpack.groovy.internal.GroovyVersionCheck;
import ratpack.groovy.internal.ScriptBackedHandler;
import ratpack.groovy.internal.capture.*;
import ratpack.groovy.script.ScriptNotFoundException;
import ratpack.guice.BindingsSpec;
import ratpack.guice.Guice;
import ratpack.handling.Handler;
import ratpack.registry.Registry;
import ratpack.server.*;
import ratpack.server.internal.BaseDirFinder;
import ratpack.server.internal.FileBackedReloadInformant;
import ratpack.server.internal.ServerCapturer;
import ratpack.util.internal.IoUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static ratpack.util.Exceptions.uncheck;

public abstract class GroovyRatpackWorkflow {

  public static RatpackServer of(@DelegatesTo(value = GroovyRatpackWorkflowServerSpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) throws Exception {
    final RatpackWorkflow.RegistryHolder holder = new RatpackWorkflow.RegistryHolder();
    RatpackServer server =  RatpackServer.of(d -> {
      GroovyRatpackWorkflowServerSpec spec = new GroovyRatpackWorkflowServerSpec((RatpackServerSpec)d);
      configurer.setDelegate(spec);
      configurer.setResolveStrategy(Closure.DELEGATE_FIRST);
      configurer.call();
      holder.overrides = spec.getRegistry();
    });
    ServerCapturer.capture(server).registry(r -> r.join(holder.overrides));
    return server;
  }

  public static void ratpack(@DelegatesTo(value = Ratpack.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer) {
    try {
      RatpackScriptBacking.withBacking(closure -> closure.setDelegate(new StandaloneWorkflowScriptBacking()), () -> {});
      RatpackScriptBacking.execute(configurer);
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  public interface Ratpack {
    /**
     * Registers the closure used to configure the {@link ratpack.guice.BindingsSpec} that will back the application.
     *
     * @param configurer The configuration closure, delegating to {@link ratpack.guice.BindingsSpec}
     */
    void bindings(@DelegatesTo(value = BindingsSpec.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);

    /**
     * Registers the closure used to build the handler chain of the application.
     *
     * @param configurer The configuration closure, delegating to {@link GroovyChain}
     */
    void handlers(@DelegatesTo(value = GroovyChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);

    /**
     * Registers the closure used to build the configuration of the server.
     *
     * @param configurer The configuration closure, delegating to {@link ServerConfigBuilder}
     */
    void serverConfig(@DelegatesTo(value = ServerConfigBuilder.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);

    void workflow(@DelegatesTo(value = GroovyWorkChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> configurer);
  }

  public static abstract class Script {

    public static final String DEFAULT_HANDLERS_PATH = "handlers.groovy";
    public static final String DEFAULT_BINDINGS_PATH = "bindings.groovy";
    public static final String DEFAULT_APP_PATH = "ratpack.groovy";

    private Script() {
    }

    public static void checkGroovy() {
      GroovyVersionCheck.ensureRequiredVersionUsed(GroovySystem.getVersion());
    }

    public static Action<? super RatpackWorkflowServerSpec> app() {
      return app(false);
    }

    public static Action<? super RatpackWorkflowServerSpec> app(boolean staticCompile) {
      return app(staticCompile, DEFAULT_APP_PATH, DEFAULT_APP_PATH.substring(0, 1).toUpperCase() + DEFAULT_APP_PATH.substring(1));
    }

    public static Action<? super RatpackWorkflowServerSpec> app(Path script) {
      return app(false, script);
    }

    public static Action<? super RatpackWorkflowServerSpec> app(boolean compileStatic, Path script) {
      return b -> doApp(b, compileStatic, script.getParent(), script);
    }

    public static Action<? super RatpackWorkflowServerSpec> app(boolean staticCompile, String... scriptPaths) {
      return b -> {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        BaseDirFinder.Result baseDirResult = Arrays.stream(scriptPaths)
            .map(scriptPath -> BaseDirFinder.find(classLoader, scriptPath))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .orElseThrow(() -> new ScriptNotFoundException(scriptPaths));

        Path baseDir = baseDirResult.getBaseDir();
        Path scriptFile = baseDirResult.getResource();
        doApp(b, staticCompile, baseDir, scriptFile);
      };
    }

    private static void doApp(RatpackWorkflowServerSpec definition, boolean staticCompile, Path baseDir, Path scriptFile) throws Exception {
      String script = IoUtils.read(UnpooledByteBufAllocator.DEFAULT, scriptFile).toString(CharsetUtil.UTF_8);

      RatpackWorkflowDslClosures closures = new RatpackWorkflowDslScriptCapture(staticCompile, RatpackWorkflowDslBacking::new).apply(scriptFile, script);
      definition.serverConfig(ClosureUtil.configureDelegateFirstAndReturn(loadPropsIfPresent(ServerConfig.builder().baseDir(baseDir), baseDir), closures.getServerConfig()));

      definition.registry(r -> {
        return Guice.registry(bindingsSpec -> {
          bindingsSpec.bindInstance(new FileBackedReloadInformant(scriptFile));
          ClosureUtil.configureDelegateFirst(bindingsSpec, closures.getBindings());
        }).apply((Registry)r);
      });

      definition.handler(r -> {
        return Groovy.chain(r, closures.getHandlers());
      });

      definition.workflow(wc -> {
        ClosureUtil.configureDelegateFirst(new DefaultGroovyWorkChain(wc), closures.getWorkflows());
      });
    }

    private static ServerConfigBuilder loadPropsIfPresent(ServerConfigBuilder serverConfigBuilder, Path baseDir) {
      Path propsFile = baseDir.resolve(BaseDir.DEFAULT_BASE_DIR_MARKER_FILE_PATH);
      if (Files.exists(propsFile)) {
        serverConfigBuilder.props(propsFile);
      }
      return serverConfigBuilder;
    }

    public static Function<Registry, Handler> handlers() {
      return handlers(false);
    }

    public static Function<Registry, Handler> handlers(boolean staticCompile) {
      return handlers(staticCompile, DEFAULT_HANDLERS_PATH);
    }

    public static Function<Registry, Handler> handlers(boolean staticCompile, String scriptPath) {
      checkGroovy();
      return r -> {
        Path scriptFile = r.get(FileSystemBinding.class).file(scriptPath);
        boolean development = r.get(ServerConfig.class).isDevelopment();
        return new ScriptBackedHandler(scriptFile, development,
            new RatpackDslScriptCapture(staticCompile, HandlersOnly::new)
                .andThen(RatpackDslClosures::getHandlers)
                .andThen(c -> Groovy.chain(r, c))
        );
      };
    }

    public static Function<Registry, Registry> bindings() {
      return bindings(false);
    }

    public static Function<Registry, Registry> bindings(boolean staticCompile) {
      return bindings(staticCompile, DEFAULT_BINDINGS_PATH);
    }

    public static Function<Registry, Registry> bindings(boolean staticCompile, String scriptPath) {
      checkGroovy();
      return r -> {
        Path scriptFile = r.get(FileSystemBinding.class).file(scriptPath);
        String script = IoUtils.read(UnpooledByteBufAllocator.DEFAULT, scriptFile).toString(CharsetUtil.UTF_8);
        Closure<?> bindingsClosure = new RatpackDslScriptCapture(staticCompile, BindingsOnly::new).andThen(RatpackDslClosures::getBindings).apply(scriptFile, script);
        return Guice.registry(bindingsSpec -> {
          bindingsSpec.bindInstance(new FileBackedReloadInformant(scriptFile));
          ClosureUtil.configureDelegateFirst(bindingsSpec, bindingsClosure);
        }).apply(r);
      };
    }
  }

}
