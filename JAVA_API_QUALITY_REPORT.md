# Java API Quality Report

Branch: `audit/java-api-quality-report`

Scope: backend Java code only.

Goal: identify unused code, complexity risks, duplicated flows, clean code issues, test coverage gaps, and safe improvement opportunities without changing public API behavior.

This report is intentionally diagnostic. It does not propose behavior changes. Any future refactor should be protected by tests first.

## Executive Summary

The backend is currently in a healthy functional state: the full Maven test suite passes with 63 tests.

The largest quality risks are not compile errors or obvious PMD violations. They are maintainability and verification risks:

- Coverage and CRAP cannot currently be measured reliably because JaCoCo is not wired into the Maven test lifecycle.
- `ProfessorService` has become the main concentration point for professor CRUD, approval, course request workflow, course mapping, and publication lookup.
- `AuthService.register` does too many things in one method and uses raw `RuntimeException` for expected API errors.
- Ranking and report calculations duplicate approved-professor/window/publication-count logic.
- Some list/detail mapping flows can create N+1 database queries.
- One admin professor-publications flow returns raw `Object`/entity-like data instead of typed API DTOs.
- There are likely unused DTO classes that create naming confusion.

Recommended next step: create a small tooling PR first to enable JaCoCo coverage reporting correctly. Then refactor one service boundary at a time with characterization tests around current API contracts.

## Commands Run

Backend test baseline:

```bash
mvn -Dmaven.repo.local=/tmp/irs-m2 test
```

Result: passed.

Summary:

- Tests run: 63
- Failures: 0
- Errors: 0
- Skipped: 0

PMD and CPD:

```bash
mvn -Dmaven.repo.local=/tmp/irs-m2 org.apache.maven.plugins:maven-pmd-plugin:3.26.0:pmd org.apache.maven.plugins:maven-pmd-plugin:3.26.0:cpd
```

Result: build success.

Summary:

- PMD default rules reported no violations.
- CPD reported no duplicated blocks at the default threshold.

SpotBugs:

```bash
mvn -Dmaven.repo.local=/tmp/irs-m2 com.github.spotbugs:spotbugs-maven-plugin:4.8.6.6:spotbugs
```

Result: build success.

Summary:

- 28 SpotBugs bug instances in `target/spotbugsXml.xml`.
- The findings are mostly representation exposure warnings in DTOs/entities and some static-field suggestions.

Dependency analyzer:

```bash
mvn -Dmaven.repo.local=/tmp/irs-m2 org.apache.maven.plugins:maven-dependency-plugin:3.8.1:analyze
```

Result: build success.

Summary:

- The output is noisy because this is a Spring Boot starter based project.
- Do not remove Spring Boot starters just because dependency-analyzer labels them unused.
- Treat this output as advisory only until the analyzer is configured for Boot starters.

JaCoCo attempts:

```bash
mvn -Dmaven.repo.local=/tmp/irs-m2 org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent test org.jacoco:jacoco-maven-plugin:0.8.12:report
```

Result: tests passed, but JaCoCo skipped report generation because the execution data file was missing.

Reason found: the Surefire plugin currently hardcodes `argLine`, so the property injected by `jacoco:prepare-agent` is not being used.

Current Surefire pattern:

```xml
<argLine>-XX:+EnableDynamicAgentLoading -Xshare:off -Dnet.bytebuddy.experimental=true</argLine>
```

Expected JaCoCo-friendly pattern:

```xml
<argLine>${argLine} -XX:+EnableDynamicAgentLoading -Xshare:off -Dnet.bytebuddy.experimental=true</argLine>
```

An offline instrumentation attempt also failed because the JaCoCo offline runtime was not on the test classpath. The codebase was cleaned afterward and `mvn test` passed again.

## Size And Test Metrics

Production Java:

- Files: 76
- Lines: 2,978

Test Java:

- Files: 9
- Lines: 1,651

Largest production files:

