package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.WorkConfigSource;
import com.danveloper.ratpack.workflow.WorkState;
import com.danveloper.ratpack.workflow.WorkStatus;
import com.danveloper.ratpack.workflow.internal.DefaultWorkStatus;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.io.ByteSource;
import ratpack.config.ConfigData;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultWorkStatusDeserializer extends JsonDeserializer<DefaultWorkStatus> {
  @Override
  public DefaultWorkStatus deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    JsonNode node = p.getCodec().readTree(p);
    String id = node.get("id").isNull() ? null : node.get("id").textValue();
    Long startTime = node.get("startTime").isNull() ? null : node.get("startTime").asLong();
    Long endTime = node.get("endTime").isNull() ? null : node.get("endTime").asLong();
    WorkState state = node.get("state").isNull() ? null : WorkState.valueOf(node.get("state").asText());
    List<WorkStatus.WorkStatusMessage> messages = Lists.newArrayList(node.get("messages").iterator()).stream().map(n -> {
      Long time = n.get("time").longValue();
      String content = n.get("content").textValue();
      return new WorkStatus.WorkStatusMessage(time, content);
    }).collect(Collectors.toList());

    String config = node.get("config").isNull() ? null : node.get("config").deepCopy().toString();
    ConfigData data;
    try {
      data = config == null ? null : ConfigData.of(d -> d.json(ByteSource.wrap(config.getBytes())).build());
    } catch (Exception e) {
      throw new RuntimeException("Unable to deserialize WorkConfigData", e);
    }
    DefaultWorkStatus status = new DefaultWorkStatus();
    status.setId(id);
    status.setStartTime(startTime);
    status.setEndTime(endTime);
    status.setState(state);
    status.setMessages(messages);
    status.setConfig(data != null ? WorkConfigSource.of(data) : null);

    return status;
  }
}
