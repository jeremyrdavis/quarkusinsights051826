# Local Setup

Set up the workshop on your own laptop. Should take 5–15 minutes depending on what you already have installed.

## Prerequisites

| Tool | Version | How to check |
|---|---|---|
| **JDK** | 25 (or newer) | `java -version` |
| **Docker** | Any recent | `docker info` |
| **Git** | Any recent | `git --version` |

Maven comes bundled — every workshop project ships with `./mvnw` (Linux/macOS) and `./mvnw.cmd` (Windows), so you don't need a separate Maven install.

### JDK 25

The starter project uses Java 25. If your `java -version` shows something older, install JDK 25 from any of:

- **SDKMAN** (recommended for Linux/macOS):
  ```bash
  sdk install java 25-tem
  sdk use java 25-tem
  ```
- **Homebrew** (macOS): `brew install openjdk@25`
- **Direct download**: <https://adoptium.net/temurin/releases/?version=25>

If your IDE uses a different JDK, point it at JDK 25 explicitly (IntelliJ: Project Structure → Project SDK; VS Code: `java.configuration.runtimes` in settings).

### Docker

Quarkus Dev Services automatically spins up a PostgreSQL container for development and tests. You don't need to install or configure Postgres yourself — but Docker has to be running.

- **macOS / Windows**: Docker Desktop
- **Linux**: Docker Engine + `docker compose` plugin

Verify with `docker info` — if you see a list of containers/networks, you're good. If you see `Cannot connect to the Docker daemon`, start Docker first.

## Clone and verify

```bash
# Clone the parent repo (this workshop is a subdirectory)
git clone https://github.com/jeremyrdavis/QuarkusInsights.git
cd QuarkusInsights/workshop/01-Episode-End-to-End/module-01-code

# Sanity-check the starter
./mvnw test
```

The first run will download a lot of Maven dependencies — expect 1–3 minutes. After that, Dev Services pulls the Postgres image (~80 MB) on its first use; subsequent runs reuse the cached image.

You should see something like:

```
[INFO] Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Specifically, `HealthCheckSmokeTest` should report green. That's the one test the starter ships with — it confirms Quarkus boots, Dev Services brings up PostgreSQL, and the health endpoint returns `UP`.

If you see `BUILD SUCCESS`: **you're ready.** Open `workshop/01-Episode-End-to-End/01-Value-Objects.md` and start Step 1.

## Optional: dev mode

You don't need this to do the workshop, but it's worth knowing.

`./mvnw quarkus:dev` starts the app with **live reload** — change a file, see the change applied without restart. In the dev-mode terminal:

- `r` — rerun tests (continuous testing)
- `o` — toggle test output
- `h` — show all dev-mode commands

The Dev UI at <http://localhost:8080/q/dev/> shows your endpoints, datasource, CDI beans, and configuration.

## Troubleshooting

**`./mvnw: command not found` or `Permission denied`**

Make the wrapper executable: `chmod +x mvnw`. On Windows, use `mvnw.cmd` instead.

**`Could not find suitable JDK` or version mismatches**

If `./mvnw` runs but compiles fail, check `./mvnw -version` — it should show "Java version: 25" somewhere. If not, set `JAVA_HOME` to your JDK 25 install:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)   # macOS
# or wherever your JDK 25 lives
```

**`Could not pull image docker.io/library/postgres:18`**

Check your Docker is logged in and has network access, then `docker pull postgres:18` manually to confirm. Some corporate networks block Docker Hub — work with your team if so.

**Port 8080 already in use**

Either kill the process using port 8080, or override the port:

```bash
./mvnw quarkus:dev -Dquarkus.http.port=8090
```

**Tests fail with `Connection refused: PostgreSQL`**

Dev Services failed to start PostgreSQL. Run `docker ps` — if no Postgres container is running, check Docker is up. If a Postgres container *is* running but tests still fail, kill it (`docker rm -f <id>`) and let Quarkus restart it fresh.

**Slow first run**

Maven downloading dependencies (~1–3 min on first invocation) and Docker pulling images (~30 sec for `postgres:18`). Both are cached after the first run.

## Alternative: GitHub Codespaces

If you don't want to install anything locally, see [GitHub-Codespaces.md](GitHub-Codespaces.md) for a browser-only setup.
