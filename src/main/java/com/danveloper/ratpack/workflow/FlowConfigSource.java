package com.danveloper.ratpack.workflow;

import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import ratpack.config.ConfigData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

  public static FlowConfigSource of(ConfigData configData) throws Exception{
    String name = configData.getRootNode().get("name").asText();
    String description = configData.getRootNode().get("description").asText();
    Map<String, String> tags = configData.get("/tags", LinkedHashMap.class);
    List<WorkConfigSource> works = Lists.newArrayList(configData.getRootNode().get("works").iterator())
        .stream()
        .map(Object::toString)
        .map(json -> {
          try {
            return ConfigData.of(d -> d.json(ByteSource.wrap(json.getBytes())).build());
          } catch (Exception e) {
            return null;
          }
        })
        .filter(d -> d != null)
        .map(WorkConfigSource::of)
        .collect(Collectors.toList());
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
