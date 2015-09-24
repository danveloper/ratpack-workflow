package com.danveloper.ratpack.workflow.internal

import com.danveloper.ratpack.workflow.WorkState
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class FlowStatusDeSerializerSpec extends Specification {
  def status = new DefaultFlowStatus().with { s ->
    s.id = 1
    s.name = "foo"
    s.description = "bar"
    s.startTime = 123l
    s.endTime = 456l
    s.state = WorkState.COMPLETED
    s.tags = [stack: "prod"]
    s.works = [
        new DefaultWorkStatus()
    ]
    s
  }
  def mapper = new ObjectMapper()
  void "should (de)serialize properly"() {
    setup:
    def json = mapper.writeValueAsString(status)

    when:
    def st = mapper.readValue(json, DefaultFlowStatus)

    then:
    st == status
  }
}
