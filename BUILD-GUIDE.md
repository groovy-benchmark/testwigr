# Build Guide

This guide explains how to build Testwigr from source on any machine (Linux, macOS or Windows, x86_64 or ARM64).

## TL;DR

```bash
# 1. Make sure JDK 21 is installed and active (see "Installing JDK 21" below)
java -version          # should print 21.x

# 2. Build (skipping tests)
./gradlew clean build -x test        # Linux / macOS
gradlew.bat clean build -x test      # Windows (cmd / PowerShell)

# 3. Result
ls build/libs/testwigr-0.0.1-SNAPSHOT.jar
```

No database, Docker or network service is needed to **build** the project. MongoDB (usually via Docker) is only needed to **run** the application and the tests.

## 1. Technology stack

| Component     | Version                              | Notes                                              |
|---------------|--------------------------------------|----------------------------------------------------|
| Java (JDK)    | **21**                               | Required by the Gradle toolchain in `build.gradle` |
| Gradle        | 8.13 (via the included wrapper)      | Do not use a system-wide Gradle install            |
| Groovy        | 4.0.25 (resolved by Gradle)          | No separate install needed                         |
| Spring Boot   | 3.4.3                                |                                                    |
| Database      | MongoDB                              | Runtime and tests only, not needed for the build   |
| Build output  | Executable Spring Boot jar           | `build/libs/testwigr-0.0.1-SNAPSHOT.jar`           |

**About the Java version:** `build.gradle` declares a Java **toolchain** of version 21 (`JavaLanguageVersion.of(21)`). Gradle compiles with a JDK 21 that it finds on the machine, and it does **not** download one automatically (no toolchain resolver is configured). So a JDK 21 must be installed. The simplest setup is to make JDK 21 the Java that runs Gradle (`JAVA_HOME`). Gradle 8.13 itself can run on JDK 17 to 23; it cannot run on JDK 24 or newer.

## 2. Prerequisites

1. **JDK 21.** A full JDK is required, a JRE is not enough. Any vendor works (Eclipse Temurin, Azul Zulu, Amazon Corretto, Microsoft Build of OpenJDK, Oracle, OpenJDK from your OS package manager, etc.).
2. **Internet access** (at least for the first build) to:
   - `services.gradle.org` (Gradle 8.13 distribution, only once)
   - `plugins.gradle.org` (Gradle plugins)
   - `repo.maven.apache.org` (Maven Central, dependencies)
3. **About 1 GB of free disk space** for Gradle and the dependency cache.
4. **Git**, to clone the repository.
5. **Optional, to run the app or the tests:** Docker with Docker Compose v2 (`docker compose`), or any MongoDB reachable on `localhost:27017`.

You do **not** need Gradle or Groovy installed.

## 3. Installing JDK 21

Pick the option for your platform. After installing, open a **new** terminal and check with `java -version` (it should report `21.x`).

### Any platform: SDKMAN! (Linux, macOS, WSL)

```bash
curl -s "https://get.sdkman.io" | bash
sdk list java | grep -E "21\.[0-9.]+"       # pick an available JDK 21 identifier
sdk install java <identifier>               # e.g. 21.x.x-tem
sdk use java <identifier>
```

### Linux

- **Debian / Ubuntu:** `sudo apt install openjdk-21-jdk`
- **Fedora / RHEL / CentOS:** `sudo dnf install java-21-openjdk-devel`
- **Arch:** `sudo pacman -S jdk21-openjdk`
- **Manual tarball:** download a JDK 21 `.tar.gz` for your architecture (x64 or aarch64) from <https://adoptium.net>, extract it anywhere and point `JAVA_HOME` to the extracted folder.

If several JDKs are installed, select JDK 21 with `sudo update-alternatives --config java` (Debian/Ubuntu), `sudo alternatives --config java` (Fedora/RHEL), or simply set `JAVA_HOME` for the build (see below).

### macOS (Intel and Apple Silicon)

