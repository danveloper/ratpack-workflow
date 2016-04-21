package com.danveloper.ratpack.workflow;

import ratpack.api.Nullable;
import ratpack.exec.Operation;
import ratpack.registry.Registry;

public interface WorkCompletionHandler {
  /**
   * @deprecated use complete(Registry, WorkStatus) instead
   */
  @Deprecated
  default Operation complete() {
    return complete(Registry.empty(), null);
  }

  Operation complete(Registry registry, @Nullable WorkStatus workStatus);
}
