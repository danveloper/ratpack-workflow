package com.danveloper.ratpack.workflow.internal;

import com.danveloper.ratpack.workflow.WorkConfigSource;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Map;

public class WorkConfigSourceSerializer extends JsonSerializer<WorkConfigSource> {
  @Override
  public void serialize(WorkConfigSource value, JsonGenerator gen, SerializerProvider serializers) throws IOException, JsonProcessingException {
    gen.getCodec().writeValue(gen, value.get("", Map.class));
  }
}
