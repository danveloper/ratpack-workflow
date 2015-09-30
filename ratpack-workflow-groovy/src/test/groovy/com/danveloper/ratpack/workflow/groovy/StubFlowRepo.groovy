package com.danveloper.ratpack.workflow.groovy

import com.danveloper.ratpack.workflow.FlowConfigSource
import com.danveloper.ratpack.workflow.FlowStatus
import com.danveloper.ratpack.workflow.FlowStatusRepository
import com.danveloper.ratpack.workflow.Page
import ratpack.exec.Promise

class StubFlowRepo implements FlowStatusRepository {
  @Override
  Promise<FlowStatus> create(FlowConfigSource config) {
    return null
  }

  @Override
  Promise<FlowStatus> save(FlowStatus status) {
    return null
  }

  @Override
  Promise<FlowStatus> get(String id) {
    return null
  }

  @Override
  Promise<Page<FlowStatus>> list(Integer offset, Integer limit) {
    return null
  }

  @Override
  Promise<Page<FlowStatus>> listRunning(Integer offset, Integer limit) {
    return null
  }

  @Override
  Promise<Page<FlowStatus>> findByTag(Integer offset, Integer limit, String key, String value) {
    return null
  }
}
