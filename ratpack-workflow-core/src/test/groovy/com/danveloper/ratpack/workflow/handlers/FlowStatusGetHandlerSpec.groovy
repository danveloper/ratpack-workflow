package com.danveloper.ratpack.workflow.handlers

import com.danveloper.ratpack.workflow.FlowStatusRepository
import com.danveloper.ratpack.workflow.WorkState
import com.danveloper.ratpack.workflow.internal.DefaultFlowStatus
import com.danveloper.ratpack.workflow.internal.DefaultWorkStatus
import com.danveloper.ratpack.workflow.server.RatpackWorkflow
import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup
import spock.lang.Specification

class FlowStatusGetHandlerSpec extends Specification {
  FlowStatusRepository repo = Mock(FlowStatusRepository)

  @AutoCleanup
  @Delegate
  EmbeddedApp app = EmbeddedApp.fromServer {
    RatpackWorkflow.of { spec ->
      spec
      .flowRepo { w -> repo}
      .serverConfig { d -> d.port(0) }
          .handlers { chain ->
        chain.get(":id", new FlowStatusGetHandler())
      }
    }
  }

  void "should return a FlowStatus by id"() {
    setup:
    def flowStatus = new DefaultFlowStatus()
    flowStatus.id = 1
    flowStatus.name = "My Flow"
    flowStatus.description = "My Description"
    flowStatus.state = WorkState.FAILED
    flowStatus.startTime = System.currentTimeMillis()
    flowStatus.endTime = System.currentTimeMillis()
    flowStatus.works = [
        new DefaultWorkStatus()
    ]

    when:
    def resp = httpClient.get("1")

    then:
    1 * repo.get(_) >> {
      Promise.value(flowStatus)
    }
    new ObjectMapper().writeValueAsString(flowStatus) == resp.body.text
  }
}
