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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DefaultFlowStatus that = (DefaultFlowStatus) o;

    if (id != null ? !id.equals(that.id) : that.id != null) return false;
    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (description != null ? !description.equals(that.description) : that.description != null) return false;
    if (startTime != null ? !startTime.equals(that.startTime) : that.startTime != null) return false;
    if (endTime != null ? !endTime.equals(that.endTime) : that.endTime != null) return false;
    if (state != that.state) return false;
    if (tags != null ? !tags.equals(that.tags) : that.tags != null) return false;
    return !(works != null ? !works.equals(that.works) : that.works != null);

  }

  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (description != null ? description.hashCode() : 0);
    result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
    result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
    result = 31 * result + (state != null ? state.hashCode() : 0);
    result = 31 * result + (tags != null ? tags.hashCode() : 0);
    result = 31 * result + (works != null ? works.hashCode() : 0);
    return result;
  }
}
