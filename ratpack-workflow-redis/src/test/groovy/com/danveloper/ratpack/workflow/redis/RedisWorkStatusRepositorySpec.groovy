package com.danveloper.ratpack.workflow.redis

import com.danveloper.ratpack.workflow.WorkConfigSource
import com.danveloper.ratpack.workflow.WorkState
import com.danveloper.ratpack.workflow.internal.DefaultWorkStatus
import com.google.common.io.ByteSource
import ratpack.config.ConfigData
import redis.clients.jedis.JedisPool

class RedisWorkStatusRepositorySpec extends RedisRepositorySpec {

  RedisWorkStatusRepository repo = new RedisWorkStatusRepository(new JedisPool("localhost", port))

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
    def statuses = execControl.yield { repo.list() }.valueOrThrow

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
    def runnings = execControl.yield { repo.listRunning() }.valueOrThrow

    then:
    1 == runnings.size()
    runnings[0].id == status.id

    when:
    ((DefaultWorkStatus)status).@state = WorkState.COMPLETED

    and:
    status = execControl.yield { repo.save(status) }.valueOrThrow

    and:
    runnings = execControl.yield { repo.listRunning() }.valueOrThrow

    then:
    0 == runnings.size()
  }
}
