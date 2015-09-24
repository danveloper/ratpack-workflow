package com.danveloper.ratpack.workflow.guice;

import com.danveloper.ratpack.workflow.GroovyWorkChain;
import com.danveloper.ratpack.workflow.WorkChain;
import com.danveloper.ratpack.workflow.internal.DefaultGroovyWorkChain;
import com.danveloper.ratpack.workflow.internal.DefaultWorkChain;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;

public interface GroovyWorkflowModule {
  static WorkflowModule of(@DelegatesTo(value = GroovyWorkChain.class, strategy = Closure.DELEGATE_FIRST) final Closure<?> workChainAction) {
    Action<WorkChain> chainAct = wc -> {
      workChainAction.setDelegate(wc);
      workChainAction.call();
    };
    return WorkflowModule.of(chainAct).withWorkChainFunction(r -> {
      DefaultWorkChain delegate = new DefaultWorkChain(r);
      return new DefaultGroovyWorkChain(delegate);
    });
  }
}
