package com.danveloper.ratpack.workflow.internal

import com.danveloper.ratpack.workflow.FlowConfigSource
import com.google.common.io.ByteSource
import ratpack.config.ConfigData
import ratpack.test.exec.ExecHarness
import spock.lang.Specification

class InMemoryFlowStatusRepositorySpec extends Specification {

  def json = """
    {
      "name": "AWESOMEWORKFLOW-001-EL8_MODE",
      "description": "My really awesome workflow!",
      "tags": {
        "stack": "prod",
        "phase": "build"
      },
      "works": [
        {
          "type": "api/resize",
          "version": "1.0",
          "data": {
            "foo": "bar"
          }
        },
        {
          "type": "api/scale",
          "version": "1.0",
          "data": {
            "direction": "up"
          }
        }
      ]
    }
  """
  def configData = ConfigData.of { d ->
    d.json(ByteSource.wrap(json.bytes)).build()
  }
  def workStatusRepo = new InMemoryWorkStatusRepository()
  def flowStatusRepo = new InMemoryFlowStatusRepository(workStatusRepo)

  void "should create work statuses for each work"() {
    given:
    def status = ExecHarness.yieldSingle {
      flowStatusRepo.create(FlowConfigSource.of(configData))
    }.valueOrThrow

    expect:
    2 == status.works.size()
  }

  void "should be able to retrieve a flow status from the repo"() {
    given:
    def status = ExecHarness.yieldSingle {
      flowStatusRepo.create(FlowConfigSource.of(configData))
    }.valueOrThrow

    expect:
    status == ExecHarness.yieldSingle {
      flowStatusRepo.get(status.getId())
    }.valueOrThrow
  }

  void "should be able to retrieve flow statuses by tags"() {
    setup:
    def status = ExecHarness.yieldSingle {
      flowStatusRepo.create(FlowConfigSource.of(configData))
    }.valueOrThrow

    when:
    def statuses = ExecHarness.yieldSingle {
      flowStatusRepo.findByTag("stack", "prod")
    }.valueOrThrow

    then:
    1 == statuses.size()
    statuses[0] == status

    when:
    statuses = ExecHarness.yieldSingle {
      flowStatusRepo.findByTag("phase", "build")
    }.valueOrThrow

    then:
    1 == statuses.size()
    statuses[0] == status

    when:
    statuses = ExecHarness.yieldSingle {
      flowStatusRepo.findByTag("nothing", "")
    }.valueOrThrow

    then:
    0 == statuses.size()
  }
}