```bash
brew install --cask temurin@21
/usr/libexec/java_home -V                       # list installed JDKs
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

### Windows

- **winget:** `winget install EclipseAdoptium.Temurin.21.JDK`
- **Chocolatey:** `choco install temurin21`
- **Manual:** download the JDK 21 `.msi` from <https://adoptium.net> and enable the "Set JAVA_HOME" option in the installer.

### Using JDK 21 only for this build

You don't have to make JDK 21 your default Java. Setting `JAVA_HOME` in the current terminal is enough:

```bash
# Linux / macOS
export JAVA_HOME=/path/to/jdk21
export PATH="$JAVA_HOME/bin:$PATH"
```

```powershell
# Windows PowerShell
$env:JAVA_HOME = "C:\path\to\jdk21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

```bat
:: Windows cmd
set JAVA_HOME=C:\path\to\jdk21
set PATH=%JAVA_HOME%\bin;%PATH%
```

## 4. Building

From the project root:

```bash
# Linux / macOS (if needed, make the wrapper executable first: chmod +x gradlew)
./gradlew clean build -x test
```

```bat
:: Windows
gradlew.bat clean build -x test
```

The first build downloads Gradle 8.13 and all dependencies, which takes a few minutes. Later builds take seconds.

**Expected output:**

```
> Task :compileGroovy
> Task :bootJar
...
> Task :build

BUILD SUCCESSFUL
```

**Artifacts** in `build/libs/`:

| File                                 | Description                                                    |
|--------------------------------------|----------------------------------------------------------------|
| `testwigr-0.0.1-SNAPSHOT.jar`        | Executable Spring Boot jar with all dependencies (about 45 MB) |
| `testwigr-0.0.1-SNAPSHOT-plain.jar`  | Plain jar without dependencies (can be ignored)                |

**Important:** always pass `-x test` (or `-PskipMongo`, see section 6) when you only want to build. Without it, `./gradlew build` runs the tests, and the `test` task first runs `docker compose ... up` to start a test MongoDB, so it fails on machines without Docker.

### Useful Gradle tasks

| Command                          | What it does                                             |
|----------------------------------|----------------------------------------------------------|
| `./gradlew clean build -x test`  | Full build without running tests (recommended)           |
| `./gradlew bootJar`              | Build the executable jar only                            |
| `./gradlew compileGroovy`        | Compile main sources only (fast compile check)           |
| `./gradlew test`                 | Start a test MongoDB with Docker and run all tests       |
| `./gradlew unitTest`             | Run tests excluding `*IntegrationSpec` / `*IntegrationTest` |
| `./gradlew bootRun`              | Run the application from sources                         |
| `./gradlew dependencies`         | Print the resolved dependency tree                       |

## 5. Running the application

The application expects MongoDB on `localhost:27017` (database `testwigr`), as configured in `src/main/resources/application.properties`.

```bash
# 1. Start MongoDB (Docker)
docker compose -f docker/docker-compose.yml up -d mongodb

# 2. Run the app
java -jar build/libs/testwigr-0.0.1-SNAPSHOT.jar
# or, from sources:
./gradlew bootRun
```

- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>

Configuration can be overridden with environment variables using Spring Boot's relaxed binding, for example `SPRING_DATA_MONGODB_HOST`, `SPRING_DATA_MONGODB_PORT` and `APP_JWT_SECRET`. A template for local values is provided in `.env.template` (copy it to `.env`, which is git-ignored).

To run the whole stack (MongoDB, Mongo Express and the app) with Docker:

```bash
docker compose -f docker/docker-compose.yml up -d
```

See `README.md` for API usage.

## 6. Running the tests

The tests need MongoDB. The `test` task starts one automatically with Docker Compose (`docker/docker-compose-test.yml`) and stops it at the end:

```bash
./gradlew test
```

- **Docker must be running**, and Docker Compose v2 must be available as `docker compose`.
- If you already have a MongoDB on `localhost:27017`, skip the automatic Docker start with `./gradlew test -PskipMongo`. Without Docker and without a MongoDB, the tests that use the database fail (after connection timeouts, which can take several minutes).
- To produce a report even when some tests fail: `./gradlew test -PignoreTestFailures=true`.
- Report: `build/reports/tests/test/index.html`.

