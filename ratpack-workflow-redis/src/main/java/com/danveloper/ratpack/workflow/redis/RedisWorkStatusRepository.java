package com.danveloper.ratpack.workflow.redis;

import com.danveloper.ratpack.workflow.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import ratpack.exec.Blocking;
import ratpack.exec.Promise;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.stream.Collectors;

public class RedisWorkStatusRepository extends RedisRepositorySupport implements WorkStatusRepository {
  public RedisWorkStatusRepository() {
  }

  public RedisWorkStatusRepository(ObjectMapper mapper) {
    super(mapper);
  }

  public RedisWorkStatusRepository(JedisPool jedisPool) {
    super(jedisPool);
  }

  public RedisWorkStatusRepository(JedisPool jedisPool, ObjectMapper mapper) {
    super(jedisPool, mapper);
  }

  @Override
  public Promise<WorkStatus> create(WorkConfigSource source) {
    WorkStatus workStatus = WorkStatus.of(source);
    return Blocking.get(() ->
            exec(jedis -> {
              jedis.lpush("work:keys", workStatus.getId());
              return jedis.hset("work:all", workStatus.getId(), json(workStatus));
            })
    ).map(l -> workStatus);
  }

  @Override
  public Promise<WorkStatus> save(WorkStatus status) {
    String json = json(status);
    return Blocking.op(() ->
            exec(jedis -> {
              jedis.hset("work:all", status.getId(), json);
              if (status.getState() != WorkState.RUNNING) {
                jedis.lrem("work:running", 0, status.getId());
              } else {
                jedis.lpush("work:running", status.getId());
              }
              return null;
            })
    ).flatMap(get(status.getId()));
  }

  @Override
  public Promise<Page<WorkStatus>> list(Integer offset, Integer limit) {
    return list0(offset, limit, "work:keys");
  }

  @Override
  public Promise<Page<WorkStatus>> listRunning(Integer offset, Integer limit) {
    return list0(offset, limit, "work:running");
  }

  private Promise<Page<WorkStatus>> list0(Integer offset, Integer limit, String key) {
    long correctedLimit = limit - 1;
    long startIdx = offset * correctedLimit + (offset > 0 ? 1 : 0);
    long endIdx = startIdx + correctedLimit;
    return Blocking.get(() ->
            exec(jedis -> {
              long maxRecords = jedis.llen(key);
              List<String> ids = jedis.lrange(key, startIdx, endIdx);
              List<WorkStatus> workStatuses = Lists.newArrayList();
              if (ids.size() > 0) {
                workStatuses = jedis.hmget("work:all", ids.toArray(new String[ids.size()]))
                    .stream().map(this::readWorkStatus).collect(Collectors.toList());
              }
              return new Page<>(offset, limit, (int) Math.max(1, Math.ceil(maxRecords / limit)), workStatuses);
            })
    );
  }

  @Override
  public Promise<WorkStatus> get(String id) {
    return Blocking.get(() ->
            exec(jedis -> jedis.hget("work:all", id))
    ).map(this::readWorkStatus);
  }

  @Override
  public Promise<Boolean> lock(String id) {
    return Blocking.get(() ->
            exec(jedis -> {
              String key = "lock:" + id;
              if (jedis.exists(key)) {
                return false;
              } else {
                String val = jedis.getSet(key, "true");
                return val == null ? Boolean.TRUE : Boolean.parseBoolean(val);
              }
            })
    );
  }

  @Override
  public Promise<Boolean> unlock(String id) {
    return Blocking.get(() ->
            exec(jedis -> {
              String key = "lock:" + id;
              if (jedis.exists(key)) {
                jedis.del(key);
                return !jedis.exists(key);
              } else {
                return true;
              }
            })
    );
  }

}
