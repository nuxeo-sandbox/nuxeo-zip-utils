# Nuxeo Zip Utils — Agent Guide

## Project

Nuxeo LTS 2025 plugin (Java 21, Maven) providing utilities for dealing with archives (zip, tar, rar, 7z, etc.). Exposes Automation operations for zip inspection, extraction, creation, and generic archive handling via Apache Commons Compress.

- **Parent**: `org.nuxeo:nuxeo-parent:2025.16`
- **GroupId**: `nuxeo.zip.utils`
- **Version**: `2025.1.0-SNAPSHOT`

## Modules

| Module | Purpose |
|--------|---------|
| `nuxeo-zip-utils-core` | Java code, OSGI-INF component XMLs, tests |
| `nuxeo-zip-utils-package` | Nuxeo Marketplace package (assembly) |

Key paths in the core module:
- Java sources (zip ops): `nuxeo-zip-utils-core/src/main/java/nuxeo/zip/utils/`
- Java sources (archive utils): `nuxeo-zip-utils-core/src/main/java/org/nuxeo/utils/archive/`
- Component XMLs: `nuxeo-zip-utils-core/src/main/resources/OSGI-INF/`
- Bundle manifest: `nuxeo-zip-utils-core/src/main/resources/META-INF/MANIFEST.MF`
- Tests: `nuxeo-zip-utils-core/src/test/java/`

## Build & Test Commands

```bash
# Full build (all modules)
mvn clean install

# Build skipping tests
mvn clean install -DskipTests

# Run tests only in core module
mvn test -pl nuxeo-zip-utils-core

# Run a single test class
mvn test -pl nuxeo-zip-utils-core -Dtest=TestZipInfo
```

No CI workflows, no linter, no formatter configured in this repo. `nuxeo.skip.enforcer=true` is set in the root POM.

## Adding New Code

### New Automation Operation

1. Create the Java class annotated with `@Operation(id = "...")` containing `@OperationMethod`
2. Create or update an OSGI-INF XML to register it via `<extension target="org.nuxeo.ecm.core.operation.OperationServiceComponent" point="operations">`
3. List the XML in `META-INF/MANIFEST.MF` under `Nuxeo-Component:`. Currently all components are comma-separated on one line; if the line gets too long, use continuation lines (each starting with a single space)
4. MANIFEST.MF **must end with a trailing newline** (`\n` after the last line) or the last header is silently dropped

### New Service (Component + Extension Point)

1. Create a service interface and a component class extending `DefaultComponent`
2. Declare in OSGI-INF XML with `<component>`, `<implementation>`, `<service>`, optional `<extension-point>`
3. Register in MANIFEST.MF
4. Look up at runtime: `Framework.getService(MyService.class)`

### New Schema or Document Type

- Schema XSD files go in `src/main/resources/schema/`
- Register via `<extension target="org.nuxeo.ecm.core.schema.TypeService" point="schema">`
- Document types via `point="doctype"`

### Dependencies

Module POMs declare dependencies **without `<version>` tags** — versions are managed by `nuxeo-parent`. The core module depends on `nuxeo-automation-core` and `nuxeo-platform-filemanager` (compile), `commons-compress` (managed by nuxeo-parent), `zstd-jni`, `brotli dec`, and `xz` (compile with explicit versions), and `nuxeo-automation-test` + several platform modules (test).

## Critical Pitfalls

These will cause silent failures or build errors if ignored:

- **NOT Spring**: No `@Autowired`, `@Component`, `@Service`. Use Nuxeo's component model (`DefaultComponent`, `Framework.getService()`)
- **Jakarta, not javax**: All imports use `jakarta.*` namespace (`jakarta.inject`, `jakarta.ws.rs`, etc.)
- **JUnit 4 only**: Use `@RunWith(FeaturesRunner.class)` + `@Features(...)` + `@Deploy(...)`. No JUnit 5
- **Log4j2 only**: `LogManager.getLogger(MyClass.class)`. No SLF4J, no `System.out.println`. (Note: some legacy code in `org.nuxeo.utils.archive` still uses commons-logging `LogFactory` — new code should use Log4j2)
- **No raw Jackson for REST**: Use Nuxeo's `MarshallerRegistry` framework
- **No JPA**: Document storage uses `CoreSession` / `DocumentModel` API

## Testing Patterns

```java
@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy("nuxeo.zip.utils.nuxeo-zip-utils-core")
public class TestMyOperation {

    @Inject
    protected CoreSession session;

    @Inject
    protected AutomationService automationService;

    @Test
    public void testSomething() throws Exception {
        // ...
    }
}
```