The helper scripts `scripts/start-test-db.sh` and `scripts/stop-test-db.sh` (Linux / macOS) do the same as the automatic start/stop.

## 7. Building with Docker

`docker/Dockerfile` builds the jar inside a container (`eclipse-temurin:21-jdk-alpine`), so you don't need a local JDK:

```bash
docker build -f docker/Dockerfile -t testwigr .
```

The base images are multi-arch (amd64 and arm64). The image starts the app with the `prod` profile and still needs a MongoDB to connect to.

## 8. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `Could not find or load main class org.gradle.wrapper.GradleWrapperMain` | `gradle/wrapper/gradle-wrapper.jar` is missing | Make sure your checkout contains `gradle/wrapper/gradle-wrapper.jar`, then pull or re-clone. |
| `Cannot find a Java installation on your machine matching this tasks requirements: {languageVersion=21, ...}` / `No matching toolchains found` | No JDK 21 installed, or Gradle cannot detect it | Install JDK 21 (section 3) and point `JAVA_HOME` to it. If it is installed in a custom location, add `org.gradle.java.installations.paths=/path/to/jdk21` to `~/.gradle/gradle.properties`. |
| `Unsupported class file major version ...` or other errors at Gradle startup | Gradle 8.13 is running on a JDK that is too new (24+) | Set `JAVA_HOME` to JDK 21. Check with `./gradlew -version`, which prints the JVM being used. |
| `JAVA_HOME is set to an invalid directory` | `JAVA_HOME` points to a non-existent folder or to a JRE | Point it to the root folder of a JDK 21 (the folder that contains `bin/javac`). |
| `Execution failed for task ':startTestMongoDB'` / `Cannot connect to the Docker daemon` | `./gradlew build` or `./gradlew test` tried to start MongoDB with Docker | To only build, use `./gradlew build -x test`. To test, start Docker, or use `-PskipMongo` with your own MongoDB. |
| `unknown shorthand flag: 'f' in -f` or `docker: 'compose' is not a docker command` | Only the old `docker-compose` v1 is installed | Install Docker Compose v2 (included in current Docker Desktop / Docker Engine packages). |
| App starts but logs `MongoSocketOpenException` / `Connection refused` | No MongoDB on `localhost:27017` | Start it (section 5) or set `SPRING_DATA_MONGODB_HOST` / `SPRING_DATA_MONGODB_PORT`. |
| `Could not resolve all dependencies ...` / connection timeouts | No access to Maven Central, the Gradle Plugin Portal or `services.gradle.org` | Check internet / firewall. Behind a proxy, add `systemProp.https.proxyHost`, `systemProp.https.proxyPort` (and the `http.` variants) to `~/.gradle/gradle.properties`. |
| `FileNotFoundException: ~/.gradle/wrapper/dists/gradle-8.13-bin/.../gradle-8.13-bin.zip` | Corrupted or partial Gradle distribution in the local cache | Delete `~/.gradle/wrapper/dists/gradle-8.13-bin` (Windows: `%USERPROFILE%\.gradle\wrapper\dists\gradle-8.13-bin`) and build again. |
| `Permission denied: ./gradlew` | The execute bit was lost (e.g. ZIP download, Windows checkout) | `chmod +x gradlew` |
| `/bin/sh^M: bad interpreter` | `gradlew` was checked out with Windows (CRLF) line endings | `git config core.autocrlf input` and re-checkout, or run `sed -i 's/\r$//' gradlew`. |
| `Port 8080 was already in use` when running the app | Another process is using port 8080 | Stop that process, or run with `--server.port=8081`. |

## 9. Build-related changes made to this repository

The source code and the build configuration were already correct; no source or `build.gradle` change was needed. The only change:

1. **Added `gradle/wrapper/gradle-wrapper.jar`** (the official Gradle 8.13 wrapper, SHA-256 `81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f`, matching <https://services.gradle.org/distributions/gradle-8.13-wrapper.jar.sha256>). It was missing, so `./gradlew` could not start.
