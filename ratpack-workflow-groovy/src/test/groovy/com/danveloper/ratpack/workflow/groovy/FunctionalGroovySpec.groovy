package com.danveloper.ratpack.workflow.groovy

import com.danveloper.ratpack.workflow.server.RatpackWorkflow
import com.fasterxml.jackson.databind.ObjectMapper
import com.danveloper.ratpack.workflow.groovy.GroovyRatpackWorkflowEmbeddedApp
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
  GroovyRatpackWorkflowEmbeddedApp app = GroovyRatpackWorkflowEmbeddedApp.of {
    workflow {
      all {
        latch.countDown()
        next()
      }
      all {
        latch.countDown()
        complete()
      }
    }
    handlers {
      prefix("ops") {
        post(RatpackWorkflow.workSubmissionHandler())
        get(":id", RatpackWorkflow.workStatusGetHandler())
        get(RatpackWorkflow.workListHandler())
      }
      prefix("flows") {
        post(RatpackWorkflow.flowSubmissionHandler())
        get(":id", RatpackWorkflow.flowStatusGetHandler())
        get(RatpackWorkflow.flowListHandler())
      }
    }
  }

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
