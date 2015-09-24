package com.danveloper.ratpack.workflow.guice

import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.func.Action
import ratpack.guice.Guice
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FunctionalGroovySpec extends Specification {

  def singleJson = """
        {
          "type": "w",
          "version": "1.0",
          "data": {
            "foo": "bar"
          }
        }
  """

  static CountDownLatch latch

  @AutoCleanup
  @Delegate
  EmbeddedApp app = EmbeddedApp.of({ spec -> spec
      .registry(Guice.registry { b -> b
        .module GroovyWorkflowModule.of {
          all {
            latch.countDown()
            next()
          }
          all {
            latch.countDown()
            complete()
          }
        }
      })
      .handlers { chain -> chain
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
}
