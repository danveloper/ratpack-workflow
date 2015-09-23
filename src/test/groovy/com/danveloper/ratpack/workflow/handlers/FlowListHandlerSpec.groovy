package com.danveloper.ratpack.workflow.handlers

import com.danveloper.ratpack.workflow.FlowStatusRepository
import com.danveloper.ratpack.workflow.WorkState
import com.danveloper.ratpack.workflow.internal.DefaultFlowStatus
import com.danveloper.ratpack.workflow.internal.DefaultWorkStatus
import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup
import spock.lang.Specification

class FlowListHandlerSpec extends Specification {

  FlowStatusRepository repo = Mock(FlowStatusRepository)

  @AutoCleanup
  @Delegate
  EmbeddedApp app = EmbeddedApp.of({ spec -> spec
    .registryOf { r -> r.add(FlowStatusRepository, repo) }
    .handlers { chain ->
      chain.all(new FlowListHandler())
    }
  } as Action)

  void "should list flows"() {
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
    def resp = httpClient.get()

    then:
    1 * repo.list() >> {
      Promise.value([flowStatus])
    }
    new ObjectMapper().writeValueAsString([flowStatus]) == resp.body.text
  }
}
