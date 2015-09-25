package com.danveloper.ratpack.workflow.redis

import com.danveloper.ratpack.workflow.FlowConfigSource
import com.danveloper.ratpack.workflow.WorkState
import com.google.common.io.ByteSource
import ratpack.config.ConfigData
import ratpack.test.exec.ExecHarness
import redis.clients.jedis.JedisPool

class RedisFlowStatusRepositorySpec extends RedisRepositorySpec {
  def flowJson = """
    {
      "name": "AWESOMEWORKFLOW-001-EL8_MODE",
      "description": "My really awesome workflow!",
      "tags": {
        "stack": "prod",
        "phase": "build"
      },
      "works": [
        {
          "type": "w",
          "version": "1.0",
          "data": {
            "foo": "bar"
          }
        }
      ]
    }
  """

  def jedisPool = new JedisPool("localhost", port)
  RedisWorkStatusRepository workRepo = new RedisWorkStatusRepository(jedisPool)
  RedisFlowStatusRepository repo = new RedisFlowStatusRepository(jedisPool, workRepo)
  def d = ConfigData.of { d -> d.json(ByteSource.wrap(flowJson.bytes)) }
  def config = FlowConfigSource.of(d)

  void "should create FlowStatus objects"() {
    given:
    def status = execControl.yield { repo.create(config) }.valueOrThrow

    expect:
    status.id != null
    status.name == "AWESOMEWORKFLOW-001-EL8_MODE"
    status.description == "My really awesome workflow!"
    status.tags == [stack: "prod", phase: "build"]
    status.state == WorkState.NOT_STARTED
    1 == status.works.size()
    status.works[0].config.getType() == "w"
    status.works[0].config.getVersion() == "1.0"
  }

  void "should be able to update and save FlowStatus state"() {
    when:
    def status = execControl.yield { repo.create(config) }.valueOrThrow

    then:
    status.state == WorkState.NOT_STARTED

    when:
    status.toMutable().state = WorkState.RUNNING

    and:
    status = execControl.yield { repo.save(status) }.valueOrThrow

    then:
    status.state == WorkState.RUNNING
  }

  void "should be able to list all known FlowStatuses"() {
    when:
    def status = execControl.yield { repo.create(config) }.valueOrThrow

    and:
    def statuses = execControl.yield { repo.list() }.valueOrThrow

    then:
    1 == statuses.size()
    statuses[0] == status
  }

  void "should be able to list all running FlowStatuses"() {
    when:
    def status = execControl.yield { repo.create(config) }.valueOrThrow

    then:
    status.state == WorkState.NOT_STARTED

    when:
    status.toMutable().state = WorkState.RUNNING

    and:
    status = execControl.yield { repo.save(status) }.valueOrThrow

    and:
    def statuses = execControl.yield { repo.listRunning() }.valueOrThrow

    then:
    1 == statuses.size()
    statuses[0] == status

    when:
    status.toMutable().state = WorkState.COMPLETED

    and:
    status = execControl.yield { repo.save(status) }.valueOrThrow

    and:
    statuses = execControl.yield { repo.listRunning() }.valueOrThrow

    then:
    0 == statuses.size()
  }

  void "should be able to retrieve status by id"() {
    when:
    def status = execControl.yield { repo.create(config) }.valueOrThrow

    and:
    def status2 = execControl.yield { repo.get(status.id) }.valueOrThrow

    then:
    status == status2
  }

  void "should be able to retrieve flow statuses by tags"() {
    setup:
    def status = ExecHarness.yieldSingle {
      repo.create(config)
    }.valueOrThrow

    when:
    def statuses = ExecHarness.yieldSingle {
      repo.findByTag("stack", "prod")
    }.valueOrThrow

    then:
    1 == statuses.size()
    statuses[0] == status

    when:
    statuses = ExecHarness.yieldSingle {
      repo.findByTag("phase", "build")
    }.valueOrThrow

    then:
    1 == statuses.size()
    statuses[0] == status

    when:
    statuses = ExecHarness.yieldSingle {
      repo.findByTag("nothing", "")
    }.valueOrThrow

    then:
    0 == statuses.size()
  }
}
