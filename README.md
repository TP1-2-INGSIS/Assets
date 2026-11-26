# Assets

## Dev tooling

Quality gates, all wired into `./gradlew check` (and therefore CI):

- **Formatter:** Spotless with google-java-format and unused-import removal. Run `./gradlew spotlessApply` to format; `check` fails if the code is not formatted.
- **Linter:** Checkstyle (`config/checkstyle/checkstyle.xml`), zero warnings allowed.
- **Coverage:** JaCoCo report (`build/reports/jacoco/test/html`) and a verification that fails the build below 80% line coverage. Only the Spring Boot entry point (`AssetsApplication`) is excluded.
- **Pre-commit hook:** `.githooks/pre-commit` runs `./gradlew check`. It is installed automatically (`core.hooksPath`) when compiling, or manually with `./gradlew installGitHooks`.

Tests use the `test` profile: H2 in memory and an in-memory blob storage, so no PostgreSQL or Azure is needed.
