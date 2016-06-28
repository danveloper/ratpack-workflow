Ratpack Workflow
---

A workflow engine for Ratpack, built on the Ratpack Execution Model. Requires Ratpack 1.3.3.

[![Build Status](https://travis-ci.org/danveloper/ratpack-workflow.svg)](https://travis-ci.org/danveloper/ratpack-workflow)

Try it
===

Add this to your build.gradle:

```groovy
repositories {
  maven { url 'http://oss.jfrog.org/artifactory/repo' }
}

dependencies {
  compile 'com.danveloper.ratpack.workflow:ratpack-workflow:0.3.3'
}
```

Quick Start
===

The project ships with an extension to the regular Ratpack server specification. Nothing about your application needs to change except the class from which you import the `RatpackServer`. The below example shows importing the Groovy DSL from the `GroovyRatpackWorkflow` class, instead of Ratpack's `Groovy` class.

```groovy
@Grab('com.danveloper.ratpack.workflow:ratpack-workflow-groovy:0.3.3')

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

RESTful HTTP handlers are provided via static methods on the `RatpackWorkflow` class. You can use these endpoints to list jobs, lookup specific jobs, and submit jobs.

Submitting Work
===

A properly formed JSON payload can be POSTed to the `FlowSubmissionHandler` to start a flow.

```javascript
{
  "name": "AWESOME-FLOW-001"
  "description": "My awesome flow thingy",
  "tags": {
    "key": "val"
  },
  "works": [
    {
      "type": "workSomething",
      "version": "1.0",
      "data": {
        "key": "val",
        "key2": "val2"
      }
    },
    {
      "type": "workSomethingElse",
      "version": "1.0",
      "data": {
        "somekey": "someval",
        "somekey2": "someval2"
      }
    }
  ]
}
```

Given this JSON, a workflow definition like the following will be matched:

```
class WorkSomethingConfig {
  String key
  String key2
}

class WorkSomethingElseConfig {
  String somekey
  String somekey2
}

ratpack {
  workflow {
    work("workSomething", "1.0") {
      def workConfig = config.mapData(WorkSomethingConfig)
      assert workConfig.key == "val"
      assert workConfig.key2 == "val2"
      complete()
    }
    work("workSomethingElse", "1.0") {
      def workConfig = config.mapData(WorkSomethingElseConfig)
      assert workConfig.somekey == "someval"
      assert workConfig.somekey2 == "someval2"
      complete()
    }
  }
  handlers {
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
