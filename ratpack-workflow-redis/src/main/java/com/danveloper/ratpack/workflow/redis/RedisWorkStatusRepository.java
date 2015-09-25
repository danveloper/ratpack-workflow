package com.danveloper.ratpack.workflow.redis;

import com.danveloper.ratpack.workflow.WorkConfigSource;
import com.danveloper.ratpack.workflow.WorkState;
import com.danveloper.ratpack.workflow.WorkStatus;
import com.danveloper.ratpack.workflow.WorkStatusRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
      exec(jedis -> jedis.hset("work:all", workStatus.getId(), json(workStatus)))
    ).map(l -> workStatus);
  }

  @Override
  public Promise<WorkStatus> save(WorkStatus status) {
    String json = json(status);
    return Blocking.op(() ->
            exec(jedis -> {
              jedis.hset("work:all", status.getId(), json);

              if (status.getState() != WorkState.RUNNING) {
                jedis.hdel("work:running", status.getId());
              } else {
                jedis.hset("work:running", status.getId(), json);
              }
              return null;
            })
    ).flatMap(get(status.getId()));
  }

  @Override
  public Promise<List<WorkStatus>> list() {
    return Blocking.get(() ->
      exec(jedis -> jedis.hgetAll("work:all"))
    ).map(works ->
      works.values().stream().map(this::readWorkStatus).collect(Collectors.toList())
    );
  }

  @Override
  public Promise<List<WorkStatus>> listRunning() {
    return Blocking.get(() ->
      exec(jedis -> jedis.hgetAll("work:running"))
    ).map(works ->
      works.values().stream().map(this::readWorkStatus).collect(Collectors.toList())
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
        String key = "lock:"+id;
        if (jedis.exists(key)) {
          return false;
        } else {
          return Boolean.parseBoolean(jedis.getSet(key, "true"));
        }
      })
    );
  }

  @Override
  public Promise<Boolean> unlock(String id) {
    return Blocking.get(() ->
      exec(jedis -> {
        String key = "lock:"+id;
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
