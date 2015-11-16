package com.danveloper.ratpack.workflow.internal

import com.danveloper.ratpack.workflow.*
import com.google.common.io.ByteSource
import ratpack.config.ConfigData
import ratpack.exec.Blocking
import ratpack.exec.Execution
import ratpack.exec.Operation
import ratpack.registry.Registry
import ratpack.test.exec.ExecHarness
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import static com.danveloper.ratpack.workflow.internal.ExceptionUtil.exceptionToString

class DefaultWorkContextSpec extends Specification {

  @AutoCleanup
  ExecHarness execHarness = ExecHarness.harness()

  ConfigData d = ConfigData.of { spec ->
    spec
        .json(
        ByteSource.wrap("""
        {
          "type": "api/resize",
          "version": "2.0",
          "data": {
            "foo": "bar"
          }
        }
        """.bytes))
        .build()
  }

  def config = new WorkConfigSource(d)

  void "should call a chain"() {
    setup:
    AtomicInteger adder = new AtomicInteger()
    ConfigData d = ConfigData.of { spec ->
      spec
          .props([type: "foo", version: "1.0", data: "1"])
          .build()
    }
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
        .work("foo", "1.0") { ctx ->
      adder.incrementAndGet()
    }

    when:
    ExecHarness.runSingle { exec ->
      DefaultWorkContext
          .start(chain.works as Work[], new WorkConfigSource(d), new InMemoryWorkStatusRepository(), Registry.empty())
          .operation()
          .then()
    }

    and:
    execHarness.controller.eventLoopGroup.awaitTermination(1, TimeUnit.SECONDS)

    then:
    1 == adder.get()
  }

  void "should properly match work handlers"() {
    setup:
    AtomicInteger badAdder = new AtomicInteger()
    AtomicInteger goodAdder = new AtomicInteger()
    ConfigData d = ConfigData.of { spec ->
      spec
          .props([type: "foo", version: "1.0", data: "1"])
          .build()
    }
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .work("bar", "1.0") { ctx ->
      badAdder.incrementAndGet()
    }
    .work("foo", "2.0") { ctx ->
      badAdder.incrementAndGet()
    }
    .work("foo", "1.0") { ctx ->
      goodAdder.incrementAndGet()
    }
    .work("bar", "1.1") { ctx ->
      badAdder.incrementAndGet()
    }

    when:
    ExecHarness.runSingle { exec ->
      DefaultWorkContext
          .start(chain.works as Work[], new WorkConfigSource(d), new InMemoryWorkStatusRepository(), Registry.empty())
          .operation()
          .then()
    }

    and:
    execHarness.controller.eventLoopGroup.awaitTermination(1, TimeUnit.SECONDS)

    then:
    0 == badAdder.get()
    1 == goodAdder.get()
  }

  void "should provide flow capabilities"() {
    setup:
    def badAdder = new AtomicInteger()
    def goodAdder = new AtomicInteger()

    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .flow("api", "1.0") { schain ->
      schain.work("resize") {
        badAdder.incrementAndGet()
      }
    }
    .flow("api", "2.0") { schain ->
      schain.work("resize") {
        goodAdder.incrementAndGet()
      }
    }
    .work("resize") {
      badAdder.incrementAndGet()
    }

    when:
    run(chain)

    then:
    0 == badAdder.get()
    1 == goodAdder.get()
  }

  void "all-type works should be called when appropriate"() {
    setup:
    AtomicInteger adder = new AtomicInteger()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all { ctx ->
      adder.incrementAndGet()
      ctx.next()
    }
    .flow("api", "2.0") { schain ->
      schain.all { ctx ->
        adder.incrementAndGet()
        ctx.next()
      }
      schain.work("resize") {
        adder.incrementAndGet()
      }
    }
    .all { ctx ->
      // should not be called, since we are outside of the above sub-chain
      adder.incrementAndGet()
    }

    when:
    run(chain)

    then:
    3 == adder.get()
  }

  void "errors should stop processing"() {
    setup:
    AtomicInteger adder = new AtomicInteger()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
        .all { ctx ->
      adder.incrementAndGet()
      throw new RuntimeException("failed")
    }
    .all { ctx ->
      // should not be called, since we are outside of the above sub-chain
      adder.incrementAndGet()
    }

    when:
    run(chain)

    then:
    1 == adder.get()
  }

