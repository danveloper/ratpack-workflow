package com.danveloper.ratpack.workflow;

import java.util.List;

public interface MutableFlowStatus extends FlowStatus {
  void setStartTime(Long startTime);
  void setEndTime(Long endTime);
  void setState(WorkState state);
  void setWorks(List<WorkStatus> works);
}
