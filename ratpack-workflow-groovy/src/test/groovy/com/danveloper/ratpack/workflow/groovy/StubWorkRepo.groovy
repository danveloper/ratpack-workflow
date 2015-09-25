package com.danveloper.ratpack.workflow.groovy

import com.danveloper.ratpack.workflow.WorkConfigSource
import com.danveloper.ratpack.workflow.WorkStatus
import com.danveloper.ratpack.workflow.WorkStatusRepository
import ratpack.exec.Promise

class StubWorkRepo implements WorkStatusRepository {
  @Override
  Promise<WorkStatus> create(WorkConfigSource source) {
    return null
  }

  @Override
  Promise<WorkStatus> save(WorkStatus status) {
    return null
  }

  @Override
  Promise<List<WorkStatus>> list() {
    return null
  }

  @Override
  Promise<List<WorkStatus>> listRunning() {
    return null
  }

  @Override
  Promise<WorkStatus> get(String id) {
    return null
  }

  @Override
  Promise<Boolean> lock(String id) {
    return null
  }

  @Override
  Promise<Boolean> unlock(String id) {
    return null
  }
}