  void "work status should be accessible from the execution"() {
    setup:
    AtomicInteger adder = new AtomicInteger()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
        .all { ctx ->
      def ws = Execution.current().get(WorkStatus)
      if (ws) {
        adder.incrementAndGet()
      } else {
        throw new RuntimeException()
      }
      ctx.complete()
    }

    when:
    run(chain)

    then:
    1 == adder.get()
  }

  void "failures should be properly reflected"() {
    setup:
    AtomicInteger adder = new AtomicInteger()
    WorkStatusRepository workStatusRepository = new InMemoryWorkStatusRepository()
    RuntimeException exception = new RuntimeException("!!")

    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all { ctx ->
      adder.incrementAndGet()
      throw exception
    }

    when:
    String id = ExecHarness.yieldSingle { exec ->
      DefaultWorkContext
          .start(chain.works as Work[], config, workStatusRepository, Registry.empty())
    }.valueOrThrow

    and:
    execHarness.controller.eventLoopGroup.awaitTermination(1, TimeUnit.SECONDS)

    then:
    1 == adder.get()

    when:
    def works = execHarness.yield {
      workStatusRepository.get(id)
    }.valueOrThrow

    then:
    works.state == WorkState.FAILED
    works.error == exceptionToString(exception)
  }

  void "work should be able to retry itself"() {
    setup:
    AtomicInteger adder = new AtomicInteger()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all { ctx ->
      def num = adder.incrementAndGet()
      if (num < 2) {
        ctx.retry()
      } else {
        ctx.complete()
      }
    }

    when:
    run(chain)

    then:
    2 == adder.get()
  }

  void "should be able to retrieve objects from the context registry"() {
    setup:
    def repo = new InMemoryWorkStatusRepository()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all { ctx ->
      def status = ctx.get(WorkStatus)
      if (status.state != WorkState.RUNNING) {
        throw new RuntimeException("!!")
      } else {
        ctx.complete()
      }
    }

    when:
    run(chain, repo)

    and:
    def page = ExecHarness.yieldSingle {
      repo.list(0, 10)
    }.valueOrThrow

    and:
    def works = page.objs

    then:
    1 == works.size()
    works[0].error == null
  }

  void "should be able to store objects in the context registry"() {
    setup:
    def repo = new InMemoryWorkStatusRepository()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all { ctx ->
      ctx.next(Registry.single(new TestObject(foo: "bar")))
    }
    .all { ctx ->
      Optional<TestObject> obj = ctx.maybeGet(TestObject)
      if (!obj.present) {
        throw new RuntimeException("!!")
      } else {
        ctx.complete()
      }
    }

    when:
    run(chain, repo)

    and:
    def page = ExecHarness.yieldSingle {
      repo.list(0, 10)
    }.valueOrThrow

    and:
    def works = page.objs

    then:
    1 == works.size()
    works[0].error == null
  }

  void "work status should be properly reflected"() {
    setup:
    def repo = new InMemoryWorkStatusRepository()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all { ctx ->
      ctx.complete()
    }

    when:
    run(chain, repo)

    and:
    def page = ExecHarness.yieldSingle {
      repo.list(0, 10)
    }.valueOrThrow

    and:
    def works = page.objs

    then:
    1 == works.size()
    works[0].error == null
    works[0].endTime != null
    works[0].state == WorkState.COMPLETED
  }

  void "work should be able to make the config data onto objects"() {
    setup:
    def repo = new InMemoryWorkStatusRepository()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all { ctx ->
      TestObject obj = ctx.config.mapData(TestObject)
      ctx.next(Registry.single(obj))
    }
    .all { ctx ->
      def obj = ctx.maybeGet(TestObject)
      if (!obj.present || obj.get().foo != "bar") {
        throw new RuntimeException()
      } else {
        ctx.complete()
      }
    }

    when:
    run(chain, repo)

    and:
    def page = ExecHarness.yieldSingle {
      repo.list(0, 10)
    }.valueOrThrow

    and:
    def works = page.objs

    then:
    1 == works.size()
    works[0].error == null
    works[0].endTime != null
    works[0].state == WorkState.COMPLETED
  }

