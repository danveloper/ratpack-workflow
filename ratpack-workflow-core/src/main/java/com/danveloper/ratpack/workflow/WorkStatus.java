package com.danveloper.ratpack.workflow;

import com.danveloper.ratpack.workflow.internal.DefaultWorkStatus;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public interface WorkStatus {
  String getId();
  WorkConfigSource getConfig();
  Long getStartTime();
  Long getEndTime();
  WorkState getState();
  Throwable getError();
  List<WorkStatusMessage> getMessages();

  static WorkStatus of(String id, WorkConfigSource config) {
    return DefaultWorkStatus.of(id, config);
  }

  static WorkStatus of(WorkConfigSource config) {
    return DefaultWorkStatus.of(config);
  }

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

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      WorkStatusMessage that = (WorkStatusMessage) o;

      if (time != null ? !time.equals(that.time) : that.time != null) return false;
      return !(content != null ? !content.equals(that.content) : that.content != null);

    }

    @Override
    public int hashCode() {
      int result = time != null ? time.hashCode() : 0;
      result = 31 * result + (content != null ? content.hashCode() : 0);
      return result;
    }
  }
}
