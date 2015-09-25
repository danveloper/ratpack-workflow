package com.danveloper.ratpack.workflow.groovy.internal

import com.danveloper.ratpack.workflow.server.GroovyRatpackWorkflow
import ratpack.test.embed.EmbeddedApp

abstract class GroovyRatpackWorkflowScriptAppSpec extends EmbeddedAppSpec {
  @Delegate
  EmbeddedApp application

  private File appFile

  def setup() {
    application = createApplication()
  }

  abstract EmbeddedApp createApplication()

  File getRatpackFile() {
    return getApplicationFile("ratpack.groovy")
  }

  void script(String text) {
    ratpackFile.text = "import static ${GroovyRatpackWorkflow.name}.ratpack\n\n$text"
  }

  protected getApplicationFile(String fileName) {
    if (!appFile) {
      appFile = temporaryFolder.newFile(fileName)
    }
    return appFile
  }
}
