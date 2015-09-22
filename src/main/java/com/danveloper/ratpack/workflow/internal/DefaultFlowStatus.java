package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.FlowStatus;
import com.danveloper.ratpack.workflow.WorkState;
import com.danveloper.ratpack.workflow.WorkStatus;

import java.util.List;
import java.util.Map;

public class DefaultFlowStatus implements FlowStatus {
  private String id;
  private String name;
  private String description;
  private Long startTime;
  private Long endTime;
  private WorkState state;
  private Map<String, String> tags;
  private List<WorkStatus> works;

  void setId(String id) {
    this.id = id;
  }

  void setName(String name) {
    this.name = name;
  }

  void setDescription(String description) {
    this.description = description;
  }

  void setStartTime(Long time) {
    this.startTime = time;
  }

  void setEndTime(Long time) {
    this.endTime = time;
  }

  void setState(WorkState state) {
    this.state = state;
  }

  void setTags(Map<String, String> tags) {
    this.tags = tags;
  }

  void setWorks(List<WorkStatus> works) {
    this.works = works;
  }

  @Override
  public String getId() {
    return this.id;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public WorkState getState() {
    return this.state;
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
  public Map<String, String> getTags() {
    return this.tags;
  }

  @Override
  public List<WorkStatus> getWorks() {
    return this.works;
  }
}
