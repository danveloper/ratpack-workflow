package com.danveloper.ratpack.workflow.internal;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil {
  public static String exceptionToString(Throwable e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }
}
