package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.MutableWorkStatus;
import com.danveloper.ratpack.workflow.WorkConfigSource;
import com.danveloper.ratpack.workflow.WorkState;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@JsonDeserialize(using = DefaultWorkStatusDeserializer.class)
public class DefaultWorkStatus implements MutableWorkStatus {
  private String id;
  private WorkConfigSource config;
  private Long startTime;
  private Long endTime;
  private WorkState state;
  private Throwable error;
  private List<WorkStatusMessage> messages;

  DefaultWorkStatus() {
    this.messages = Lists.newArrayList();
  }

  public static DefaultWorkStatus of(WorkConfigSource config) {
    String id = new UUID(new Random().nextLong(), new Random().nextLong()).toString();
    return of(id, config);
  }

  public static DefaultWorkStatus of(String id, WorkConfigSource config) {
    DefaultWorkStatus status = new DefaultWorkStatus();
    status.setId(id);
    status.setConfig(config);
    status.setState(WorkState.NOT_STARTED);
    status.setMessages(Lists.newArrayList());

    return status;
  }

  void setId(String id) {
    this.id = id;
  }

  void setConfig(WorkConfigSource config) {
    this.config = config;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  public void setState(WorkState state) {
    this.state = state;
  }

  void setError(Throwable error) {
    this.error = error;
  }

  void setMessages(List<WorkStatusMessage> messages) {
    this.messages = messages;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public WorkConfigSource getConfig() {
    return this.config;
  }

  @Override
  public Long getStartTime() {
    return this.startTime;
  }

  @Override
  public Long getEndTime() {
    return this.endTime;
  }

  @Override
  public WorkState getState() {
    return this.state;
  }

  @Override
  public Throwable getError() {
    return this.error;
  }

  @Override
  public List<WorkStatusMessage> getMessages() {
    return this.messages;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DefaultWorkStatus that = (DefaultWorkStatus) o;

    if (id != null ? !id.equals(that.id) : that.id != null) return false;
    if (config != null ? !config.equals(that.config) : that.config != null) return false;
    if (startTime != null ? !startTime.equals(that.startTime) : that.startTime != null) return false;
    if (endTime != null ? !endTime.equals(that.endTime) : that.endTime != null) return false;
    if (state != that.state) return false;
    if (error != null ? !error.equals(that.error) : that.error != null) return false;
    return !(messages != null ? !messages.equals(that.messages) : that.messages != null);

  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (config != null ? config.hashCode() : 0);
    result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
    result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
    result = 31 * result + (state != null ? state.hashCode() : 0);
    result = 31 * result + (error != null ? error.hashCode() : 0);
    result = 31 * result + (messages != null ? messages.hashCode() : 0);
    return result;
  }
}
