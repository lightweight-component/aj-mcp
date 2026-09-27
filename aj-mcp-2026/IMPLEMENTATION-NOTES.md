# Implementation decisions and follow-up choices

Implemented without waiting for confirmation, using the agreed Java 8 / three-module / lightweight / AJ Util boundaries.
These are adopted defaults, not blockers.

| Topic               | Adopted default                                                              | Possible future change                                                  |
|---------------------|------------------------------------------------------------------------------|-------------------------------------------------------------------------|
| Maven naming        | aj-mcp-next-core/client/server; inherited ajaxjs-parent:1.36 version         | Separate Next release version; directory/packages unchanged             |
| Transports          | STDIO and sessionless Streamable HTTP inside client/server                   | Thin Servlet/Spring integration without another foundation module       |
| AJ Util             | JSON engine, AJ HTTP initialization, AES-GCM                                 | Rebuild stale same-version local artifacts from refactored sources      |
| Registration        | Explicit tool/resource/prompt/method                                         | Annotation convenience is optional; old scanner not migrated            |
| Caching             | TTL 0/private, no shared authorization cache; callTool refreshes descriptors | Explicit identity-scoped cache for high-volume workloads                |
| MRTR                | Bounded automatic rounds; explicit application input handler                 | Wire roots, models and approval UI without automatic consent            |
| State security      | AJ AES-GCM, expiry, parameter/identity binding                               | Deployment key management/rotation and single-use replay ledger         |
| Subscriptions       | Request-scoped, finite lifetime, exact URI matching, explicit re-listen      | Provider-defined sub-resource relationships, never guessed URI prefixes |
| HTTP authentication | Authentication hook; default unauthenticated loopback example                | Require authentication/TLS remotely; no built-in OAuth issuer           |
| Legacy/extensions   | No automatic legacy fallback or Tasks/extension execution                    | Coexist with legacy SDK; no all-version conformance claim               |

## Boundaries

- The blocking JDK HTTP adapter is not a high-concurrency public gateway. Active SSE occupies an HTTP worker.
  Hosts still supply network/header/read/write timeouts, TLS, rate limits and business authorization.
- Java cancellation is cooperative; the SDK cannot safely kill arbitrary code that ignores interrupts.
  Callbacks must return promptly; synchronous future continuations must not block receive/deadline threads.
- RequestState does not replace input validation, permission checks or one-use execution records.
- Low-level HTTP tools/call requires listTools or explicit descriptors; callTool refreshes automatically.
- Core retains its pinned official schema snapshot. Passing tests is not official conformance certification.
- The local aj-net:2.1 artifact initially lacked the refactored Request class. The neighboring source was built and
  installed locally only, with AJ HTTP tests skipped. Its source was not changed and nothing was remotely published.

## Validation commands

Final results: standalone clean verify on actual JDK 8u181 and root module-selected verify on JDK 17.0.2 passed.
Each run: 119 tests (core 85, client 8, server 25, packaged integration 1), zero failures/errors/skips.
Eleventy documentation build and git diff --check passed; test EchoServer subprocesses exited.
The complete legacy external-service integration suite was not run. Nothing was committed, pushed or published.

```sh
mvn -f aj-mcp-2026/pom.xml -DskipTests=false clean verify
mvn -pl aj-mcp-2026/mcp-server -am -DskipTests=false verify
```

verify includes random-port HTTP, subprocess STDIO, the packaged-JAR example, UTF-8, header binding, Origin,
authentication, MRTR, subscriptions, failures, deadlines/cancellation, close, request isolation and state tampering.
package alone does not run the post-package Failsafe integration test. Legacy integration tests requiring external
services are outside this verification scope.

[中文](IMPLEMENTATION-NOTES.zh-CN.md)
