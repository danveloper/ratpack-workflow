package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.Work;
import com.danveloper.ratpack.workflow.WorkContext;

public class PrefixMatchingTypedVersionedWork extends TypedVersionedWork {

  public PrefixMatchingTypedVersionedWork(String type, String version, Work delegate) {
    super(type, version, delegate);
  }

  public void handle(WorkContext ctx) {
    if (getType() != null && getVersion() != null &&
        (getType().equals(ctx.getConfig().getType()) || ctx.getConfig().getType().startsWith(getType()+"/"))
        && getVersion().equals(ctx.getConfig().getVersion())) {
      getDelegate().handle(ctx);
    } else {
      ctx.next();
    }
  }
}
