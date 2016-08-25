package com.danveloper.ratpack.workflow.server;

import com.danveloper.ratpack.workflow.handlers.*;
import com.google.common.collect.Maps;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import ratpack.func.Action;
import ratpack.impose.Impositions;
import ratpack.registry.Registry;
import ratpack.server.RatpackServer;
import ratpack.server.RatpackServerSpec;
import ratpack.server.internal.DefaultRatpackServer;
import ratpack.server.internal.ServerCapturer;
import ratpack.util.Exceptions;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

public interface RatpackWorkflow {

  class RegistryHolder {
    public static Registry registry;
  }

  static RatpackServer of(Action<? super RatpackWorkflowServerSpec> definition) throws Exception {
    Action<RatpackServerSpec> definitionAction = d -> {
      RatpackWorkflowServerSpec spec = new RatpackWorkflowServerSpec((RatpackServerSpec) d);
      definition.execute(spec);
      RegistryHolder.registry = spec.getRegistryDefaults();
    };

    ClassPool cp = ClassPool.getDefault();
    CtClass clazz = cp.get(DefaultRatpackServer.class.getCanonicalName());
    if (!clazz.isFrozen()) {
      CtMethod m = clazz.getDeclaredMethod("buildAdapter");
      m.insertAt(262, true, "serverRegistry = com.danveloper.ratpack.workflow.server.RatpackWorkflow.RegistryHolder.registry.join(serverRegistry);");
      clazz.freeze();
    }
    ClassLoader cl = new DelegatingClassLoader(Thread.currentThread().getContextClassLoader());
    Class rc = clazz.toClass(cl);
    Object delegate = rc.getDeclaredConstructors()[0].newInstance(definitionAction, Impositions.current());

    RatpackServer s1 = (RatpackServer)Proxy.newProxyInstance(RatpackWorkflow.class.getClassLoader(), new Class[]{RatpackServer.class}, new InvocationHandler() {
      public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(delegate, args);
      }
    });

    ServerCapturer.capture(s1);
    return s1;
  }

  class DelegatingClassLoader extends ClassLoader {

    private final Map<String, Class> classCache = Maps.newHashMap();

    DelegatingClassLoader(ClassLoader parent) {
      super(parent);
    }

    @Override
    public Class<?> loadClass(String clas) throws ClassNotFoundException {
      if (clas.startsWith("ratpack.server.internal.DefaultRatpackServer")) {
        return classCache.computeIfAbsent(clas, this::load);
      }
      return super.loadClass(clas);
    }

    private Class load(String clas) {
      return Exceptions.uncheck(() -> {
        ClassPool cp = ClassPool.getDefault();
        CtClass ct = cp.get(clas);
        return ct.toClass(this);
      });
    }
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
