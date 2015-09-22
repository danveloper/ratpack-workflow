package com.danveloper.ratpack.workflow.internal

import com.danveloper.ratpack.workflow.FlowConfigSource
import com.danveloper.ratpack.workflow.FlowStatusRepository
import com.danveloper.ratpack.workflow.Work
import com.danveloper.ratpack.workflow.WorkStatusRepository
import com.google.common.io.ByteSource
import ratpack.config.ConfigData
import ratpack.registry.Registry
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class DefaultWorkProcessorSpec extends Specification {

  @AutoCleanup
  ExecHarness execHarness = ExecHarness.harness(Runtime.getRuntime().availableProcessors())

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
          "type": "foo",
          "version": "1.0",
          "data": {
            "foo": "bar"
          }
        },
        {
          "type": "bar",
          "version": "1.0",
          "data": {
            "direction": "up"
          }
        }
      ]
    }
  """
  def configData = ConfigData.of { d ->
    d.json(ByteSource.wrap(json.bytes)).build()
  }

  void "should invoke all work for a given description and chain"() {
    setup:
    CountDownLatch latch = new CountDownLatch(3)
    DefaultWorkChain chain = new DefaultWorkChain(Registry.empty())
        .work("foo", "1.0") { ctx ->
          latch.countDown()
          ctx.next()
        }
        .work("bar", "1.0") { ctx ->
          latch.countDown()
          ctx.next()
        }
        .all {
          latch.countDown()
        }
    WorkStatusRepository workStatusRepository = new InMemoryWorkStatusRepository()
    FlowStatusRepository flowStatusRepository = new InMemoryFlowStatusRepository(workStatusRepository)
    DefaultWorkProcessor processor = new DefaultWorkProcessor(chain.works as Work[], workStatusRepository, flowStatusRepository)

    when:
    execHarness.run {
      processor.onStart(null)
      flowStatusRepository.create(FlowConfigSource.of(configData)).then { flowStatus ->
        processor.start(flowStatus).operation().then()
      }
    }

    and:
    execHarness.controller.eventLoopGroup.awaitTermination(1, TimeUnit.SECONDS)

    and:
    latch.await(10, TimeUnit.SECONDS)

    then:
    0l == latch.count
  }
}
