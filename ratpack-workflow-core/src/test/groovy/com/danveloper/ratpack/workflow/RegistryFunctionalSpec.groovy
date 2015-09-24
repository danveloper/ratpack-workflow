package com.danveloper.ratpack.workflow

import com.danveloper.ratpack.workflow.guice.WorkflowModule
import com.danveloper.ratpack.workflow.internal.DefaultWorkStatus
import com.danveloper.ratpack.workflow.internal.TestObject
import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.func.Action
import ratpack.guice.Guice
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RegistryFunctionalSpec extends Specification {
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

  @AutoCleanup
  @Delegate
  EmbeddedApp app = EmbeddedApp.of({ spec -> spec
      .registry(Guice.registry { b -> b
        .bindInstance(TestObject, new TestObject(foo: "bar"))
        .module WorkflowModule.of { chain ->
          chain.all { ctx ->
            TestObject obj = ctx.get(TestObject)
            if (obj.foo != "bar") {
              throw new RuntimeException("!!")
            }
            ctx.complete()
            latch.countDown()
          }
        }
      })
      .handlers { chain -> chain
        .prefix("ops") { pchain ->
          pchain.post(WorkflowModule.workSubmissionHandler())
              .get(WorkflowModule.workListHandler())
              .get(":id", WorkflowModule.workStatusGetHandler())
        }
      }
  } as Action)

  void "should be able to retrieve items from the registry within the work chain"() {
    setup:
    def mapper = new ObjectMapper()
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
    latch.await(10, TimeUnit.SECONDS)

    and:
    def ref = mapper.readValue(resp.body.text, Map)

    and:
    resp = httpClient.get("ops/${ref.id}".toString())

    and:
    def status = mapper.readValue(resp.body.text, Map)

    then:
    status.state == "COMPLETED"
  }
}
