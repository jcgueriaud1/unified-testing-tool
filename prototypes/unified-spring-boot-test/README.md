# PROTOTYPE: @UnifiedSpringBootTest (throwaway)

Answers the task "Prove @UnifiedSpringBootTest switches web environment per Testing Tool" (#24) on the
map "Unified testing API for Vaadin" (#2). Never merged; lives on branch `task/unified-spring-boot-test`.

Spring Boot 4.1.1, Vaadin 25.3.5, Browserless Test 25.3.2, Playwright 1.64.0, JDK 21.
One view (`MainView`: TextField "Name", Button "Greet", Span `#greeting`) backed by `GreetingService`.

## Run

```
mvn test -Dvaadin.testing.tool=browserless                                     # default
mvn test -Dvaadin.testing.tool=playwright                                      # needs Chromium
mvn test -Dvaadin.testing.tool=browserless -DargLine=-Dunified.alwaysRandomPort=true   # fallback variant
```

## How it works

- `@UnifiedSpringBootTest` = `@BootstrapWith(UnifiedSpringBootTestContextBootstrapper)` + `SpringExtension`
  + `UnifiedExtension`. Not meta-annotated with `@SpringBootTest` (one `@BootstrapWith` only).
- The bootstrapper extends `SpringBootTestContextBootstrapper`: `getWebEnvironment` returns MOCK for
  Browserless, RANDOM_PORT for browsers (drives `server.port=0` and the web merged config).
- **Hack:** `SpringBootContextLoader` decides embedded server vs mock servlet context from the
  package-private `SpringBootTestAnnotation` customizer, read from the test class's `@SpringBootTest`.
  No public hook exists, so `org.springframework.boot.test.context.UnifiedWebEnvironmentAccess`
  (test sources, Spring's package) swaps that customizer.
- `UnifiedExtension` injects `UnifiedPage`: Browserless via
  `SpringBrowserlessApplicationContext.create(springContext, ...)` (public API, no base class);
  Playwright against `http://localhost:${local.server.port}`.
- The Testing Tool comes from the system property `vaadin.testing.tool`: Spring's bootstrapper cannot
  read JUnit configuration parameters.

## Results (all green)

| Point | Browserless run | Playwright run |
|---|---|---|
| 1. Web environment per run | mock, no server (`WebServerApplicationContext` false) | Tomcat on random ports (36491, 40025) |
| 2. Browserless Spring wiring without base class | `SpringBrowserlessApplicationContext.create` works; views get beans | n/a |
| 3. Base URL from port | n/a | `http://localhost:40025`, never 8080 |
| 4. `@MockitoBean` | mock used | mock used, seen in the real browser |
| 5. Context cache | 2 plain classes share 1 context (1 service instance); mocked class has its own | same |
| Build time (3 tests) | 6.8 s | 25.9 s |

Fallback (always RANDOM_PORT, also on Browserless): green, 10.8 s, starts Tomcat for nothing.

Browser runs need `com.vaadin:vaadin-dev` (dev mode serving the pre-built default dev bundle);
without it: `'vaadin-dev-server' not found`. No Node.js was needed.
