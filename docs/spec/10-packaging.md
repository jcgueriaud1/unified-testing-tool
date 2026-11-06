# 10. Packaging

**Open.** Decided by [What are the Unified API's modules and artifacts?](https://github.com/jcgueriaud1/unified-testing-tool/issues/29).

Already fixed by other decisions:

| Input | From |
|---|---|
| The API depends on `flow-server` (Flow types in signatures) | [#16](https://github.com/jcgueriaud1/unified-testing-tool/issues/16) |
| No compile-time dependency on any Testing Tool; only the run's tool on the classpath | [#10](https://github.com/jcgueriaud1/unified-testing-tool/issues/10) |
| `@UnifiedSpringBootTest` needs Spring Boot test support | [#25](https://github.com/jcgueriaud1/unified-testing-tool/issues/25) |
| Browser runs need `com.vaadin:vaadin-dev` | [#24](https://github.com/jcgueriaud1/unified-testing-tool/issues/24) |
| Add-ons ship wrappers in their own jar, Unified API as `optional` dependency | [ADR 0003](../adr/0003-wrappers-are-interfaces-shipped-in-the-addon-jar.md) |
| Possible community `unified-addons` bundle | [#18](https://github.com/jcgueriaud1/unified-testing-tool/issues/18) |

To decide: artifact split, BOM, mapping the run's Testing Tool to dependencies, baselines and versioning, who maintains the community bundle.
