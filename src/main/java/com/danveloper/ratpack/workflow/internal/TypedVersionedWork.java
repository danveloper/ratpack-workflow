package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.Work;
import com.danveloper.ratpack.workflow.WorkContext;

public class TypedVersionedWork implements Work {

  private final String type;
  private final String version;
  private final Work delegate;

  TypedVersionedWork(String type, String version, Work delegate) {
    this.type = type;
    this.version = version;
    this.delegate = delegate;
  }

  @Override
  public void handle(WorkContext ctx) {
    if (("".equals(type) && "".equals(version)) ||
        ("".equals(type) && version.equals(ctx.getConfig().getVersion())) ||
        (type.equals(ctx.getConfig().getVersion()) && "".equals(version)) ||
        (type.equals(ctx.getConfig().getType()) && version.equals(ctx.getConfig().getVersion()))) {
      delegate.handle(ctx);
    } else {
      ctx.next();
    }
  }

  String getType() {
    return this.type;
  }

  String getVersion() {
    return this.version;
  }

  Work getDelegate() {
    return this.delegate;
  }
}
