package com.danveloper.ratpack.workflow;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Page<T> {

  private final Integer offset;
  private final Integer limit;
  private final Integer numPages;
  private final List<T> objs;

  @JsonCreator
  public Page(@JsonProperty("offset") Integer offset, @JsonProperty("limit") Integer limit,
              @JsonProperty("numPages") Integer numPages, @JsonProperty("objs") List<T> objs) {
    this.offset = offset;
    this.limit = limit;
    this.numPages = numPages;
    this.objs = objs;
  }

  public Integer getOffset() {
    return offset;
  }

  public Integer getLimit() {
    return limit;
  }

  public Integer getNumPages() {
    return numPages;
  }

  public List<T> getObjs() {
    return objs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Page<?> page = (Page<?>) o;

    if (offset != null ? !offset.equals(page.offset) : page.offset != null) return false;
    if (limit != null ? !limit.equals(page.limit) : page.limit != null) return false;
    if (numPages != null ? !numPages.equals(page.numPages) : page.numPages != null) return false;
    return !(objs != null ? !objs.equals(page.objs) : page.objs != null);

  }

  @Override
  public int hashCode() {
    int result = offset != null ? offset.hashCode() : 0;
    result = 31 * result + (limit != null ? limit.hashCode() : 0);
    result = 31 * result + (numPages != null ? numPages.hashCode() : 0);
    result = 31 * result + (objs != null ? objs.hashCode() : 0);
    return result;
  }
}
