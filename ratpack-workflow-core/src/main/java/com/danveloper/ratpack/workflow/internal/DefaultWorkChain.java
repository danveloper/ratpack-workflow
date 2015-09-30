package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.Work;
import com.danveloper.ratpack.workflow.WorkChain;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import ratpack.func.Action;
import ratpack.registry.Registry;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultWorkChain implements WorkChain {
  private final List<Work> works = Lists.newArrayList();
  private final Registry registry;

  public DefaultWorkChain(Registry registry) {
    this.registry = registry;
  }

  @Override
  public WorkChain all(Class<? extends Work> work) {
    works.add(new TypedVersionedWork("", "", registry.get(work)));
    return this;
  }

  @Override
  public WorkChain work(String type, String version, Work work) {
    works.add(new TypedVersionedWork(type, version, work));
    return this;
  }

  @Override
  public WorkChain work(String type, String version, Class<? extends Work> work) {
    Optional<? extends Work> workOption = registry.maybeGet(TypeToken.of(work));
    if (!workOption.isPresent()) {
      throw new IllegalArgumentException("could not find class " + work + " in the registry!");
    }
    return work(type, version, workOption.get());
  }

  @Override
  public WorkChain flow(String type, String version, Action<WorkChain> subchain) throws Exception {
    DefaultWorkChain sub = new DefaultWorkChain(registry);
    subchain.execute(sub);
    List<Work> subworks = sub.works.stream()
        .map(w -> {
          String wType = ((TypedVersionedWork)w).getType();
          Work delegate = ((TypedVersionedWork)w).getDelegate();
          String normalizedType = wType != null && wType.length() > 0 ? type + "/" + wType : wType;
          return new TypedVersionedWork(normalizedType, version, delegate);
        })
        .collect(Collectors.toList());

    WorkChainWork delegate = new WorkChainWork(subworks.toArray(new Work[subworks.size()]));
    Work work = new PrefixMatchingTypedVersionedWork(type, version, delegate);
    works.add(work);
    return this;
  }

  @Override
  public List<Work> getWorks() {
    return this.works;
  }
}
