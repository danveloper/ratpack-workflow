package com.danveloper.ratpack.workflow.groovy

import com.danveloper.ratpack.workflow.FlowStatusRepository
import com.danveloper.ratpack.workflow.WorkConfigSource
import com.danveloper.ratpack.workflow.WorkStatus
import com.danveloper.ratpack.workflow.WorkStatusRepository
import ratpack.error.internal.DefaultDevelopmentErrorHandler
import ratpack.exec.Promise
import spock.lang.AutoCleanup
import spock.lang.Specification

class FunctionalGroovyDSLSpec extends Specification {

  @Delegate
  @AutoCleanup
  GroovyRatpackWorkflowEmbeddedApp app = GroovyRatpackWorkflowEmbeddedApp.of {
    workRepo(new StubWorkRepo())
    flowRepo { w -> new StubFlowRepo() }

    handlers {
      get {
        render get(WorkStatusRepository).class.simpleName
      }
      get("f") {
        render get(FlowStatusRepository).class.simpleName
      }
    }
  }

  void "should favor specified WorkStatusRepository and FlowStatusRepository"() {
    when:
    def resp = httpClient.get()

    then:
    resp.statusCode == 200
    "StubWorkRepo" == resp.body.text

    when:
    resp = httpClient.get("f")

    then:
    resp.statusCode == 200
    "StubFlowRepo" == resp.body.text
  }
}
