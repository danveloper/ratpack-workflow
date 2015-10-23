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

  def cleanup() {
    jedisPool.resource.flushAll()
  }

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
    def page = execControl.yield { repo.list(0, 10) }.valueOrThrow

    and:
    def statuses = page.objs

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
    def page = execControl.yield { repo.listRunning(0, 10) }.valueOrThrow

    and:
    def statuses = page.objs

    then:
    1 == statuses.size()
    statuses[0] == status

    when:
    status.toMutable().state = WorkState.COMPLETED

    and:
    status = execControl.yield { repo.save(status) }.valueOrThrow

    and:
    page = execControl.yield { repo.listRunning(0, 10) }.valueOrThrow

    and:
    statuses = page.objs

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
      repo.findByTag(0, 10, "stack", "prod")
    }.valueOrThrow.objs

    then:
    1 == statuses.size()
    statuses[0] == status

    when:
    statuses = ExecHarness.yieldSingle {
      repo.findByTag(0, 10, "phase", "build")
    }.valueOrThrow.objs

    then:
    1 == statuses.size()
    statuses[0] == status

    when:
    statuses = ExecHarness.yieldSingle {
      repo.findByTag(0, 10, "nothing", "")
    }.valueOrThrow.objs

    then:
    0 == statuses.size()
  }

  void "should properly page FlowStatuses"() {
    setup:
    def statuses = (1..30).collect {
      ExecHarness.yieldSingle {
        repo.create(config)
      }.value
    }
    def reversed = statuses.reverse()

    when:
    def page = ExecHarness.yieldSingle {
      repo.list(0, 10)
    }.valueOrThrow

    and:
    def ids = page.objs*.id

    then:
    page.offset == 0
    page.limit == 10
    page.numPages == 3
    page.objs.size() == 10
    statuses.containsAll(page.objs)
    ids == reversed[0..9]*.id

    when:
    def page2 = ExecHarness.yieldSingle {
      repo.list(1, 10)
    }.valueOrThrow

    and:
    def ids2 = page2.objs*.id

    then:
    page2.offset == 1
    page2.limit == 10
    page2.numPages == 3
    page2.objs.size() == 10
    statuses.containsAll(page2.objs)
    ids.findAll { ids2.contains(it) }.size() == 0
    ids2 == reversed[10..19]*.id
  }

  void "num pages should be 1 when max records is lt limit"() {
    setup:
      ExecHarness.yieldSingle {
        repo.create(config)
      }.value

    when:
    def page = ExecHarness.yieldSingle {
      repo.list(0, 10)
    }.valueOrThrow

    then:
    page.offset == 0
    page.limit == 10
    page.numPages == 1
    page.objs.size() == 1
  }

  void "should be able to add tags to an existing flowStatus"() {
    setup:
    def status = ExecHarness.yieldSingle {
      repo.create(config)
    }.value

    when:
    status.tags["newTag"] = "true"

    and:
    ExecHarness.executeSingle {
      repo.save(status).operation()
    }

    and:
    def upd = ExecHarness.yieldSingle {
      repo.get(status.getId())
    }.valueOrThrow

    then:
    upd.tags.containsKey("newTag")
    upd.tags["newTag"] == "true"
  }
}
