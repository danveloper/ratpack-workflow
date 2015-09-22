package com.danveloper.ratpack.workflow;

import java.util.List;

public interface WorkStatus {
  String getId();
  WorkConfigSource getConfig();
  Long getStartTime();
  Long getEndTime();
  WorkState getState();
  Throwable getError();
  List<WorkStatusMessage> getMessages();

  class WorkStatusMessage {
    private Long time;
    private String content;

    public WorkStatusMessage(Long time, String content) {
      this.time = time;
      this.content = content;
    }

    public void setTime(Long time) {
      this.time = time;
    }

    public void setContent(String content) {
      this.content = content;
    }

    public Long getTime() {
      return time;
    }

    public String getContent() {
      return content;
    }
  }
}
