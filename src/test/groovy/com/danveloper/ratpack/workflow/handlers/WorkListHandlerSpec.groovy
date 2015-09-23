package com.danveloper.ratpack.workflow.handlers

import com.danveloper.ratpack.workflow.WorkStatusRepository
import com.danveloper.ratpack.workflow.internal.DefaultWorkStatus
import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.test.embed.EmbeddedApp
import spock.lang.AutoCleanup
import spock.lang.Specification

class WorkListHandlerSpec extends Specification {
  WorkStatusRepository repo = Mock(WorkStatusRepository)

  @AutoCleanup
  @Delegate
  EmbeddedApp app = EmbeddedApp.of({ spec -> spec
      .registryOf { r -> r.add(WorkStatusRepository, repo) }
      .handlers { chain ->
    chain.all(new WorkListHandler())
  }
  } as Action)

  void "should list flows"() {
    setup:
    def workStatus = new DefaultWorkStatus()
    workStatus.id = "1"

    when:
    def resp = httpClient.get()

    then:
    new ObjectMapper().writeValueAsString([workStatus]) == resp.body.text
    1 * repo.list() >> {
      Promise.value([workStatus])
    }
  }
}
