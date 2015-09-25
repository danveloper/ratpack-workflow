package com.danveloper.ratpack.workflow.redis;

import com.danveloper.ratpack.workflow.FlowStatus;
import com.danveloper.ratpack.workflow.WorkStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import ratpack.func.Function;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

abstract class RedisRepositorySupport {
  protected JedisPool jedisPool;
  protected ObjectMapper mapper;

  public RedisRepositorySupport() {
    this(new ObjectMapper());
  }

  public RedisRepositorySupport(ObjectMapper mapper) {
    this(new JedisPool(), mapper);
  }

  public RedisRepositorySupport(JedisPool jedisPool) {
    this(jedisPool, new ObjectMapper());
  }

  public RedisRepositorySupport(JedisPool jedisPool, ObjectMapper mapper) {
    this.jedisPool = jedisPool;
    this.mapper = mapper;
  }

  protected <T> T exec(Function<Jedis, T> function) {
    try (Jedis jedis = jedisPool.getResource()) {
      return function.apply(jedis);
    } catch (Exception e) {
      throw new RuntimeException("Error processing Jedis function!", e);
    }
  }

  protected String json(Object object) {
    try {
      return mapper.writeValueAsString(object);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected WorkStatus readWorkStatus(String json) {
    try {
      return mapper.readValue(json, WorkStatus.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected FlowStatus readFlowStatus(String json) {
    try {
      return mapper.readValue(json, FlowStatus.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
