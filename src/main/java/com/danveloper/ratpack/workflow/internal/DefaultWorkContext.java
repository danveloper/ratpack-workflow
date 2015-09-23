package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.*;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import io.netty.channel.EventLoop;
import ratpack.exec.ExecController;
import ratpack.exec.Execution;
import ratpack.exec.Promise;
import ratpack.registry.Registry;

import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;

public class DefaultWorkContext implements WorkContext {
  private final WorkConstants workConstants;
  private final Registry registry;
  private static final Work end = ctx -> {
    throw new RuntimeException("work was never handled!");
  };

  @Override
  public WorkStatus getStatus() {
    return getContextRegistry().get(WorkStatus.class);
  }

  @Override
  public Long getStartTime() {
    return getStatus().getStartTime();
  }

  @Override
  public Long getEndTime() {
    return getStatus().getEndTime();
  }

  @Override
  public WorkConfigSource getConfig() {
    return registry.get(WorkConfigSource.class);
  }

  @Override
  public void message(String message) {
    DefaultWorkStatus status = (DefaultWorkStatus)getContextRegistry().get(WorkStatus.class);
    status.getMessages().add(new WorkStatus.WorkStatusMessage(System.currentTimeMillis(), message));
  }

  @Override
  public void insert(Work... works) {
    if (works.length == 0) {
      throw new IllegalArgumentException("works is zero length");
    }
    workConstants.indexes.push(new ChainIndex(works, registry, false));
    next();
  }

  @Override
  public void insert(final Registry registry, Work... works) {
    if (works.length == 0) {
      throw new IllegalArgumentException("works is zero length");
    }
    workConstants.indexes.push(new ChainIndex(works, registry.join(registry), false));
    next();
  }

  @Override
  public void next() {
    Work work = null;
    ChainIndex index = workConstants.indexes.peek();

    if (index.i == 0) {
      WorkStatus status = registry.get(WorkStatus.class);
      if (status instanceof DefaultWorkStatus) {
        ((DefaultWorkStatus)status).setState(WorkState.RUNNING);
        ((DefaultWorkStatus)status).setStartTime(System.currentTimeMillis());
      } else {
        throw new IllegalStateException("cannot update state");
      }
    }

    while (work == null) {
      if (index.hasNext()) {
        work = index.next();
        if (work instanceof WorkChainWork) {
          workConstants.indexes.push(new ChainIndex(((WorkChainWork) work).getWorks(), getContextRegistry(), false));
          index = workConstants.indexes.peek();
          work = null;
        }
      } else {
        workConstants.indexes.pop();
        index = workConstants.indexes.peek();
      }
    }

    try {
      work.handle(this);
    } catch (Exception e) {
      fail(e);
    }
  }

  @Override
  public void next(Registry registry) {
    workConstants.indexes.peek().registry = getContextRegistry().join(registry);
    next();
  }

  @Override
  public void retry() {
    workConstants.indexes.peek().stepBack();
    next();
  }

  @Override
  public void retry(Registry registry) {
    workConstants.indexes.peek().stepBack();
    workConstants.indexes.peek().registry = getContextRegistry().join(registry);
    next();
  }

  @Override
  public void fail(Throwable t) {
    WorkStatus status = registry.get(WorkStatus.class);
    if (status instanceof DefaultWorkStatus) {
      ((DefaultWorkStatus)status).setError(t);
      ((DefaultWorkStatus)status).setState(WorkState.FAILED);
      ((DefaultWorkStatus)status).setEndTime(System.currentTimeMillis());
    } else {
      throw new IllegalStateException("cannot update status", t);
    }
  }

  @Override
  public void complete() {
    this.workConstants.completed = true;
  }

  @Override
  public Execution getExecution() {
    return this.workConstants.execution;
  }

  @Override
  public <O> Optional<O> maybeGet(TypeToken<O> type) {
    return getContextRegistry().maybeGet(type);
  }

  @Override
  public <O> Iterable<? extends O> getAll(TypeToken<O> type) {
    return getContextRegistry().getAll(type);
  }

  public static Promise<String> start(Work[] works, WorkStatus workStatus, WorkStatusRepository workStatusRepository, Registry registry) throws Exception {
    EventLoop el = ExecController.require().getEventLoopGroup().next();
    return start(works, el, workStatus, workStatusRepository, registry);
  }

  public static Promise<String> start(Work[] works, WorkConfigSource workConfigSource, WorkStatusRepository workStatusRepository, Registry registry) throws Exception {
    EventLoop el = ExecController.require().getEventLoopGroup().next();
    return start(works, el, workConfigSource, workStatusRepository, registry);
  }

  public static Promise<String> start(Work[] works, EventLoop eventLoop, WorkConfigSource workConfigSource, WorkStatusRepository workStatusRepository, Registry registry) throws Exception {
    return workStatusRepository.create(workConfigSource)
        .flatMap(workStatus -> start(works, eventLoop, workStatus, workStatusRepository, registry));
  }

  public static Promise<String> start(Work[] works, EventLoop eventLoop, WorkStatus workStatus, WorkStatusRepository workStatusRepository, final Registry outerRegistry) throws Exception {
    Execution.fork()
        .eventLoop(eventLoop)
        .onError(Throwable::printStackTrace)
        .start(e -> {
          WorkConstants workConstants = new WorkConstants();
          workConstants.eventLoop = e.getEventLoop();

          e.onComplete(() -> {
            if (workStatus.getState() != WorkState.RUNNING) {
              return;
            }
            if (workStatus instanceof DefaultWorkStatus) {
              WorkState resultState = workConstants.completed ? WorkState.COMPLETED : WorkState.FAILED;
              ((DefaultWorkStatus) workStatus).setEndTime(System.currentTimeMillis());
              ((DefaultWorkStatus) workStatus).setState(resultState);
              workStatusRepository.save(workStatus).operation().then();
            }
          });

          Registry subregistry = Registry.of(r -> r
                  .add(WorkConstants.class, workConstants)
                  .add(WorkConfigSource.class, workStatus.getConfig())
                  .add(WorkStatus.class, workStatus)
          );
          Registry registry = outerRegistry.join(subregistry);
          ChainIndex endChainIndex = new ChainIndex(new Work[]{end}, registry, true);
          workConstants.indexes.push(endChainIndex);

          ChainIndex chainIndex = new ChainIndex(works, registry, true);
          workConstants.indexes.push(chainIndex);

          DefaultWorkContext context = new DefaultWorkContext(workConstants, registry);
          workConstants.context = context;

          workConstants.execution = e;
          context.next();
        });
    return Promise.value(workStatus.getId());
  }

  DefaultWorkContext(WorkConstants workConstants, Registry registry) {
    this.workConstants = workConstants;
    this.registry = registry;
  }

  private Registry getContextRegistry() {
    return workConstants.indexes.peek().registry;
  }

  private static class WorkConstants {
    private Execution execution;
    private EventLoop eventLoop;
    private WorkContext context;
    private boolean completed;
    private final Deque<ChainIndex> indexes = Lists.newLinkedList();
  }

  private static class ChainIndex implements Iterator<Work> {
    final Work[] works;
    Registry registry;
    final boolean first;
    int i;

    private ChainIndex(Work[] works, Registry registry, boolean first) {
      this.works = works;
      this.registry = registry;
      this.first = first;
    }

    public Work next() {
      return works[i++];
    }

    public void stepBack() {
      i--;
    }

    @Override
    public boolean hasNext() {
      return i < works.length;
    }
  }
}
