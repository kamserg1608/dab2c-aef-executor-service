# Router executor project!!

### Build

#### Prerequisites

Maven Wrapper is used to build this project.

In order to download Apache Maven distribution,
Maven Wrapper needs Nexus CI credentials to be specified in your **~/.mavenrc**:

```
export MVNW_USERNAME=<your nexus ci token>
export MVNW_PASSWORD=<your nexus ci token password>
```

If you face an error below:
```
PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target 
```

You can copy [config/cacerts](config/cacerts) to some folder, and add the following lines to your **~/.mavenrc**:
```
export MAVEN_OPTS="$MAVEN_OPTS -Djavax.net.ssl.trustStore=<path to your folder>/cacerts -Djavax.net.ssl.trustStorePassword=changeit"
```

#### Building

For that purpose you can run Maven Wrapper from root folder of the project:

```bash
./mvnw clean install
```

