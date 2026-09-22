# CLAUDE.md

## Running commands

Permission rules are evaluated per subcommand: in `a && b` or `a | b`, every
part must match a rule on its own. A denial of a compound command usually means
one part is not allowed, not the part you were aiming at. Run one command per
call instead of chaining, and read the file rather than piping it through `tail`.

## Project

REST API for an online store: customers, products and orders.
Classic layered Spring Boot architecture, based on *Spring Boot: Arquitectura de
Back End* (Benedettelli, 2022), updated from Spring Boot 2.7 to 4.x and extended
with testing, transactions, DTOs and structured error handling.

## Stack

Java 21 · Spring Boot 4.x · Maven (`./mvnw`) · Spring Web · Spring Data JPA ·
Bean Validation · H2 (dev) / PostgreSQL · springdoc-openapi ·
JUnit 5, AssertJ, Mockito, Testcontainers

Modern APIs only:

| Legacy (Boot 2.x) | Use instead |
|---|---|
| `javax.*` | `jakarta.*` |
| Field `@Autowired` | Constructor injection, `final` fields |
| `RestTemplate` | `RestClient` |
| `@MockBean` | `@MockitoBean` |
| Lombok `@Data` DTOs | `record` |

## Layers

```
controller/   HTTP in, DTOs out. No business logic.
service/      Business rules and transactions. No HTTP types.
repository/   Spring Data interfaces.
domain/       JPA entities.
dto/          Request and response records.
exception/    Business exceptions and global handler.
config/       @Configuration classes.
```

- Entities never cross the controller boundary: DTOs in, DTOs out.
- `@Transactional` lives in the service layer.
- Errors are returned as `ProblemDetail` (RFC 7807).

## Javadoc

Every bean documents **why it is a bean**:

1. Which stereotype it uses and why that one.
2. Its scope and what that implies (singleton → no mutable state).
3. Its dependencies and how they are injected.
4. Its role in the request flow.

Public controller and service methods document `@param`, `@return` and `@throws`.
Non-obvious Spring annotations (`@Transactional(readOnly = true)`, `@Lazy`,
`@Primary`, `@Qualifier`) state why they are there.

## Tests

Written alongside the code. `@WebMvcTest` for controllers, plain unit tests with
Mockito for services, Testcontainers for integration.

## Commits

Conventional commits, imperative mood, lowercase, no trailing period, ≤ 72 chars:

```
feat(customer): add create and find endpoints

Optional body explaining why.

Refs #12
```

Types: `feat` `fix` `chore` `docs` `test` `refactor` `perf` `ci` `build`.
`Refs #N` in commits, `Closes #N` in the PR description.
