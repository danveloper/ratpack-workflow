package com.danveloper.ratpack.workflow.internal

import com.danveloper.ratpack.workflow.WorkChain
import ratpack.func.Action
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
    TypedVersionedWork flowwork = chain.works.find { TypedVersionedWork w -> w.getType() == "flow" }

    expect:
    4 == chain.works.size()
    ["resize", "autoscale", "", "flow"] == chain.works.collect { TypedVersionedWork w -> w.getType() }
    flowwork.getDelegate() instanceof WorkChainWork
    1 == flowwork.getDelegate().works.size()
    flowwork.getDelegate().works[0].getType() == "flow/foo"
  }

  void "should compose a chain from a subchain action"() {
    given:
    def act = { WorkChain chain ->
      chain.all { }
    } as Action<WorkChain>
    DefaultWorkChain chain = new DefaultWorkChain(Registry.empty()).insert(act)

    expect:
    1 == chain.works.size()
  }
}
