ratpack-workflow
---

A workflow engine for Ratpack, built on the Ratpack Execution Model

Quick Start
===

The project ships with a [WorkflowModule](https://github.com/danveloper/ratpack-workflow/blob/master/src/main/java/com/danveloper/ratpack/workflow/guice/WorkflowModule.java) that you'll need to incorporate into your project. It supplies the handlers that you can use to RESTfully retrieve details about a specific work or flow operation.

```java
public class Main {
  public static void main(String[] args) throws Exception {
    RatpackServer.start(spec -> spec
      .registry(Guice.registry(r -> r
        .module WorkflowModule.of(chain -> chain
          .work("name", "version", ctx -> {
            // this is your work handler
          })
          .all(ctx -> {
            // semantics are similar to the Ratpack Handler Chain
            ctx.next()
          })
          .all(ctx -> {
            // when you're done, call .complete()
            ctx.complete()
          })
        ))
      )
      .handlers(chain -> chain
        .get(ctx -> {
          // inter-operates politely with the HTTP handler chain
        })
        .get("ops", ctx -> {
          // RESTfully provide details about your work
          ctx.byMethod(m -> { m
            .get(WorkflowModule.workListHandler())
            .post(WorkflowModule.workSubmissionHandler())
          });
        })
        .get("ops/:id", WorkflowModule.workStatusGetHandler())
      )
    );
  }
}
```

Work vs. Flows
===

TODO: Describe the differences

Contributors
---

* [Dan Woods](https://twitter.com/danveloper)
