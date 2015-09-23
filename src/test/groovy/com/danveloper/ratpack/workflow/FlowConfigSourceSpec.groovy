package com.danveloper.ratpack.workflow

import com.google.common.io.ByteSource
import ratpack.config.ConfigData
import spock.lang.Specification

class FlowConfigSourceSpec extends Specification {

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

  void "should map json into FlowConfigSource"() {
    setup:
    ConfigData data = ConfigData.of { d ->
      d.json(ByteSource.wrap(json.bytes))
      .build()
    }

    when:
    def config = FlowConfigSource.of(data)

    then:
    config.name == "AWESOMEWORKFLOW-001-EL8_MODE"
    2 == config.works.size()
    config.tags == [stack: "prod", phase: "build"]
    config.works.type == ["api/resize", "api/scale"]
  }
}
