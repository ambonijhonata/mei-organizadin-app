# Kotlin quality gates

Run the same validation used by pull requests from the repository root:

```powershell
.\gradlew.bat --no-daemon qualityCheck
```

The task runs unit tests, Detekt 1.23.8, KtLint, Android Lint, Kotlin CPD, the
hardcoded URL check, and the suppression-policy check. Every finding fails the
task. The CPD threshold is versioned as `cpdMinimumTokens` in
`gradle.properties`. The initial value is 240 tokens: calibration found a
235-token historical duplicate after the largest copied test fake was removed.

The GitHub workflow exposes the stable check name `Kotlin Quality / Kotlin
Quality`. Configure that check as required in the `main` branch ruleset so a
failed or pending validation blocks merge.

Kover is the next quality stage. Before enabling it as a required check, measure
the current line and branch coverage, agree on minimum thresholds, add focused
tests for critical flows, and only then make coverage regression fail the pull
request. Do not introduce a baseline merely to make the first Kover run pass.

The KtLint policy keeps all standard rules enabled except naming rules that
conflict with Compose's PascalCase functions, maximum line length, and the
function/class signature rules that produce a format-check cycle under the
Android Studio code style. These choices are explicit in `.editorconfig`; no
baseline or per-file suppression is used.

Detekt complexity thresholds were calibrated against the existing Compose
structure. In particular, `LargeClass` is capped at 800 lines because the
largest current view model has 779 formatted lines. Any configured Detekt
finding still fails the build.

Android Lint promotes warnings to errors and aborts the build on every enabled
finding. Its centralized policy excludes only `GradleDependency`, because
dependency upgrades require dedicated compatibility work, and
`IconLauncherShape`, because it requires a new approved visual asset. All
other enabled Android Lint checks remain blocking.
