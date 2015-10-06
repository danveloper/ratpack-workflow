ratpack-workflow
---

A workflow engine for Ratpack, built on the Ratpack Execution Model

Quick Start
===

The project ships with a [WorkflowModule](https://github.com/danveloper/ratpack-workflow/blob/master/src/main/java/com/danveloper/ratpack/workflow/guice/WorkflowModule.java) that you'll need to incorporate into your project. It supplies the handlers that you can use to RESTfully retrieve details about a specific work or flow operation.

```groovy
@Grab('com.danveloper.ratpack.workflow:ratpack-workflow-groovy:0.1-SNAPSHOT')

import com.danveloper.ratpack.workflow.server.RatpackWorkflow
import static com.danveloper.ratpack.workflow.server.GroovyRatpackWorkflow.ratpack

ratpack {
  workflow {
    work("type", "version") {
      // do one thing
      ...
      // trigger completion
      complete()
    }
    flow("name", "version") {
      all {
        // always run this
        ...
        // move on to the next
        next()
      }
      all {
        ...
        complete()
      }
    }
  }
  handlers {
    prefix("flows") {
      get(":id", RatpackWorkflow.flowStatusGetHandler())
      get(RatpackWorkflow.flowListHandler())
      post(RatpackWorkflow.flowSubmissionHandler())
    }
    prefix("works") {
      get(":id", RatpackWorkflow.workStatusGetHandler())
      get(RatpackWorkflow.workListHandler())
      post(RatpackWorkflow.workSubmissionHandler())
    }
    // ...
  }
}
```

Backing Persistence
===

By default, work and flow status will use the `InMemoryWorkStatusRepository` and `InMemoryFlowStatusRepository` repositories respectively. By including the `ratpack-workflow-redis` module in your project, you can elect to bind the `RedisWorkStatusRepository` and `RedisFlowStatusRepository` types via a registry definition.

Redis support, by default, will connect to `localhost:6379`. Binding a custom `JedisPool` will allow you to configure connection properties.

```groovy
import com.danveloper.ratpack.workflow.redis.*
import static com.danveloper.ratpack.workflow.server.GroovyRatpackWorkflow.ratpack

ratpack {
  bindings {
    bind(FlowStatusRepository, RedisFlowStatusRepository)
    bind(WorkStatusRepository, RedisWorkStatusRepository)
  }
  workflow {
    // ...
  }
  handlers {
    // ...
  }
}
```

Work vs. Flows
===

There are two types of operations that can be submitted to the workflow processor: `work` and `flow`. A `work` type represents an atomic operation, whereas a `flow` type encapsulates many `work` types that can be chained together.

Contributors
---

* [Dan Woods](https://twitter.com/danveloper)
