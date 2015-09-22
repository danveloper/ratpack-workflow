package com.danveloper.ratpack.workflow;

import java.util.List;
import java.util.Map;

public interface FlowStatus {
  String getId();
  String getName();
  String getDescription();
  WorkState getState();
  Long getStartTime();
  Long getEndTime();
  Map<String, String> getTags();
  List<WorkStatus> getWorks();
}
