package com.danveloper.ratpack.workflow.redis;

import com.danveloper.ratpack.workflow.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import ratpack.exec.Blocking;
import ratpack.exec.Promise;
import ratpack.func.Function;
import redis.clients.jedis.JedisPool;
import rx.Observable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ratpack.rx.RxRatpack.observe;
import static ratpack.rx.RxRatpack.promise;

public class RedisFlowStatusRepository extends RedisRepositorySupport implements FlowStatusRepository {
  Function<Map<String, String>, List<FlowStatus>> statusMapper = flows ->
      flows.values().stream().map(this::readFlowStatus).collect(Collectors.toList());
  private final WorkStatusRepository workStatusRepository;

  public RedisFlowStatusRepository(WorkStatusRepository workStatusRepository) {
    this.workStatusRepository = workStatusRepository;
  }

  public RedisFlowStatusRepository(ObjectMapper mapper, WorkStatusRepository workStatusRepository) {
    super(mapper);
    this.workStatusRepository = workStatusRepository;
  }

  public RedisFlowStatusRepository(JedisPool jedisPool, WorkStatusRepository workStatusRepository) {
    super(jedisPool);
    this.workStatusRepository = workStatusRepository;
  }

  public RedisFlowStatusRepository(JedisPool jedisPool, ObjectMapper mapper, WorkStatusRepository workStatusRepository) {
    super(jedisPool, mapper);
    this.workStatusRepository = workStatusRepository;
  }

  @Override
  public Promise<FlowStatus> create(FlowConfigSource config) {
    MutableFlowStatus flowStatus = FlowStatus.of(config).toMutable();

    Observable<WorkStatus> workStatusesObs = Observable.from(config.getWorks())
        .flatMap(workConfig -> observe(workStatusRepository.create(workConfig)));

    return promise(workStatusesObs).map(works -> {
      flowStatus.setWorks(works);
      return flowStatus;
    }).flatMap(this::save).flatMap(status ->
      Blocking.get(() ->
        exec(jedis -> {
          status.getTags().forEach((key, value) ->
                  jedis.sadd("tags:" + key + ":" + value, status.getId())
          );
          return null;
        })
      ).map(l -> status)
    );
  }

  @Override
  public Promise<FlowStatus> save(FlowStatus status) {
    String json = json(status);

    return Blocking.get(() ->
            exec(jedis -> {
              jedis.hset("flow:all", status.getId(), json);

              if (status.getState() != WorkState.RUNNING) {
                jedis.hdel("flow:running", status.getId());
              } else {
                jedis.hset("flow:running", status.getId(), json);
              }
              return null;
            })
    ).flatMap(l -> get(status.getId()));
  }

  @Override
  public Promise<List<FlowStatus>> list() {
    return Blocking.get(() ->
      exec(jedis ->
        jedis.hgetAll("flow:all")
      )
    ).map(statusMapper);
  }

  @Override
  public Promise<FlowStatus> get(String id) {
    return Blocking.get(() ->
      exec(jedis ->
        jedis.hget("flow:all", id)
      )
    ).map(this::readFlowStatus);
  }

  @Override
  public Promise<List<FlowStatus>> listRunning() {
    return Blocking.get(() ->
      exec(jedis ->
        jedis.hgetAll("flow:running")
      )
    ).map(statusMapper);
  }

  @Override
  public Promise<List<FlowStatus>> findByTag(String key, String value) {
    return Blocking.get(() ->
            exec(jedis -> {
              Set<String> ids = jedis.smembers("tags:" + key + ":" + value);
              return ids.stream().map(id -> jedis.hget("flow:all", id))
                  .map(this::readFlowStatus).collect(Collectors.toList());
            })
    );
  }

}
