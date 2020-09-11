---
# Feel free to add content and custom Front Matter to this file.
# To modify the layout, see https://jekyllrb.com/docs/themes/#overriding-theme-defaults

layout: default
title: Developer Setup
nav_order: 1
has_children: false
parent: Home
---

# Developer Setup

Carnival is a Groovy multi-project that uses Gradle as the build engine. The main project is in the app directory. Folders within app contain the sub-projects (eg. app/public/carnival-core, app/public/carnival-gremlin-dsl). Every sub-project has a build.gradle configuration file that defines its dependencies and the gradle tasks that it can execute. The build.gradle file in the app directory defines project-wide configuration. Gradle task commands run from the root project filter down to the sub-projects. Gradle commands referenced in this documentation are assumed to be called from the root project directory carnival/app unless otherwise noted. This project also includes a Docker image configuration that can be built to run Carnival or the test suite.

## Installation

### Build Requirements

-   Gradle V5+
-   Installation with a package manager
-   SDKMan
-   Windows: Cygwin + SDKMan
-   Note - Cygwin needs curl installed for sdkman to work successfully. The windows 10 version of curl may cause an error when trying to install gradle. See stack overflow.
-   Windows: Scoop
-   Install Manually
    -   Java V1.8+
    -   Neo4j V3.5+ (Optional) - Helpful to browse the property graph. Desktop Community Server.

### Environment Setup

1. Clone the repository using:

```
git clone git@github.com:pennbiobank/carnival-public.git
```

2. Create a carnival home directory. This directory will contain your configuration files and will be the location where carnival reads and writes graphs and files.

```
mkdir /Users/myuser/dev/carnival/carnival_home
```

3. Make carnival aware of your carnival home directory. There are a couple of ways you can do this.

-   Use the environment variable CARNIVAL_HOME. For example:

```
export CARNIVAL_HOME=/Users/myuser/dev/carnival/carnival_home
```

-   Pass your carnival home directory to future gradle commands using the -D syntax:

```
-Dcarnival.home=/Users/myuser/dev/carnival/carnival_home
```

-   **Note** - If you use this method, then you will need to edit logback.xml, which by default references CARNIVAL_HOME.

4. Setup the configuration files in the `${CARNIVAL_HOME}/config` directory:

-   Copy the config template files from `carnival/config` to `${CARNIVAL_HOME}/config`.
-   Rename the files named `*.*-template` to `*.*` (remove -template from the name).
-   **Note** - In the config files windows paths should be specified using double forward-slashes (i.e. `C://Users//myuser//somedirectory`).
-   File descriptions:
-   application.yaml - Contains data source information (i.e. credentals to relational dbs, RDF dbs, REDCap, etc.), the default vine cache-mode, local directory configuration and the gremlin configuration.
-   logback.xml - Can be modified to change the log levels.

5. Install APOC neo4j plugin:
   Download the APOC V3.5.0.7 and save it to a directory on your local file system.
   Add the path to that directory in gremlin section of the application.yaml config file in the entry gremlin:neo4j:conf:dbms:directories:plugins:

```yaml
# gremlin
gremlin:
    neo4j:
        conf:
            dbms:
                directories:
                    plugins: /Users/myuser/Documents/Neo4j/default.graphdb/plugins
                security:
                    auth_enabled: "false"
                    procedures:
                        unrestricted: apoc.*
                        whitelist: apoc.*
```

## Building Overview

Carnival is a Gradle multi-project application comprised of sub-projects. Some sub-projects implement the data model and shared core functionality (carnival-core, carnival-util, carnival-graph, carnival- gremlin-dsl, etc.) and some extend the general data model to the biomedical domain (carnival-clinical). Every sub-project has a build.gradle configuration file that defines it's dependencies and the gradle tasks it can execute. The build.gradle file in the carnival/app directory defines project-wide configuration. These files also contain the default java arguments that are used for each task.

### Compiling

Carnival can be built by running gradle compileGroovy . The build can be cleaned by running gradle clean . To run sub-project tasks, use the gradle colon syntax: gradle :carnival- util:compileGroovy .

### Testing

Gradle is used to execute the test suite. Running tests produces html test result files in the sub-project directories `carnvial\app\carnival-\*\build\reports\tests\test\index.html` .

### Aggregating Test Results

Running the command gradle testReport will run all tests and generate aggregated results in `carnival\app\build\reports\allTests` .

### Common Test Commands

To run tests for all gradle sub-projects: `gradle test`
To run tests for all gradle sub-projects and aggrigate the results: `gradle testReport`
To run tests in a specific gradle sub-project: `gradle :carnival-util:test`
To run a specific test suite, in this example the tests located in `carnival\app\carnival-graph/src/test/groovy/carnival/graph/VertexDefTraitSpec.groovy` :
`gradle :carnival-graph:test --tests "carnival.graph.VertexDefTraitSpec"`

### HTTP Tests

Some of the tests require external HTTP resources. To run these tests:
`gradle -Dtest-http=true :carnival-core:test`

### Running Tests using Docker

The test suite can be run in the context of a docker image. If running tests in this way gradle does not need to be installed, and any configuration in the users CARNIVAL_HOME directory will be ignored.

## Requirements

### Docker

Most recent stable release, minimum version is 17.06.0
Official Docker Website Getting Started
Official Docker Installation for Windows
Docker-Compose (Version 1.22.0 or greater, Linux only) - Separate installation is only needed for linux, docker-compose is bundled with windows and mac docker installations Linux Docker-Compose Installation

### Running Tests in the Docker Environment

First build the docker image using the command: `docker-compose -f .\docker-compose-test.yml build`
Once built, the tests can be run using the command: `docker-compose -f .\docker-compose-test.yml up --force-recreate`
This has the same effect as running gradle testReports, and the aggregated test results will be in the folder `carnival\app\build\reports\allTests` .

### Publishing Libraries to Local Maven

The Groovy sub-project modules can be published to local maven repositories by running commands like the following:

```
gradle :carnival-util:publishToMavenLocal
gradle :carnival-core:publishToMavenLocal
```

To publish all modules:

```
gradle publishAll
```