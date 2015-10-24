package com.danveloper.ratpack.workflow.internal

import com.danveloper.ratpack.workflow.*
import com.danveloper.ratpack.workflow.server.WorkChainConfig
import com.google.common.io.ByteSource
import ratpack.config.ConfigData
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.registry.Registry
import ratpack.server.internal.DefaultEvent
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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
    Action<WorkChain> actChain = { chain -> chain
      .work("foo", "1.0") { ctx ->
        latch.countDown()
        ctx.next()
      }
      .work("bar", "1.0") { ctx ->
        latch.countDown()
        ctx.next()
      }
      .all { ctx ->
        latch.countDown()
        ctx.complete()
      }
    }
    WorkStatusRepository workStatusRepository = new InMemoryWorkStatusRepository()
    FlowStatusRepository flowStatusRepository = new InMemoryFlowStatusRepository(workStatusRepository)
    def workChainConfig = new WorkChainConfig()
    workChainConfig.action = actChain
    DefaultWorkProcessor processor = new DefaultWorkProcessor()
    Registry registry = Registry.of() { r ->
      r.add(WorkStatusRepository, workStatusRepository)
      r.add(FlowStatusRepository, flowStatusRepository)
      r.add(WorkChainConfig, workChainConfig)
    }

    when:
    execHarness.run {
      processor.onStart(new DefaultEvent(registry, false))
      flowStatusRepository.create(FlowConfigSource.of(configData)).then { flowStatus ->
        processor.start(flowStatus).operation().then()
      }
    }

    and:
    latch.await(10, TimeUnit.SECONDS)

    then:
    0l == latch.count
  }

  void "should fail a flow if one of the works has failed"() {
    setup:
    AtomicInteger adder = new AtomicInteger()
    Action<WorkChain> actChain = { chain -> chain
      .work("foo", "1.0") { ctx ->
        throw new RuntimeException("!!")
      }
      .work("bar", "1.0") { ctx ->
        adder.incrementAndGet()
        ctx.next()
      }
      .all { ctx ->
        adder.incrementAndGet()
      }
    }
    WorkStatusRepository workStatusRepository = new InMemoryWorkStatusRepository()
    FlowStatusRepository flowStatusRepository = new InMemoryFlowStatusRepository(workStatusRepository)
    def workChainConfig = new WorkChainConfig()
    workChainConfig.action = actChain
    DefaultWorkProcessor processor = new DefaultWorkProcessor()
    Registry registry = Registry.of() { r ->
      r.add(WorkStatusRepository, workStatusRepository)
      r.add(FlowStatusRepository, flowStatusRepository)
      r.add(WorkChainConfig, workChainConfig)
    }

    when:
    execHarness.run {
      processor.onStart(new DefaultEvent(registry, false))
      flowStatusRepository.create(FlowConfigSource.of(configData)).then { flowStatus ->
        processor.start(flowStatus).operation().then()
      }
    }

    and:
    execHarness.controller.eventLoopGroup.awaitTermination(5, TimeUnit.SECONDS)

    and:
    def page = execHarness.yield {
      flowStatusRepository.list(0, 10)
    }.valueOrThrow

    and:
    def flows = page.objs

    then:
    0 == adder.get()
    1 == flows.size()
    flows[0].state == WorkState.FAILED
    flows[0].works[0].state == WorkState.FAILED
    flows[0].works[0].error.startsWith("java.lang.RuntimeException: !!")
    flows[0].works[1].state == WorkState.NOT_STARTED
  }

  void "should invoke interceptors before flows start"() {
    setup:
    def latch = new CountDownLatch(1)
    def invocationList = []
    FlowPreStartInterceptor i = { s -> latch.countDown(); invocationList << 1; Promise.value(s) }
    Action<WorkChain> actChain = { chain -> chain.all { c -> invocationList << 2; c.complete() } }
    WorkStatusRepository workStatusRepository = new InMemoryWorkStatusRepository()
    FlowStatusRepository flowStatusRepository = new InMemoryFlowStatusRepository(workStatusRepository)
    def workChainConfig = new WorkChainConfig()
    workChainConfig.action = actChain
    DefaultWorkProcessor processor = new DefaultWorkProcessor()
    Registry registry = Registry.of() { r ->
      r.add(WorkStatusRepository, workStatusRepository)
      r.add(FlowStatusRepository, flowStatusRepository)
      r.add(WorkChainConfig, workChainConfig)
      r.add(FlowPreStartInterceptor, i)
    }

    when:
    execHarness.run {
      processor.onStart(new DefaultEvent(registry, false))
      flowStatusRepository.create(FlowConfigSource.of(configData)).then { flowStatus ->
        processor.start(flowStatus).operation().then()
      }
    }

    and:
    execHarness.controller.eventLoopGroup.awaitTermination(5, TimeUnit.SECONDS)

    and:
    latch.await(5, TimeUnit.SECONDS)

    then:
    [1, 2, 2] == invocationList
    0l == latch.count
  }

  void "should invoke error handler when error occurs"() {
    setup:
    CountDownLatch latch = new CountDownLatch(1)
    Action<WorkChain> actChain = { chain -> chain
      .all {
        throw new RuntimeException()
      }
    }
    FlowErrorHandler errorHandler = { r, f -> latch.countDown() }
    WorkStatusRepository workStatusRepository = new InMemoryWorkStatusRepository()
    FlowStatusRepository flowStatusRepository = new InMemoryFlowStatusRepository(workStatusRepository)
    def workChainConfig = new WorkChainConfig()
    workChainConfig.action = actChain
    DefaultWorkProcessor processor = new DefaultWorkProcessor()
    Registry registry = Registry.of() { r ->
      r.add(WorkStatusRepository, workStatusRepository)
      r.add(FlowStatusRepository, flowStatusRepository)
      r.add(WorkChainConfig, workChainConfig)
      r.add(FlowErrorHandler, errorHandler)
    }

    when:
    execHarness.run {
      processor.onStart(new DefaultEvent(registry, false))
      flowStatusRepository.create(FlowConfigSource.of(configData)).then { flowStatus ->
        processor.start(flowStatus).operation().then()
      }
    }

    and:
    latch.await(10, TimeUnit.SECONDS)

    then:
    0l == latch.count
  }

  void "should be able to access the FlowStatus from a work"() {
    setup:
    CountDownLatch latch = new CountDownLatch(1)
    Action<WorkChain> actChain = { chain -> chain
      .all { ctx ->
        FlowStatus status = ctx.get(FlowStatus)
        if (status) {
          latch.countDown()
        }
      }
    }
    FlowErrorHandler errorHandler = { r, f -> latch.countDown() }
    WorkStatusRepository workStatusRepository = new InMemoryWorkStatusRepository()
    FlowStatusRepository flowStatusRepository = new InMemoryFlowStatusRepository(workStatusRepository)
    def workChainConfig = new WorkChainConfig()
    workChainConfig.action = actChain
    DefaultWorkProcessor processor = new DefaultWorkProcessor()
    Registry registry = Registry.of() { r ->
      r.add(WorkStatusRepository, workStatusRepository)
      r.add(FlowStatusRepository, flowStatusRepository)
      r.add(WorkChainConfig, workChainConfig)
      r.add(FlowErrorHandler, errorHandler)
    }

    when:
    execHarness.run {
      processor.onStart(new DefaultEvent(registry, false))
      flowStatusRepository.create(FlowConfigSource.of(configData)).then { flowStatus ->
        processor.start(flowStatus).operation().then()
      }
    }

    and:
    latch.await(10, TimeUnit.SECONDS)

    then:
    0l == latch.count
  }

  void "should be able to decorate the chain"() {
    setup:
    CountDownLatch latch = new CountDownLatch(1)
    WorkChainDecorator decorator = { chain ->
      chain.all { ctx ->
        latch.countDown()
      }
    }
    WorkStatusRepository workStatusRepository = new InMemoryWorkStatusRepository()
    FlowStatusRepository flowStatusRepository = new InMemoryFlowStatusRepository(workStatusRepository)
    def workChainConfig = new WorkChainConfig()
    DefaultWorkProcessor processor = new DefaultWorkProcessor()
    Registry registry = Registry.of() { r ->
      r.add(WorkStatusRepository, workStatusRepository)
      r.add(FlowStatusRepository, flowStatusRepository)
      r.add(WorkChainConfig, workChainConfig)
      r.add(WorkChainDecorator, decorator)
    }

    when:
    execHarness.run {
      processor.onStart(new DefaultEvent(registry, false))
      flowStatusRepository.create(FlowConfigSource.of(configData)).then { flowStatus ->
        processor.start(flowStatus).operation().then()
      }
    }

    and:
    latch.await(10, TimeUnit.SECONDS)

    then:
    0l == latch.count
  }
}
