package com.danveloper.ratpack.workflow.redis

import com.danveloper.ratpack.workflow.WorkConfigSource
import com.danveloper.ratpack.workflow.WorkState
import com.danveloper.ratpack.workflow.internal.DefaultWorkStatus
import com.google.common.io.ByteSource
import ratpack.config.ConfigData
import ratpack.test.exec.ExecHarness
import redis.clients.jedis.JedisPool

class RedisWorkStatusRepositorySpec extends RedisRepositorySpec {

  def jedisPool = new JedisPool("localhost", port)
  RedisWorkStatusRepository repo = new RedisWorkStatusRepository(jedisPool)

  def cleanup() {
    jedisPool.resource.flushAll()
  }

  def d = ConfigData.of { d -> d
      .json(ByteSource.wrap("""
      {
        "type": "api/resize",
        "version": "1.0",
        "data": {
          "foo": "bar"
        }
      }
    """.bytes))
  }
  def config = WorkConfigSource.of(d)

  void "should create WorkStatus objects"() {
    given:
    def status = execControl.yield { repo.create(config) }.valueOrThrow

    expect:
    status.id != null
    status.config.getType() == "api/resize"
    status.config.getVersion() == "1.0"
    status.config.mapData(TestObject).foo == "bar"
  }

  void "should be able to update and save WorkStatus"() {
    when:
    def status = execControl.yield { repo.create(config) }.valueOrThrow

    and:
    ((DefaultWorkStatus)status).@state = WorkState.RUNNING

    and:
    status = execControl.yield { repo.save(status) }.valueOrThrow

    then:
    status.state == WorkState.RUNNING
  }

  void "should be able to list all known WorkStatuses"() {
    when:
    def status = execControl.yield { repo.create(config) }.valueOrThrow

    and:
    def statuses = execControl.yield { repo.list(0, 10) }.valueOrThrow.objs

    then:
    1 == statuses.size()
    statuses[0].id == status.id
  }

  void "should be able to list only running jobs"() {
    when:
    def status = execControl.yield { repo.create(config) }.valueOrThrow

    and:
    ((DefaultWorkStatus)status).@state = WorkState.RUNNING

    and:
    status = execControl.yield { repo.save(status) }.valueOrThrow

    and:
    def runnings = execControl.yield { repo.listRunning(0, 10) }.valueOrThrow.objs

    then:
    1 == runnings.size()
    runnings[0].id == status.id

    when:
    ((DefaultWorkStatus)status).@state = WorkState.COMPLETED

    and:
    status = execControl.yield { repo.save(status) }.valueOrThrow

    and:
    runnings = execControl.yield { repo.listRunning(0, 10) }.valueOrThrow.objs

    then:
    0 == runnings.size()
  }

  void "should properly page WorkStatuses"() {
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
}
