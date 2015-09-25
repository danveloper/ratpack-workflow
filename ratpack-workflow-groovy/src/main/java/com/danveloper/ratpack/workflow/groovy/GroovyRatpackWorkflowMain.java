package com.danveloper.ratpack.workflow.groovy;

import com.danveloper.ratpack.workflow.server.GroovyRatpackWorkflow;
import com.danveloper.ratpack.workflow.server.RatpackWorkflow;

public class GroovyRatpackWorkflowMain {

  public static void main(String[] args) throws Exception {
    RatpackWorkflow.start(GroovyRatpackWorkflow.Script.app());
  }
}
