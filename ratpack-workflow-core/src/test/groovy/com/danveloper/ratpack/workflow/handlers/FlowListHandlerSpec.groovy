package com.danveloper.ratpack.workflow.handlers

import com.danveloper.ratpack.workflow.FlowStatusRepository
import com.danveloper.ratpack.workflow.Page
import com.danveloper.ratpack.workflow.WorkState
import com.danveloper.ratpack.workflow.internal.DefaultFlowStatus
import com.danveloper.ratpack.workflow.internal.DefaultWorkStatus
import com.danveloper.ratpack.workflow.server.RatpackWorkflow
import com.fasterxml.jackson.databind.ObjectMapper
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.server.RatpackServer
import ratpack.test.embed.EmbeddedApp
import ratpack.test.embed.internal.EmbeddedAppSupport
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FlowListHandlerSpec extends Specification {

  FlowStatusRepository repo = Mock(FlowStatusRepository)
  ObjectMapper mapper = new ObjectMapper()

  @Delegate
  EmbeddedApp app = fromServer(
      RatpackWorkflow.of { spec ->
        spec
            .flowRepo { w -> repo }
            .serverConfig { d -> d.port(0) }
            .handlers { chain ->
          chain.all(new FlowListHandler())
        }
      })

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
    def page = new Page<>(0, 10, 1, [flowStatus])

    when:
    def resp = httpClient.get()

    then:
    1 * repo.list(0, 10) >> {
      Promise.value(page)
    }
    mapper.writeValueAsString(page) == resp.body.text
  }

  void "should properly page results"() {
    setup:
    def flows = []
    (1..30).each { int i ->
      def flowStatus = new DefaultFlowStatus()
      flowStatus.id = i
      flowStatus.name = "My Flow ${i}".toString()
      flowStatus.description = "My Description ${i}"
      flowStatus.state = i % 2 == 0 ? WorkState.FAILED : WorkState.COMPLETED
      flowStatus.startTime = System.currentTimeMillis()
      flowStatus.endTime = System.currentTimeMillis()
      flowStatus.works = [
          new DefaultWorkStatus()
      ]
      flows << flowStatus
    }
    def page = new Page<>(0, 10, 3, flows)

    when:
    def resp = httpClient.get()

    then:
    1 * repo.list(0, 10) >> {
      Promise.value(page)
    }
    mapper.writeValueAsString(page) == resp.body.text

    when:
    page = new Page<>(1, 10, 3, flows)

    and:
    resp = httpClient.get("?offset=1&limit=10")

    then:
    1 * repo.list(1, 10) >> {
      Promise.value(page)
    }
    mapper.writeValueAsString(page) == resp.body.text
  }
}
