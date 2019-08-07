package com.danveloper.ratpack.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import ratpack.config.ConfigData;
import ratpack.util.Types;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlowConfigSource {

  private final String name;
  private final String description;
  private final Map<String, String> tags;
  private final List<WorkConfigSource> works;

  public FlowConfigSource(String name, String description, Map<String, String> tags, List<WorkConfigSource> works) {
    this.name = name;
    this.description = description;
    this.tags = tags;
    this.works = works;
  }

  public static FlowConfigSource of(ConfigData configData) throws Exception {
    String name = configData.getRootNode().get("name").asText();
    String description = configData.getRootNode().get("description").asText();
    Map<String, String> tags = Types.cast(configData.get("/tags", LinkedHashMap.class));
    List<WorkConfigSource> works = Lists.newArrayList();
    for (JsonNode node : configData.getRootNode().get("works")) {
      String json = node.toString();
      try {
        ConfigData workConfig = ConfigData.of(d -> d.json(ByteSource.wrap(json.getBytes())).build());
        WorkConfigSource workConfigSource = WorkConfigSource.of(workConfig);
        works.add(workConfigSource);
      } catch (Exception IGNORE) {
        throw new RuntimeException("error converting work config source");
      }
    }
    return new FlowConfigSource(name, description, tags, works);
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  public List<WorkConfigSource> getWorks() {
    return works;
  }
}