- `@Deploy("bundle.symbolic.name")` — the symbolic name is in MANIFEST.MF (`Bundle-SymbolicName`: `nuxeo.zip.utils.nuxeo-zip-utils-core`)
- `@Deploy("bundle:OSGI-INF/test-contrib.xml")` — deploy test-only XML contributions
- `TransactionalFeature.nextTransaction()` — flush async work between steps

## Local References (optional)

If the Nuxeo LTS 2025 source code or other plugin examples are available locally, working with local files is faster and avoids network calls. Ask the user for local paths before falling back to GitHub.

Prompt the user with:

> "Do you have the Nuxeo LTS 2025 source code cloned locally? If so, what is the path? (e.g., `~/GitHub/nuxeo/nuxeo-lts`). Otherwise, I'll use the GitHub repository."

Similarly, for plugin examples:

> "Do you have any Nuxeo plugin examples locally? (e.g., `nuxeo-labs-dynamic-fields`, `nuxeo-dynamic-asset-transformation`). If so, what are the paths? Otherwise, I'll browse them on GitHub."

### Fallback URLs

- Nuxeo LTS 2025: https://github.com/nuxeo/nuxeo-lts (branch `2025`)
- Plugin examples: https://github.com/nuxeo-sandbox/nuxeo-labs-signature-webui, https://github.com/nuxeo-sandbox/nuxeo-labs-pdf-toolkit

## Code Style

### Java

- 4-space indent, **no tabs**, no trailing spaces, K&R braces, ~120 char lines
- Use modern Java: `var`, records, pattern matching `instanceof`, switch expressions, text blocks, `String.formatted()`
- **No wildcard imports**. Import order: static, `java.*`, `jakarta.*`, `org.*`, `com.*`
- Always use braces for `if`/`else` blocks (even single-line)
- No `final` on method parameters or local variables
- No `private` methods/fields (use `protected`); exceptions: `log` and `serialVersionUID`
- No `final` classes or methods (hinders extensibility)
- Use `i++` (not `++i`) for simple increments
- Use `Objects.requireNonNull` for null checks
- Logging: parameterized messages `log.debug("Processing: {}", docId)`, use `if (log.isDebugEnabled())` for non-constant messages
- Javadoc first sentence: 3rd person verb phrase ending with period (*Gets the foobar.* not *Get the foobar*)
- `@since 2025.XX` on new public API. No `@author` tag
- License header: Apache 2.0 with current year and `Contributors:` section

### XML (OSGI-INF, POMs, XSD, HTML)

- **2-space indent**, no tabs, 120 char line width
- Self-closing tags: add space before `/>` (e.g., `<property name="foo" />`)

### JavaScript (Automation Scripting)

- **4-space indent**, no tabs
- Nuxeo Automation Scripting uses **ECMAScript 5** (no `let`/`const`, no arrow functions, no template literals)
- Automation operations are called as global functions: `Document.Create(input, type, name, properties)`, `Document.Query(null, {"query": queryString})`, etc. — always pass named parameters as an object

### Markdown (README, docs)

- Use GitHub alert syntax for notes, warnings, tips, etc.:
  ```
  > [!NOTE]
  > Content here

  > [!WARNING]
  > Content here
  ```

## Release Process

> [!WARNING]
> Check the repository is clean before starting. Alert and stop if there are uncommitted changes.

> [!IMPORTANT]
> The version numbers below are examples only. Always read the actual current version from the POM before running any command, and derive the release and next snapshot versions from it (e.g. `2025.1.0-SNAPSHOT` → release `2025.1.0` → next snapshot `2025.2.0-SNAPSHOT`).

1. Remove `-SNAPSHOT` from the current version: `mvn versions:set -DnewVersion=<current-version-without-SNAPSHOT> -DgenerateBackupPoms=false`
2. Build: `mvn clean install -DskipTests`
3. Copy `nuxeo-zip-utils-package/target/nuxeo-zip-utils-package-<version>.zip` to `~/Downloads/`
4. Bump to next snapshot (increment minor, reset incremental to 0, add `-SNAPSHOT`): `mvn versions:set -DnewVersion=<next-version>-SNAPSHOT -DgenerateBackupPoms=false`
5. Verify: `mvn clean install -DskipTests`
6. Commit and push:
   ```bash
   git add .
   git commit -m "Post <version> release"
   git push
   ```

> [!NOTE]
> No git tag or GitHub release is created. The ZIP copied to `~/Downloads` is the deliverable.
