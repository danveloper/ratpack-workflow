package com.danveloper.ratpack.workflow.internal.capture;

import com.danveloper.ratpack.workflow.server.GroovyRatpackWorkflow;
import groovy.lang.Script;
import ratpack.func.BiFunction;
import ratpack.func.Function;
import ratpack.groovy.script.internal.ScriptEngine;

import java.nio.file.Path;

public class RatpackWorkflowDslScriptCapture implements BiFunction<Path, String, RatpackWorkflowDslClosures> {
  private final boolean compileStatic;
  private final Function<? super RatpackWorkflowDslClosures, ? extends GroovyRatpackWorkflow.Ratpack> function;

  public RatpackWorkflowDslScriptCapture(boolean compileStatic,
                                         Function<? super RatpackWorkflowDslClosures, ? extends GroovyRatpackWorkflow.Ratpack> function) {
    this.compileStatic = compileStatic;
    this.function = function;
  }

  public RatpackWorkflowDslClosures apply(Path file, String script) throws Exception {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    ScriptEngine<Script> scriptEngine = new ScriptEngine<>(classLoader, compileStatic, Script.class);
    return RatpackWorkflowDslClosures.capture(function, () -> scriptEngine.create(file.getFileName().toString(), file, script).run());
  }
}
