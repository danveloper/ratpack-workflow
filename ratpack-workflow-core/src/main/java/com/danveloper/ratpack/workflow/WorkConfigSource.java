package com.danveloper.ratpack.workflow;

import com.danveloper.ratpack.workflow.internal.WorkConfigSourceSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import ratpack.config.ConfigData;

@JsonSerialize(using = WorkConfigSourceSerializer.class)
public class WorkConfigSource {

  private final ConfigData configData;
  private final TypeVersion typeVersion;

  public WorkConfigSource(ConfigData configData) {
    this.configData = configData;
    this.typeVersion = configData.get(TypeVersion.class);
  }

  public static WorkConfigSource of(ConfigData configData) {
    return new WorkConfigSource(configData);
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

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TypeVersion that = (TypeVersion) o;

      if (type != null ? !type.equals(that.type) : that.type != null) return false;
      return !(version != null ? !version.equals(that.version) : that.version != null);

    }

    @Override
    public int hashCode() {
      int result = type != null ? type.hashCode() : 0;
      result = 31 * result + (version != null ? version.hashCode() : 0);
      return result;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    WorkConfigSource that = (WorkConfigSource) o;

    if (configData.getRootNode() != null ? !configData.getRootNode().equals(that.configData.getRootNode()) : that.configData.getRootNode() != null) return false;
    return !(typeVersion != null ? !typeVersion.equals(that.typeVersion) : that.typeVersion != null);

  }

  @Override
  public int hashCode() {
    int result = configData.getRootNode() != null ? configData.getRootNode().hashCode() : 0;
    result = 31 * result + (typeVersion != null ? typeVersion.hashCode() : 0);
    return result;
  }
}