| File | Lines | Notes |
| --- | ---: | --- |
| `ProfessorService.java` | 270 | Main complexity hotspot. |
| `PublicationService.java` | 148 | Good service boundary, but has repeated auth/status logic and N+1 risk. |
| `Professor.java` | 137 | Entity is moderately large due course relation and lifecycle fields. |
| `ProfessorCourseChangeRequest.java` | 121 | Entity has meaningful workflow state. |
| `PublicationController.java` | 117 | Controller is mostly thin, but has repeated role checks. |
| `AuthService.java` | 108 | Registration method has several responsibilities. |
| `CourseComplianceDto.java` | 104 | Mutable DTO style differs from newer record DTOs. |
| `ProfessorController.java` | 101 | Controller is acceptable but covers many flows. |
| `ReportService.java` | 86 | Clear but duplicates metric logic with ranking. |
| `RankingService.java` | 76 | Clear but duplicates report metric logic. |

Largest test files:

| File | Lines | Notes |
| --- | ---: | --- |
| `ProfessorServiceTest.java` | 513 | Strong coverage of professor/course workflow, but broad. |
| `ProfessorControllerTest.java` | 314 | Important contract protection. |
| `PublicationServiceTest.java` | 198 | Covers publication business rules. |
| `ReportServiceTest.java` | 182 | Protects compliance calculations. |
| `PublicationControllerTest.java` | 122 | Protects publication HTTP behavior. |
| `AuthServiceTest.java` | 122 | Covers auth service behavior. |
| `RankingServiceTest.java` | 105 | Protects ranking behavior. |
| `AuthControllerTest.java` | 72 | Protects auth HTTP behavior. |
| `ReportingWindowServiceTest.java` | 23 | Focused date-window test. |

Package distribution:

| Package Area | Production Files | Test Files | Observation |
| --- | ---: | ---: | --- |
| `professor` | 30 | 2 | High behavior density, needs service-level protection before refactors. |
| `auth` | 10 | 2 | Registration/login have tests, but error contracts should be expanded. |
| `publication` | 10 | 2 | Reasonable test ratio. |
| `course` | 8 | 0 | Course behavior is indirectly tested through professor/report flows. |
| `report` | 6 | 2 | Good focused report coverage. |
| `ranking` | 4 | 1 | Good focused ranking coverage. |
| `common` | 3 | 0 | Exception/error response contracts need controller coverage. |
| `user` | 3 | 0 | Mostly model/repository support. |
| `config` | 1 | 0 | Security configuration should be protected by controller authorization tests. |

## Coverage And CRAP Status

Coverage ratio was not computed because JaCoCo is not currently integrated into the Maven lifecycle.

CRAP score was also not computed. CRAP depends on cyclomatic complexity and test coverage, so any CRAP result without reliable coverage would be misleading.

Recommended tooling PR before refactoring:

1. Add JaCoCo to `pom.xml`.
2. Update Surefire `argLine` so JaCoCo can inject its Java agent.
3. Generate `target/site/jacoco/index.html`.
4. Start with reporting only, not threshold enforcement.
5. Add threshold enforcement only after the team agrees on realistic baseline targets.

Suggested minimum first target:

- Service classes: track coverage first.
- Business-rule services should trend toward high branch coverage.
- Do not block the build globally until legacy uncovered areas are understood.

## Tool Findings

### PMD

No violations were reported by default PMD rules.

Important caveat: default PMD rules are not enough to prove low complexity. A stricter PMD ruleset should be configured if the team wants checks for:

- Excessive method length.
- Excessive class length.
- Cyclomatic complexity.
- Cognitive complexity.
- Too many dependencies.
- Duplicate literals.
- God classes.

### CPD

No duplicate blocks were reported at the default threshold.

Important caveat: duplicated business ideas can exist even when token-level duplication is low. The ranking/report duplication in this project is conceptual rather than copy-paste duplication.

### SpotBugs

SpotBugs reported 28 bug instances.

Main categories:

- `EI_EXPOSE_REP`: exposed internal mutable representation.
- `EI_EXPOSE_REP2`: stored external mutable representation.
- `SS_SHOULD_BE_STATIC`: field can be static.

Most important affected areas:

- `professor.dto`
- `report.dto`
- `SecurityConfig`
- `CourseController`
- `ProfessorController`
- `PublicationController`
- `Professor`
- `ProfessorCourseChangeRequest`
- `JwtService`
- `RegisterRequest`

Interpretation:

