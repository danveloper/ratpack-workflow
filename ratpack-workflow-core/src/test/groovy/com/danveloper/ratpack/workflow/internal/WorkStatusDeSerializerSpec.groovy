package com.danveloper.ratpack.workflow.internal

import com.danveloper.ratpack.workflow.WorkConfigSource
import com.danveloper.ratpack.workflow.WorkState
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.io.ByteSource
import ratpack.config.ConfigData
import spock.lang.Specification

class WorkStatusDeSerializerSpec extends Specification {

  def d = ConfigData.of { d -> d
    .json(ByteSource.wrap("""
      {
        "type": "api/resize",
        "version": "1.0",
        "data": {
          "foo": "bar"
        }
      }
    """.bytes))
  }

  def status = new DefaultWorkStatus().with { s ->
    s.id = "1"
    s.startTime = 123l
    s.endTime = 456l
    s.state = WorkState.COMPLETED
    s.config = WorkConfigSource.of(d)
    s
  }

  def mapper = new ObjectMapper()

  void "should deserialize DefaultWorkStatus"() {
    setup:
    def json = mapper.writeValueAsString(status)

    when:
    def st = mapper.readValue(json, DefaultWorkStatus)

    then:
    st == status
  }
}