  void "work operations should wait for all async operations to return before continuing"() {
    setup:
    def times = []
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all { ctx ->
      times << System.currentTimeMillis()
      Blocking.get {
        Thread.sleep(500)
      }.then {
        ctx.next()
      }
    }
    .all {
      times << System.currentTimeMillis()
    }

    when:
    execHarness.run { exec ->
      DefaultWorkContext
          .start(chain.works as Work[], config, new InMemoryWorkStatusRepository(), Registry.empty())
          .operation()
          .then()
    }

    and:
    execHarness.controller.eventLoopGroup.awaitTermination(2, TimeUnit.SECONDS)

    then:
    times[1] - times[0] >= 500
  }

  void "should be able to insert work in the chain"() {
    setup:
    AtomicInteger adder = new AtomicInteger()
    def extraWork = { ctx -> adder.incrementAndGet() } as Work
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all { ctx ->
      adder.incrementAndGet()
      ctx.insert(extraWork)
    }

    when:
    run(chain)

    then:
    2 == adder.get()
  }

  void "work should fail if no work handler exists"() {
    setup:
    InMemoryWorkStatusRepository repo = new InMemoryWorkStatusRepository()
    AtomicInteger adder = new AtomicInteger()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .work("foo") {
      adder.incrementAndGet()
    }

    when:
    run(chain, repo)

    then:
    0 == adder.get()

    when:
    def page = execHarness.yield {
      repo.list(0, 10)
    }.valueOrThrow

    and:
    def works = page.objs

    then:
    1 == works.size()
    works[0].state == WorkState.FAILED
  }

  void "should detect dangling executions"() {
    setup:
    InMemoryWorkStatusRepository repo = new InMemoryWorkStatusRepository()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all {
      // do nothing
    }

    when:
    run(chain, repo)

    and:
    def page = execHarness.yield {
      repo.list(0, 10)
    }.valueOrThrow

    and:
    def works = page.objs

    then:
    1 == works.size()
    works[0].state == WorkState.FAILED
  }

  void "messages should be saved with the workstatus"() {
    setup:
    def message = "msg"
    InMemoryWorkStatusRepository repo = new InMemoryWorkStatusRepository()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all { ctx ->
      ctx.message(message)
      ctx.complete()
    }

    when:
    run(chain, repo)

    and:
    def page = execHarness.yield {
      repo.list(0, 10)
    }.valueOrThrow

    and:
    def works = page.objs

    then:
    1 == works.size()
    1 == works[0].messages.size()
    works[0].messages[0].content == message
  }

  private void run(DefaultWorkChain chain, WorkStatusRepository repository=new InMemoryWorkStatusRepository(), Registry registry=Registry.empty()) {
    ExecHarness.runSingle { exec ->
      DefaultWorkContext
          .start(chain.works as Work[], config, repository, registry)
          .operation()
          .then()
    }
    execHarness.controller.eventLoopGroup.awaitTermination(1, TimeUnit.SECONDS)
  }

  void "insert with registry should propagate components down the chain"() {
    setup:
    def testObject = new TestObject(foo: "bar")
    def latch = new CountDownLatch(1)
    def nextWork = { ctx ->
      TestObject obj = ctx.get(TestObject)
      if (obj.foo == "bar") {
        latch.countDown()
        ctx.complete()
      }
    } as Work
    InMemoryWorkStatusRepository repo = new InMemoryWorkStatusRepository()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all { ctx ->
      ctx.insert(Registry.single(testObject), nextWork)
    }

    when:
    run(chain, repo)

    and:
    latch.await(5, TimeUnit.SECONDS)

    then:
    0l == latch.count
  }

  void "should call WorkCompletionHandler when work is finished"() {
    setup:
    def latch = new CountDownLatch(1)
    def completionInterceptor = { Operation.of { latch.countDown() } } as WorkCompletionHandler
    InMemoryWorkStatusRepository repo = new InMemoryWorkStatusRepository()
    DefaultWorkChain chain = (DefaultWorkChain) new DefaultWorkChain(Registry.empty())
    .all { ctx ->
      ctx.complete()
    }

    when:
    run(chain, repo, Registry.single(WorkCompletionHandler, completionInterceptor))

    and:
    latch.await(10, TimeUnit.SECONDS)

    then:
    0l == latch.count
  }
}