- Some Spring constructor-injection warnings are low risk and can be suppressed/configured if SpotBugs becomes part of CI.
- DTO collection exposure warnings are more actionable. DTOs that carry `List` or `Set` should use defensive copies if immutability is expected.
- Entity collection exposure warnings need more care because JPA-managed collections are mutable by design. Do not blindly wrap JPA collections in unmodifiable collections inside entities without testing persistence behavior.

### Dependency Analyzer

The Maven dependency analyzer is noisy in this project because Spring Boot starters intentionally bring transitive dependencies.

Recommendation:

- Do not remove starter dependencies based only on analyzer output.
- If dependency analysis becomes a CI check, configure ignored dependencies/starters explicitly.
- Review only direct dependencies that are obviously unused and not starter-managed.

## Main Findings

### 1. Coverage And CRAP Are Not Measurable Yet

Severity: high.

Files:

- `pom.xml`

Problem:

The test suite passes, but coverage cannot be measured because JaCoCo is not correctly wired into Surefire. The current `argLine` prevents the JaCoCo agent from being injected.

Why it matters:

The user asked for complexity and CRAP analysis. CRAP cannot be responsibly calculated without coverage. The project currently has passing tests, but no reliable percentage for line, branch, or method coverage.

Recommended fix:

Create a separate tooling PR:

- Add JaCoCo Maven plugin.
- Preserve the existing Byte Buddy/Surefire JVM flags.
- Use `${argLine}` in Surefire.
- Run `mvn -Dmaven.repo.local=/tmp/irs-m2 test jacoco:report`.
- Publish the generated coverage report as an artifact if CI exists.

Tests before/after:

- Existing `mvn test` should remain green.
- No product behavior tests are needed because this is build tooling only.

### 2. `ProfessorService` Has Too Many Responsibilities

Severity: medium/high.

File:

- `src/main/java/br/com/ibmec/researchstars/professor/service/ProfessorService.java`

Observed responsibilities:

- List professors.
- Get professor details.
- Approve professors.
- Update professor data.
- Delete professors.
- Find professor publications through a gateway.
- Load and map courses.
- Validate course IDs.
- Create professor course change requests.
- Approve/reject course change requests.
- Supersede pending requests after direct admin edits.
- Build nested detail DTOs.

Why it matters:

The class is still understandable, but it is now the central workflow coordinator for most professor behavior. Future changes to course approval, reporting, registration, or professor profile behavior will likely touch this file, increasing conflict risk and regression risk.

Recommended refactor:

Do not split immediately without tests. First add/confirm tests for:

- Admin approving a professor with pending course request.
- Admin rejecting course change request.
- Admin direct course edit superseding pending request.
- Professor self course-change request.
- Pending course requests not changing reports.

Then consider extracting:

- `ProfessorCourseChangeService`
- `ProfessorQueryService` or mapper helpers for detail DTO assembly
- `ProfessorPublicationService` or typed publication lookup

Keep the public controller API unchanged.

### 3. `AuthService.register` Does More Than One Thing

Severity: medium/high.

File:

- `src/main/java/br/com/ibmec/researchstars/auth/service/AuthService.java`

Current method behavior:

- Checks duplicate email.
- Checks duplicate Lattes URL.
- Validates requested courses.
- Creates user.
- Creates professor.
- Creates initial pending course request.
- Builds JWT auth response.

Why it matters:

This method is a key business entry point. It mixes validation, persistence, workflow creation, and response construction. It also throws raw `RuntimeException` for expected validation failures.

Recommended refactor:

Before changing implementation, add or confirm controller tests for:

- Duplicate email response.
- Duplicate Lattes URL response.
- Invalid course ID response.
- Successful registration response shape.
- Successful registration creates pending course request, not approved courses.

Then split the method internally into small private methods or a focused registration workflow component:

- `validateUniqueRegistrationData`
- `validateRequestedCourses`
- `createProfessorUser`
- `createPendingInitialCourseRequest`
- `buildAuthResponse`

Also replace raw `RuntimeException` with domain exceptions that produce consistent API error responses.

### 4. Raw `RuntimeException` Is Used For Expected API Errors

Severity: medium.

Files:

- `src/main/java/br/com/ibmec/researchstars/auth/service/AuthService.java`
- `src/main/java/br/com/ibmec/researchstars/professor/service/ProfessorService.java`
- `src/main/java/br/com/ibmec/researchstars/ranking/service/RankingService.java`

