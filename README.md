# Search
[![Dependabot Status](https://api.dependabot.com/badges/status?host=github&repo=connexta/ion-search)](https://dependabot.com)
[![Known Vulnerabilities](https://snyk.io/test/github/connexta/ion-search/badge.svg)](https://snyk.io/test/github/connexta/ion-search)
[![CircleCI](https://circleci.com/gh/connexta/ion-search/tree/master.svg?style=svg)](https://circleci.com/gh/connexta/ion-search/tree/master)

## Prerequisites
* Java 11
* Docker daemon

## Working with IntelliJ or Eclipse
This repository uses [Lombok](https://projectlombok.org/), which requires additional configurations and plugins to work in IntelliJ/Eclipse.
Follow the instructions [here](https://www.baeldung.com/lombok-ide) to set up your IDE.

## Building
To just compile and build the projects:
```bash
./gradlew assemble
```
To do a full build with tests and the formatter:
```bash
./gradlew build
```

### Build Checks
#### OWASP
```bash
./gradlew dependencyCheckAnalyze --info
```
The report for each project can be found at build/reports/dependency-check-report.html.

#### Style
The build can fail because the static analysis tool, Spotless, detects an issue. To correct most Spotless issues:
```bash
./gradlew spotlessApply
```

For more information about spotless checks see
[here](https://github.com/diffplug/spotless/tree/master/plugin-gradle#custom-rules).

#### Tests
* Tests are run automatically with `./gradlew build`.
* To skip all tests, add `-x test`.
* Even if the tests fail, the artifacts are built and can be run.
* To change logging to better suit parallel builds pass `-Pparallel` or the `--info` flag
* To run a single test suite:
    ```bash
    ./gradlew test --tests TestClass
    ```

##### Integration Tests
* The integration tests require a Docker daemon.
* To skip integration tests, add `-PskipITests`.

##### Code Coverage
Jacoco provides code coverage under `build/reports/jacoco`. Jacoco tasks are run as part of the test task.

## Running
### Configuring
A Docker network named `cdr` is needed to run via docker-compose.

Determine if the network already exists:
```bash
docker network ls
```
If the network exists, the output includes a reference to it:
```bash
NETWORK ID          NAME                DRIVER              SCOPE
zk0kg1knhd6g        cdr                 overlay             swarm
```
If the network has not been created:
```bash
docker network create --driver=overlay --attachable cdr
```

### Running Locally via `docker stack`
```bash
docker stack deploy -c docker-compose.yml search-stack
```

#### Helpful `docker stack` Commands
* To stop the Docker service:
    ```bash
    docker stack rm search-stack
    ```
* To check the status of all services in the stack:
    ```bash
    docker stack services search-stack
    ```
* To stream the logs to the console for a specific service:
    ```bash
    docker service logs -f <service_name>
    ```

### Running in the Cloud
There are two ways to configure the build system to deploy the service to a cloud:
- Edit the `deploy.bash` file. Set two variables near the top of the file:
  - `SET_DOCKER_REG="ip:port"`
  - `SET_DOCKER_W="/path/to/docker/wrapper/"`

OR

- Avoid editing a file in source control by exporting values:
    ```bash
    export DOCKER_REGISTRY="ip:port"
    export DOCKER_WRAPPER="/path/to/docker/wrapper/"
    ```

After configuring the build system:
```bash
./gradlew deploy
```

## Using
### Querying
The Search service can be queried for datasets. See [Query Service API](https://github.com/connexta/ion-query-api) for information about paths and parameters.
The Search service supports the OGC Catalogue Common Query Language (OGC CommonQL).
See [Annex B - BNF Definition of OGC CommonQL](http://docs.opengeospatial.org/is/12-168r6/12-168r6.html#62) for the definition of the grammar.

The following are valid query attributes:

| Attribute | Description  |
|---|---|
| `contents` | A client can perform keyword queries with this attribute |
| `country_code` | GENC:3:3-7 trigraph indicating a country |
| `created` | The date the File was created |
| `expiration` | The date past which the information in the Dataset should not be consumed |
| `icid` | The Information Community Identification of the File |
| `id` | The unique ID that is associated with each Dataset |
| `keyword` | The topic of the Dataset |
| `modified` | The date the File was last modified |
| `resource_uri` | Location of the File |
| `title` | Name given to the Dataset |

### Inspecting
The service is deployed with (Springfox) **Swagger UI**.
This library uses Spring Boot annotations to create documentation for the service endpoints.
The `/swagger-ui.html` endpoint can be used to view Swagger UI.
The service is also deployed with Spring Boot Actuator.
The `/actuator` endpoint can be used to view the Actuator.
