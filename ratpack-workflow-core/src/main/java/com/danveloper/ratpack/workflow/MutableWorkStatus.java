package com.danveloper.ratpack.workflow;

public interface MutableWorkStatus extends WorkStatus {

  void setState(WorkState state);
  void setStartTime(Long startTime);
  void setEndTime(Long endTime);
}
