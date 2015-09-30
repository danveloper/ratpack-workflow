package com.danveloper.ratpack.workflow.handlers

import com.danveloper.ratpack.workflow.Page
import com.danveloper.ratpack.workflow.WorkStatusRepository
import com.danveloper.ratpack.workflow.internal.DefaultWorkStatus
import com.danveloper.ratpack.workflow.server.RatpackWorkflow
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
  EmbeddedApp app = EmbeddedApp.fromServer {
    RatpackWorkflow.of { spec ->
      spec
          .workRepo(repo)
          .serverConfig { d -> d.port(0) }
          .handlers { chain ->
        chain.get(new WorkListHandler())
      }
    }
  }

  void "should list flows"() {
    setup:
    def workStatus = new DefaultWorkStatus()
    workStatus.id = "1"
    def page = new Page<>(0, 10, 1, [workStatus])

    when:
    def resp = httpClient.get()

    then:
    new ObjectMapper().writeValueAsString(page) == resp.body.text
    1 * repo.list(0, 10) >> {
      Promise.value(page)
    }
  }

  void "should properly page results"() {
    setup:
    def works = []
    (1..30).each { i ->
      def workStatus = new DefaultWorkStatus()
      workStatus.id = "${i}".toString()
      works << workStatus
    }
    def page = new Page<>(0, 10, 3, [works])

    when:
    def resp = httpClient.get()

    then:
    new ObjectMapper().writeValueAsString(page) == resp.body.text
    1 * repo.list(0, 10) >> {
      Promise.value(page)
    }
  }
}
