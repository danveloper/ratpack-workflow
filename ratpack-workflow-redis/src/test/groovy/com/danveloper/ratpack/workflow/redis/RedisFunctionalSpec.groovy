package com.danveloper.ratpack.workflow.redis

import com.danveloper.ratpack.workflow.server.RatpackWorkflow
import ratpack.test.embed.EmbeddedApp
import redis.clients.jedis.JedisPool

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RedisFunctionalSpec extends RedisRepositorySpec {

  def jedisPool = new JedisPool("localhost", port)

  static CountDownLatch latch

  def singleJson = """
        {
          "type": "w",
          "version": "1.0",
          "data": {
            "foo": "bar"
          }
        }
  """

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

  EmbeddedApp app = EmbeddedApp.fromServer(RatpackWorkflow.of { spec -> spec
    .workRepo(new RedisWorkStatusRepository(jedisPool))
    .flowRepo { w -> new RedisFlowStatusRepository(jedisPool, w) }
    .workflow { chain -> chain
      .all { ctx ->
        latch.countDown()
        ctx.next()
      }
      .all { ctx ->
        latch.countDown()
        ctx.complete()
      }
    }
    .handlers { chain -> chain
      .prefix("ops") { pchain ->
        pchain.post(RatpackWorkflow.workSubmissionHandler())
      }
      .prefix("flows") { pchain ->
        pchain.post(RatpackWorkflow.flowSubmissionHandler())
      }
    }
  })

  void "should be able to submit and execute work"() {
    setup:
    latch = new CountDownLatch(2)

    when:
    app.httpClient.requestSpec { rspec -> rspec
      .body { b ->
        b.text(singleJson)
      }
      .headers { h ->
        h.set("content-type", "application/json")
      }
    }.post("ops")

    and:
    latch.await(5, TimeUnit.SECONDS)

    then:
    0l == latch.count
  }

  void "should be able to submit and execute flows"() {
    setup:
    latch = new CountDownLatch(2)

    when:
    app.httpClient.requestSpec { rspec -> rspec
        .body { b ->
          b.text(flowJson)
        }
        .headers { h ->
          h.set("content-type", "application/json")
        }
    }.post("flows")

    and:
    latch.await(5, TimeUnit.SECONDS)

    then:
    0l == latch.count
  }
}
