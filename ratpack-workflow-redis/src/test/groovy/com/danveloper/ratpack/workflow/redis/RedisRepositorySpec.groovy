package com.danveloper.ratpack.workflow.redis

import ratpack.test.exec.ExecHarness
import redis.embedded.RedisServer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import static com.danveloper.ratpack.workflow.redis.PortFinder.nextFree

abstract class RedisRepositorySpec extends Specification {

  static int port = nextFree()

  @AutoCleanup("stop")
  @Shared
  RedisServer server = new RedisServer(port)

  @AutoCleanup
  ExecHarness execControl = ExecHarness.harness()

  def setupSpec() {
    server.start()
  }

}
