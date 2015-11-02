package com.danveloper.ratpack.workflow;

import groovy.lang.Closure;
import groovy.lang.DelegatesTo;

public interface GroovyWorkChain extends WorkChain {

  default GroovyWorkChain all(Work work) {
    return (GroovyWorkChain)work("", work::handle);
  }

  default GroovyWorkChain all(@DelegatesTo(value = WorkContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> work) {
    Work w = ctx -> {
      work.setDelegate(ctx);
      work.setResolveStrategy(Closure.DELEGATE_FIRST);
      work.call();
    };
    return all(w);
  }

  default GroovyWorkChain work(String type, @DelegatesTo(value = WorkContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> work) {
    return work(type, "", work);
  }

  GroovyWorkChain work(String type, String version, @DelegatesTo(value = WorkContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> work);

  GroovyWorkChain work(String type, String version, Class<? extends Work> work);

  default GroovyWorkChain flow(String type, @DelegatesTo(value = GroovyWorkChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> subchain) throws Exception {
    return flow(type, "", subchain);
  }

  default GroovyWorkChain insert(@DelegatesTo(value = GroovyWorkChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> chain) throws Exception {
    chain.setDelegate(this);
    chain.setResolveStrategy(Closure.DELEGATE_FIRST);
    chain.call();
    return this;
  }

  GroovyWorkChain flow(String type, String version, @DelegatesTo(value = GroovyWorkChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> subchain) throws Exception;
}
