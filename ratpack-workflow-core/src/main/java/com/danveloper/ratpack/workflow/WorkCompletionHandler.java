package com.danveloper.ratpack.workflow;

import ratpack.exec.Operation;

public interface WorkCompletionHandler {
  Operation complete();
}
