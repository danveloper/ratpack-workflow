package com.danveloper.ratpack.workflow.internal

import ratpack.registry.Registry
import spock.lang.Specification

class DefaultWorkChainSpec extends Specification {

  void "should compose a chain"() {
    given:
    DefaultWorkChain chain = new DefaultWorkChain(Registry.empty())
      .work("resize", "1.0") {}
      .work("autoscale") {}
      .all {}
      .flow("flow") { schain ->
        schain.work("foo") {}
      }
    TypedVersionedWork flowwork = chain.works.find { TypedVersionedWork w -> w.@type == "flow" }

    expect:
    4 == chain.works.size()
    ["resize", "autoscale", "", "flow"] == chain.works.collect { TypedVersionedWork w -> w.@type }
    flowwork.@delegate instanceof WorkChainWork
    1 == flowwork.@delegate.works.size()
    flowwork.@delegate.works[0].@type == "foo"
  }
}