Problem:

Expected cases like duplicate user, invalid credentials, missing professor, and gateway failures are represented inconsistently. Some use domain exceptions, others use `RuntimeException` or `IllegalArgumentException`.

Why it matters:

API clients need stable HTTP status codes and error response shapes. Inconsistent exception types make behavior harder to reason about and harder to test.

Recommended fix:

Add explicit exception classes or reuse existing domain exceptions:

- `DuplicateResourceException`
- `InvalidCredentialsException`
- `ProfessorNotFoundException`
- `InvalidCourseAssignmentException`

Before changing:

- Add controller tests for status code and response JSON.
- Verify existing frontend error handling expects the current message format.

### 5. Ranking And Report Logic Duplicate The Same Business Concept

Severity: medium.

Files:

- `src/main/java/br/com/ibmec/researchstars/report/service/ReportService.java`
- `src/main/java/br/com/ibmec/researchstars/ranking/service/RankingService.java`

Duplicated concept:

- Use the default reporting window.
- Count only approved professors.
- Count only validated publications.
- Count publications in the window.
- Use the same professor-level publication count to decide compliance/ranking.

Why it matters:

The dashboard, course compliance, and ranking are business-critical. If a future requirement changes the default window or compliance threshold, these flows can diverge again.

Recommended refactor:

Create a small reusable domain service after tests are in place:

- `ProfessorPublicationMetricsService`

Possible responsibilities:

- Return publication count for professor in reporting window.
- Return compliant/not compliant for professor.
- Return approved professor metrics used by ranking/report.

Tests before changing:

- Ranking uses the calendar-aligned default window.
- Course compliance uses the same window.
- Only approved professors count.
- Only validated publications count.
- Multi-course professors appear once in ranking.
- Multi-course professors count in every approved course they belong to.

### 6. Publication List Mapping Can Produce N+1 Queries

Severity: medium.

File:

- `src/main/java/br/com/ibmec/researchstars/publication/service/PublicationService.java`

Problem:

`findAll` maps publications through `toResponse`, and `toResponse` resolves professor name by querying `ProfessorRepository` for each publication.

Why it matters:

This is fine for small local data, but admin publication lists can grow. Pagination hides some risk, but each page can still create unnecessary database calls.

Recommended fix:

Options:

- Add repository query/projection that returns publication and professor name together.
- Batch-load professor IDs for the current page and map names in memory.
- Add entity relation if the domain model supports it cleanly.

Tests before changing:

- Admin publication list response shape stays the same.
- Professor name still appears when available.
- Missing professor fallback behavior stays the same.
- Existing filters by status/professor/title still work.

### 7. Admin Professor Publications Flow Is Untyped

Severity: medium.

Files:

- `src/main/java/br/com/ibmec/researchstars/professor/service/ProfessorPublicationsGateway.java`
- `src/main/java/br/com/ibmec/researchstars/professor/service/ProfessorPublicationsGatewayImpl.java`
- `src/main/java/br/com/ibmec/researchstars/professor/dto/ProfessorPublicationsResponse.java`

Problem:

The gateway returns `List<Object>`, and the response DTO exposes `List<Object> publications`.

Why it matters:

This breaks the project rule of not exposing JPA entities or untyped objects directly in API responses. It makes the frontend/backend contract less clear and harder to test.

Recommended fix:

Use a typed DTO:

- `PublicationResponse`, if the existing API shape is acceptable.
- Or a smaller `ProfessorPublicationSummaryDto` if the professor detail screen needs fewer fields.

Tests before changing:

- Admin can open professor detail and see publications.
- JSON response fields match current frontend needs.
- No entity-only fields leak into the response.

### 8. Possible Unused DTO Classes

Severity: low/medium.

Files:

- `src/main/java/br/com/ibmec/researchstars/course/dto/Course.java`
- `src/main/java/br/com/ibmec/researchstars/user/dto/User.java`

Problem:

These classes appear unused and share names with domain entities. This increases cognitive load and import confusion.

Recommended fix:

Delete only after a compile check confirms there are no references.

Tests before changing:

- `mvn test` is enough if they are truly unused.

