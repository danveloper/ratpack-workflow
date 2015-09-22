package com.danveloper.ratpack.workflow;

import ratpack.config.ConfigData;

public class WorkConfigSource {

  private final ConfigData configData;
  private final TypeVersion typeVersion;

  public WorkConfigSource(ConfigData configData) {
    this.configData = configData;
    this.typeVersion = configData.get(TypeVersion.class);
  }

  public String getVersion() {
    return this.typeVersion.version;
  }

  public String getType() {
    return this.typeVersion.type;
  }

  public <O> O get(String path, Class<O> type) {
    return configData.get(path, type);
  }

  public <O> O mapData(Class<O> type) {
    return get("/data", type);
  }

  static class TypeVersion {
    String type;
    String version;

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }
  }
}
