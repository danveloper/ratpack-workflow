package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.GroovyWorkChain;
import com.danveloper.ratpack.workflow.Work;
import com.danveloper.ratpack.workflow.WorkChain;
import com.danveloper.ratpack.workflow.WorkContext;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import ratpack.func.Action;

import java.util.List;

public class DefaultGroovyWorkChain implements GroovyWorkChain {

  private WorkChain delegate;

  public DefaultGroovyWorkChain(WorkChain delegate) {
    this.delegate = delegate;
  }

  @Override
  public GroovyWorkChain work(String type, String version, @DelegatesTo(value = WorkContext.class, strategy = Closure.DELEGATE_FIRST) Closure<?> work) {
    delegate.work(type, version, ctx -> {
      work.setDelegate(ctx);
      work.setResolveStrategy(Closure.DELEGATE_FIRST);
      work.call();
    });
    return this;
  }

  @Override
  public WorkChain all(Class<? extends Work> work) {
    delegate.all(work);
    return this;
  }

  @Override
  public WorkChain work(String type, String version, Work work) {
    delegate.work(type, version, work);
    return this;
  }

  @Override
  public GroovyWorkChain work(String type, String version, Class<? extends Work> work) {
    delegate.work(type, version, work);
    return this;
  }

  @Override
  public GroovyWorkChain flow(String type, String version, @DelegatesTo(value = GroovyWorkChain.class, strategy = Closure.DELEGATE_FIRST) Closure<?> subchain) throws Exception {
    delegate.flow(type, version, wc -> {
      subchain.setDelegate(new DefaultGroovyWorkChain(wc));
      subchain.setResolveStrategy(Closure.DELEGATE_FIRST);
      subchain.call();
    });
    return this;
  }

  @Override
  public GroovyWorkChain flow(String type, String version, @DelegatesTo(value = GroovyWorkChain.class, strategy = Closure.DELEGATE_FIRST) Action<WorkChain> subchain) throws Exception {
    delegate.flow(type, version, subchain);
    return this;
  }

  @Override
  public List<Work> getWorks() {
    return delegate.getWorks();
  }
}
