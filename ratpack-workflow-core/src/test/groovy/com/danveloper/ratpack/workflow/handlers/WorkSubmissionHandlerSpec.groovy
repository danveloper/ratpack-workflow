package com.danveloper.ratpack.workflow.handlers

import com.danveloper.ratpack.workflow.WorkConfigSource
import com.danveloper.ratpack.workflow.WorkProcessor
import com.danveloper.ratpack.workflow.WorkStatus
import com.danveloper.ratpack.workflow.WorkStatusRepository
import com.danveloper.ratpack.workflow.internal.DefaultWorkStatus
import com.danveloper.ratpack.workflow.server.RatpackWorkflow
import ratpack.exec.Promise
import ratpack.registry.Registry
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup
import spock.lang.Specification

class WorkSubmissionHandlerSpec extends Specification {
  WorkStatusRepository repo = Mock(WorkStatusRepository)
  WorkProcessor workProcessor = Mock(WorkProcessor)

  def json = """
        {
          "type": "api/resize",
          "version": "1.0",
          "data": {
            "foo": "bar"
          }
        }
  """

  @AutoCleanup
  @Delegate
  EmbeddedApp app = EmbeddedApp.fromServer {
    RatpackWorkflow.of { spec ->
      spec
          .registryOf { r ->
            r.add(WorkProcessor, workProcessor)
            r.add(WorkStatusRepository, repo)
          }
          .serverConfig { d -> d.port(0) }
          .handlers { chain ->
        chain.post(new WorkSubmissionHandler())
      }
    }
  }

  void "should form a WorkConfigSource and submit for processing"() {
    setup:
    def workStatus = Mock(DefaultWorkStatus)
    workStatus.id = "1"

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
    1 * repo.create(_) >> { WorkConfigSource config ->
      return Promise.value(workStatus)
    }
    1 * workProcessor.start(_) >> { WorkStatus status ->
      assert status == workStatus
      return Promise.value("1")
    }
  }
}
