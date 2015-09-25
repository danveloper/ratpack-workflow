package com.danveloper.ratpack.workflow.groovy

import com.danveloper.ratpack.workflow.groovy.internal.GroovyRatpackWorkflowScriptAppSpec
import com.danveloper.ratpack.workflow.server.GroovyRatpackWorkflow
import com.danveloper.ratpack.workflow.server.RatpackWorkflow
import ratpack.groovy.Groovy
import ratpack.server.RatpackServer
import ratpack.server.internal.ServerCapturer
import ratpack.test.embed.EmbeddedApp
import ratpack.test.embed.internal.EmbeddedAppSupport

class RatpackWorkflowGroovyScriptAppSpec extends GroovyRatpackWorkflowScriptAppSpec {
  boolean compileStatic = false
  boolean development = false

  @Override
  EmbeddedApp createApplication() {
    new EmbeddedAppSupport() {
      @Override
      protected RatpackServer createServer() {
        ServerCapturer.capture(new ServerCapturer.Overrides().port(0)) {
          RatpackWorkflow.of(GroovyRatpackWorkflow.Script.app(compileStatic, ratpackFile.canonicalFile.toPath()))
        }
      }
    }
  }

  def "can use script app"() {
    given:
    compileStatic = true

    when:
    script """
      import com.danveloper.ratpack.workflow.WorkProcessor

      ratpack {
        workflow {
          all {
            complete()
          }
        }
        handlers {
          get { WorkProcessor processor ->
            render processor.toString()
          }
        }
      }
    """

    and:
    def resp = get()

    then:
    resp.statusCode == 200
    resp.body.text != ""
    resp.body.text.startsWith("com.danveloper.ratpack.workflow.")
  }
}
