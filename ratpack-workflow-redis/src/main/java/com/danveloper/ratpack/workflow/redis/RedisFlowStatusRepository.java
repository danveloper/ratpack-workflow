package com.danveloper.ratpack.workflow.redis;

import com.danveloper.ratpack.workflow.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import ratpack.exec.Blocking;
import ratpack.exec.Promise;
import redis.clients.jedis.JedisPool;
import rx.Observable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ratpack.rx.RxRatpack.observe;
import static ratpack.rx.RxRatpack.promise;

public class RedisFlowStatusRepository extends RedisRepositorySupport implements FlowStatusRepository {
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
                      jedis.lpush("flow:keys", status.getId());
                      status.getTags().forEach((key, value) ->
                              jedis.lpush("tags:" + key + ":" + value, status.getId())
                      );
                      return null;
                    })
            ).map(l -> status)
    );
  }

  @Override
  public Promise<FlowStatus> save(FlowStatus status) {
    String json = json(status);

    return get(status.getId()).flatMap(existing ->
            Blocking.get(() ->
                    exec(jedis -> {
                      jedis.hset("flow:all", status.getId(), json);
                      List<String> existingWorkIds = jedis.lrange("flow:works:" + status.getId(), 0, 1000);
                      status.getWorks().forEach(workStatus -> {
                        if (!existingWorkIds.contains(workStatus.getId())) {
                          jedis.lpush("flow:works:" + status.getId(), workStatus.getId());
                        }
                      });
                      if (status.getState() != WorkState.RUNNING) {
                        jedis.lrem("flow:running", 0, status.getId());
                      } else if (existing != null && existing.getState() != status.getState()) {
                        jedis.lpush("flow:running", status.getId());
                      }
                      if (existing != null) {
                        status.getTags().entrySet().forEach(e -> {
                          if (!existing.getTags().containsKey(e.getKey())) {
                            jedis.lpush("tags:" + e.getKey() + ":" + e.getValue(), status.getId());
                          }
                        });
                      }
                      return null;
                    })
            )
    ).flatMap(l -> get(status.getId()));
  }

  @Override
  public Promise<FlowStatus> get(String id) {
    return Blocking.get(() ->
            exec(jedis ->
                    jedis.hget("flow:all", id)
            )
    ).map(json -> {
      if (json != null) {
        return readFlowStatus(json);
      } else {
        return null;
      }
    }).flatMap(status -> {
      if (status != null) {
        return Blocking.get(() -> blockingHydrateWorkStatuses(status));
      } else {
        return Promise.value(null);
      }
    });
  }

  @Override
  public Promise<Page<FlowStatus>> list(Integer offset, Integer limit) {
    return list0(offset, limit, "flow:keys");
  }

  @Override
  public Promise<Page<FlowStatus>> listRunning(Integer offset, Integer limit) {
    return list0(offset, limit, "flow:running");
  }

  @Override
  public Promise<Page<FlowStatus>> findByTag(Integer offset, Integer limit, String key, String value) {
    String tagsKey = "tags:" + key + ":" + value;
    return list0(offset, limit, tagsKey);
  }

  private Promise<Page<FlowStatus>> list0(Integer offset, Integer limit, String key) {
    return Blocking.get(() ->
            exec(jedis -> {
              // redis is inclusive
              long correctedLimit = limit - 1;
              long maxRecords = jedis.llen(key);
              long startIdx = offset * correctedLimit + (offset > 0 ? 1 : 0);
              long endIdx = startIdx + correctedLimit;
              List<String> ids = jedis.lrange(key, startIdx, endIdx);
              List<FlowStatus> flowStatuses = Lists.newArrayList();
              if (ids.size() > 0) {
                flowStatuses = jedis.hmget("flow:all", ids.toArray(new String[ids.size()]))
                    .stream().map(this::readFlowStatus).map(this::blockingHydrateWorkStatuses)
                    .collect(Collectors.toList());
              }
              return new Page<>(offset, limit, (int) Math.max(1, Math.ceil(maxRecords / limit)), flowStatuses);
            })
    );
  }

  private FlowStatus blockingHydrateWorkStatuses(FlowStatus status) {
    return exec(jedis -> {
      List<String> workIds = jedis.lrange("flow:works:" + status.getId(), 0, 1000);
      Collections.reverse(workIds);
      List<WorkStatus> workStatuses = jedis.hmget("work:all", workIds.toArray(new String[workIds.size()]))
          .stream().map(this::readWorkStatus).collect(Collectors.toList());
      MutableFlowStatus mflow = status.toMutable();
      mflow.setWorks(workStatuses);
      return mflow;
    });
  }
}
