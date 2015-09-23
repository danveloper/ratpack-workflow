package com.danveloper.ratpack.workflow

import com.danveloper.ratpack.workflow.FlowStatusRepository
import com.danveloper.ratpack.workflow.WorkChain
import com.danveloper.ratpack.workflow.guice.WorkflowModule
import com.danveloper.ratpack.workflow.handlers.FlowListHandler
import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.func.Action
import ratpack.guice.Guice
import ratpack.registry.Registry
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FunctionalSpec extends Specification {

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

  @AutoCleanup
  @Delegate
  EmbeddedApp app = EmbeddedApp.of({ spec -> spec
      .handlers { chain -> chain
        .register(WorkflowModule.registry(chain.registry) { wchain ->
          wchain.all { ctx ->
            latch.countDown()
            ctx.complete()
          }
        })
        .prefix("ops") { pchain ->
          pchain.post(WorkflowModule.workSubmissionHandler())
          .get(WorkflowModule.workListHandler())
          .get(":id", WorkflowModule.workStatusGetHandler())
        }
        .prefix("flows") { pchain ->
          pchain.post(WorkflowModule.flowSubmissionHandler())
          .get(WorkflowModule.flowListHandler())
          .get(":id", WorkflowModule.flowStatusGetHandler())
        }
      }
  } as Action)

  void "should be able to submit and retrieve work"() {
    setup:
    latch = new CountDownLatch(1)

    when:
    def resp = httpClient.requestSpec { spec -> spec
      .body { b ->
        b.bytes(singleJson.bytes)
      }
      .headers { h ->
        h.add("Content-Type", "application/json")
      }
    }.post("ops")

    and:
    def ref = new ObjectMapper().readValue(resp.body.text, Map)

    then:
    resp.statusCode == 202
    ref.containsKey("id")

    when:
    resp = httpClient.get("ops/${ref.id}".toString())

    then:
    resp.statusCode == 200
    resp.headers.get("Content-Type") == "application/json"

    when:
    latch.await(1, TimeUnit.SECONDS)

    then:
    0l == latch.count
  }

  void "should be able to submit and retrieve flows"() {
    setup:
    latch = new CountDownLatch(1)

    when:
    def resp = httpClient.requestSpec { spec -> spec
        .body { b ->
      b.bytes(flowJson.bytes)
    }
    .headers { h ->
      h.add("Content-Type", "application/json")
    }
    }.post("flows")

    and:
    def ref = new ObjectMapper().readValue(resp.body.text, Map)

    then:
    resp.statusCode == 202
    ref.containsKey("id")

    when:
    resp = httpClient.get("flows/${ref.id}".toString())

    then:
    resp.statusCode == 200
    resp.headers.get("Content-Type") == "application/json"

    when:
    latch.await(1, TimeUnit.SECONDS)

    then:
    0l == latch.count
  }
}
