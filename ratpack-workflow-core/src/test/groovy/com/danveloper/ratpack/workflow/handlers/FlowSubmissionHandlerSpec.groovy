package com.danveloper.ratpack.workflow.handlers

import com.danveloper.ratpack.workflow.FlowConfigSource
import com.danveloper.ratpack.workflow.FlowStatus
import com.danveloper.ratpack.workflow.FlowStatusRepository
import com.danveloper.ratpack.workflow.WorkProcessor
import com.danveloper.ratpack.workflow.internal.DefaultFlowStatus
import com.danveloper.ratpack.workflow.server.RatpackWorkflow
import ratpack.exec.Promise
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup
import spock.lang.Specification

class FlowSubmissionHandlerSpec extends Specification {
  FlowStatusRepository repo = Mock(FlowStatusRepository)
  WorkProcessor workProcessor = Mock(WorkProcessor)

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

  @AutoCleanup
  @Delegate
  EmbeddedApp app = EmbeddedApp.fromServer {
    RatpackWorkflow.of { spec ->
      spec
          .registryOf { r ->
            r.add(WorkProcessor, workProcessor)
            r.add(FlowStatusRepository, repo)
      }
          .serverConfig { d -> d.port(0) }
          .handlers { chain ->
        chain.post(new FlowSubmissionHandler())
      }
    }
  }

  void "should form a FlowConfigSource and submit for processing"() {
    setup:
    def flowStatus = Mock(DefaultFlowStatus)

    when:
    def resp = httpClient.requestSpec { spec ->
      spec.body { b ->
        b.text(json)
      }
      .headers { h ->
        h.set("Content-Type", "application/json")
      }
    }.post()

    then:
    resp.statusCode == 202
    1 * repo.create(_) >> { FlowConfigSource config ->
      assert config.name == "AWESOMEWORKFLOW-001-EL8_MODE"
      assert config.description == "My really awesome workflow!"
      assert config.tags == [stack: "prod", phase: "build"]
      assert config.works.size() == 2
      return Promise.value(flowStatus)
    }
    1 * workProcessor.start(_) >> { FlowStatus status ->
      assert status == flowStatus
      return Promise.value("1")
    }
  }
}
