package com.danveloper.ratpack.workflow

import com.danveloper.ratpack.workflow.server.RatpackWorkflow
import ratpack.service.Service
import ratpack.service.StartEvent
import ratpack.test.embed.EmbeddedApp
import spock.lang.Issue
import spock.lang.Specification

import java.util.concurrent.atomic.LongAdder

class WorkflowServicesSpec extends Specification {

  @Issue("#7 - Services are started twice during app start")
  def "services should only initialize once"() {
    setup:
    def addr = new LongAdder()
    def app = EmbeddedApp.fromServer {
      RatpackWorkflow.of { spec -> spec
        .workflow { chain ->
          chain.all { ctx ->
            ctx.complete()
          }
        }
        .registryOf { r ->
          r.add(Service, new Service() {
            @Override
            void onStart(StartEvent event) throws Exception {
              addr.increment()
            }
          })
        }
        .handlers { chain ->
          chain.get { ctx -> ctx.render("Hello World")}
        }
      }
    }

    when:
    app.httpClient.get()

    then:
    addr.intValue() == 1

    cleanup:
    app.close()
  }
}