### 9. DTO Immutability Is Inconsistent

Severity: low/medium.

Files:

- Several DTOs under `professor/dto`
- `src/main/java/br/com/ibmec/researchstars/report/dto/CourseComplianceDto.java`

Problem:

The codebase uses a mix of records, mutable classes, and collection-bearing DTOs. SpotBugs flags exposed mutable lists/sets in DTOs.

Why it matters:

Mutable DTO collections can be changed after construction. This usually does not break HTTP serialization, but it makes service tests and internal contracts less predictable.

Recommended fix:

For record DTOs with collections, use canonical constructors:

```java
public SomeDto {
    values = values == null ? List.of() : List.copyOf(values);
}
```

For mutable DTO classes, either keep them intentionally mutable for framework compatibility or convert them to records only when JSON contract tests are in place.

Tests before changing:

- JSON serialization shape remains unchanged.
- Null and empty collections serialize as expected.

### 10. Controllers Are Mostly Thin, But Some Role Checks Repeat

Severity: low.

File:

- `src/main/java/br/com/ibmec/researchstars/publication/controller/PublicationController.java`

Problem:

The controller repeatedly calculates whether the current user is admin.

Why it matters:

This is not a major issue. It is a small readability concern.

Recommended fix:

Extract a private helper method only if touching this controller for another reason:

```java
private boolean isAdmin(UserPrincipal principal) {
    return principal.getRole() == User.Role.ADMIN;
}
```

No separate PR is needed just for this.

### 11. `CourseComplianceDto` Is Large For A DTO

Severity: low.

File:

- `src/main/java/br/com/ibmec/researchstars/report/dto/CourseComplianceDto.java`

Problem:

This DTO has grown to include derived values and compatibility aliases.

Why it matters:

The class itself is not broken, but it is a sign that frontend compatibility and domain reporting terms are mixed in one object.

Recommended fix:

Do not refactor until report JSON contract tests exist. If refactored later:

- Keep backward-compatible fields if frontend still uses them.
- Separate domain metric calculation from response serialization.

## Unused Code Candidates

These are candidates, not final deletion instructions:

| Candidate | Why It Looks Unused | Risk |
| --- | --- | --- |
| `course/dto/Course.java` | No obvious imports or references found. Name overlaps with entity. | Low if compile confirms. |
| `user/dto/User.java` | No obvious imports or references found. Name overlaps with entity. | Low if compile confirms. |

Before deleting:

```bash
rg "course.dto.Course|user.dto.User|new Course\\(|new User\\(" src/main/java src/test/java
mvn -Dmaven.repo.local=/tmp/irs-m2 test
```

## Complexity Hotspots

Manual hotspot ranking:

| Area | Risk | Reason |
| --- | --- | --- |
| `ProfessorService` | High | Too many business workflows in one class. |
| `AuthService.register` | Medium/high | Registration does validation, persistence, workflow creation, and response creation. |
| `ReportService.getCourseCompliance` | Medium | Nested course/professor loop and repeated count calls. |
| `RankingService.buildRankingEntries` | Medium | Duplicates report metric concept. |
| `PublicationService.findAll` | Medium | Per-publication professor lookup can become N+1. |
| `ProfessorPublicationsGateway` | Medium | Untyped `List<Object>` response. |

## DRY/KISS/Clean Code Assessment

### DRY

Good:

- DTO mappers exist in multiple domains.
- Controllers mostly delegate to services.
- Reporting window logic is already isolated in `ReportingWindowService`.

Needs improvement:

- Professor publication counts are calculated in multiple services.
- Some ownership/admin checks repeat in `PublicationService`.
- Error handling uses mixed exception types.

### KISS

Good:

- Most service methods are straightforward and readable.
- Course change workflow is explicit rather than hidden behind complex abstractions.
- The current code favors simple repositories and service methods.

Needs improvement:

- `ProfessorService` is becoming too broad for simple reasoning.
- Registration could be split into named steps.
- Untyped gateway responses make behavior less obvious than needed.

### Clean Code

Good:

- Naming is generally clear.
- Controllers are not doing heavy business logic.
- Tests cover recent business rules.

Needs improvement:

- Replace raw expected exceptions with domain exceptions.
- Avoid untyped `Object` API responses.
- Reduce repository calls inside DTO mapping.
- Use consistent DTO immutability patterns.

## Suggested Refactor Roadmap

### PR 1: Add Coverage Tooling

Purpose:

- Enable JaCoCo.
- Make coverage visible.
- Prepare for CRAP/cyclomatic analysis.

Behavior impact:

- None.

Suggested tests:

- Existing test suite only.

Verification:

```bash
mvn -Dmaven.repo.local=/tmp/irs-m2 test jacoco:report
```

### PR 2: Add Characterization Tests For Current API Contracts

Purpose:

- Protect behavior before refactoring internals.

Add or confirm tests for:

- Auth registration duplicate email.
- Auth registration duplicate Lattes URL.
- Auth registration invalid courses.
- Professor detail publications response shape.
- Admin publication list response shape.
- Error response format.
- Course compliance and ranking using the same default window.

Behavior impact:

- None.

### PR 3: Type The Professor Publications Response

Purpose:

- Replace `List<Object>` with a real DTO.
- Avoid entity leakage.

Behavior impact:

- Should be none if response JSON is preserved.

Tests first:

- Controller test for `GET /api/v1/professors/{id}` or whichever endpoint returns professor publications.
- Assert exact publication fields used by frontend.

### PR 4: Extract Professor Course Change Workflow

Purpose:

- Reduce `ProfessorService` responsibility.

Behavior impact:

- None.

Tests first:

- Existing professor course change tests.
- Add missing negative paths if not already covered.

Possible extraction:

- `ProfessorCourseChangeService`

### PR 5: Share Publication Metrics Between Ranking And Reports

Purpose:

- Make ranking/dashboard/report calculations impossible to diverge silently.

Behavior impact:

- None intended.

Tests first:

- Ranking and report tests around the same professor/publication fixture.
- Multi-course professor should not duplicate in ranking.
- Multi-course professor should count in each approved course.

Possible extraction:

- `ProfessorPublicationMetricsService`

### PR 6: Remove N+1 Publication Professor Lookup

Purpose:

- Improve admin publication list scalability.

Behavior impact:

- None.

Tests first:

- Publication list response contract.
- Filters and pagination still work.

Possible approach:

- Batch-load professor names by IDs for the current page.

### PR 7: Remove Confirmed Dead DTOs

Purpose:

- Reduce naming confusion.

Behavior impact:

- None.

Tests:

- Compile and full backend suite.

## Tests Recommended Before Any Behavior-Preserving Refactor

Add these only if missing:

1. `AuthControllerTest`:
   - duplicate email returns expected status and response body.
   - duplicate Lattes URL returns expected status and response body.
   - invalid course ID returns expected status and response body.

2. `ProfessorControllerTest`:
   - admin professor detail response includes current approved courses.
   - admin professor detail response includes pending request.
   - admin professor detail response includes publications in typed shape.

3. `PublicationControllerTest`:
   - admin publication list includes professor name.
   - professor cannot access another professor publication.
   - admin can access any publication.

4. `ReportServiceTest`:
   - pending course request does not affect course compliance.
   - approved multi-course professor counts in every approved course.
   - unapproved professor does not count.

5. `RankingServiceTest`:
   - multi-course professor appears once.
   - ranking and reports use the same reporting window.

6. Error response tests:
   - domain exceptions use consistent JSON shape.
   - expected validation/business errors do not become generic 500 responses.

## What Not To Change Yet

Do not change these until contract tests exist:

- Public endpoint paths.
- Public JSON field names.
- Professor approval workflow.
- Course request approval semantics.
- Ranking score semantics.
- Dashboard/report response DTO names.
- Publication validation status transitions.

Do not remove dependencies based only on `mvn dependency:analyze`.

Do not enforce a high global coverage threshold immediately after adding JaCoCo.

## Overall Health

Current state:

- Build: healthy.
- Test suite: green.
- Architecture: service/controller/repository layering is mostly respected.
- Biggest weakness: measurement and maintainability around professor/course/reporting workflows.

The codebase is in a good position for incremental cleanup. The safest order is:

1. Add coverage tooling.
2. Add characterization tests.
3. Remove untyped/unused code.
4. Extract duplicated business concepts.
5. Tighten static analysis once baseline noise is understood.

