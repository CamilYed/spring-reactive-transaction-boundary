# Releasing

This document describes the release process for Spring Reactive Transaction Boundary.

## Prerequisites

Before creating a release, make sure the following requirements are met:

- JDK 21 is available locally and in GitHub Actions.
- Docker is available for Testcontainers-based integration tests.
- The Sonatype Central Portal namespace for `io.github.camilyed` is verified.
- The public PGP key used for signing is uploaded to a keyserver supported by Central Portal.
- GitHub Actions repository secrets are configured:
  - `CENTRAL_USERNAME`
  - `CENTRAL_PASSWORD`
  - `SIGNING_KEY`
  - `SIGNING_PASSWORD`

The Sonatype credentials are Central Portal user-token credentials. They are separate from the GPG
signing key used to sign Maven artifacts.

## Release flow

The release workflow is intentionally conservative by default.

The GitHub Actions `Release` workflow supports two Central Portal publishing modes:

| Mode | Behavior |
| --- | --- |
| `USER_MANAGED` | CI uploads the signed bundle to Central Portal and waits until validation passes. The final `Publish` click is manual in Central Portal. |
| `AUTOMATIC` | CI uploads the signed bundle to Central Portal and Central Portal publishes it automatically after validation succeeds. |

For the first public release, prefer `USER_MANAGED`.

## Local verification

Run the full root build:

```bash
./gradlew spotlessCheck clean build jacocoTestReport
```

Run the standalone demo application tests:

```bash
cd examples/spring-boot-webflux-r2dbc-ddd-demo
./gradlew clean test --refresh-dependencies
cd ../../..
```

The demo is a standalone Gradle project and consumes the published artifacts like an external
application.

## Build a signed local staging repository

To verify release artifacts locally, run:

```bash
./gradlew clean build jacocoTestReport
./gradlew publishAllPublicationsToLocalBuildRepository -PreleaseVersion=0.1.0
```

The release staging repository should be created under:

```text
build/staging-deploy
```

Verify that release artifacts do not contain `SNAPSHOT` in their paths:

```bash
find build/staging-deploy -path "*SNAPSHOT*" -print
```

The command should print nothing for a release build.

Verify that signatures were generated:

```bash
find build/staging-deploy -name "*.asc" -type f | sort
```

## Build the Central Portal bundle locally

Use the same packaging command as CI:

```bash
rm -f build/central-bundle-0.1.0.zip

cd build/staging-deploy
find io -type f | LC_ALL=C sort | zip -@ ../central-bundle-0.1.0.zip
cd ../..
```

Verify the bundle:

```bash
unzip -Z1 build/central-bundle-0.1.0.zip | head -40
unzip -Z1 build/central-bundle-0.1.0.zip | grep -Eq '(^\./?$|/$)' && echo "BAD" || echo "OK"
unzip -Z1 build/central-bundle-0.1.0.zip | grep SNAPSHOT
```

Expected result:

- entries start with `io/github/camilyed/...`,
- the root/directory-entry check prints `OK`,
- the `SNAPSHOT` check prints nothing.

## GitHub Actions release

Use the manual `Release` workflow in GitHub Actions.

Inputs:

```text
version: 0.1.0
publishing_type: USER_MANAGED
```

The workflow:

1. builds and tests the project,
2. signs all Maven publications,
3. publishes artifacts to a local staging repository,
4. creates a Central Portal bundle without directory entries,
5. uploads the bundle as a GitHub Actions artifact,
6. uploads the bundle to Central Portal through the Publisher API,
7. waits for Central Portal validation.

When `publishing_type` is `USER_MANAGED`, the workflow finishes after Central Portal reaches
`VALIDATED`.

When `publishing_type` is `AUTOMATIC`, the workflow waits until Central Portal reaches `PUBLISHED`.

## Publish from Central Portal

For the first release, use `USER_MANAGED`.

After the workflow finishes successfully:

1. open Sonatype Central Portal,
2. inspect the deployment,
3. confirm that all components are validated,
4. click `Publish`.

Do not click `Drop` unless the deployment should be discarded.

After `Publish`, the version is immutable. A failed or flawed public release must be fixed with a
new version such as `0.1.1`.

## Tag the release

After the Maven Central deployment is published, tag the release from `main`:

```bash
git checkout main
git pull

git tag -a v0.1.0 -m "Release 0.1.0"
git push origin v0.1.0
```

## GitHub Release

Create a GitHub Release from tag:

```text
v0.1.0
```

Use the `CHANGELOG.md` entry as the release notes.

## Post-release verification

After Maven Central sync is complete, verify the release from a consumer project.

For the demo application, use:

```kotlin
implementation("io.github.camilyed:reactive-transaction-spring-boot-starter:0.1.0")
```

Then run:

```bash
cd examples/spring-boot-webflux-r2dbc-ddd-demo
./gradlew clean test --refresh-dependencies
cd ../../..
```

## Snapshot publishing

Snapshots are published separately from releases.

Use the snapshot repository only for `*-SNAPSHOT` versions:

```text
https://central.sonatype.com/repository/maven-snapshots/
```

A fresh snapshot can be published with:

```bash
./gradlew publishAllPublicationsToCentralSnapshotsRepository
```

When consuming snapshots, use:

```bash
./gradlew clean test --refresh-dependencies
```

## Troubleshooting

### No signing key configured

If Gradle reports that no signatory is available, verify that one of these is set:

- Gradle property `signingKey`
- environment variable `SIGNING_KEY`

For GitHub Actions, check the `SIGNING_KEY` repository secret.

### Wrong signing password

If signing fails or generated signatures are invalid, verify `SIGNING_PASSWORD`.

This must be the passphrase for the GPG private key stored in `SIGNING_KEY`.

### Central Portal cannot find the public PGP key

If Central Portal reports that it cannot find a public key by fingerprint, upload the public key:

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys FCDB615A724BCF7F4B7C7E195AD19B891B73F9D5
```

Then wait a few minutes and retry the release workflow.

### Sonatype authentication fails

If upload or publishing fails with authentication errors, generate a new Central Portal user token
and update:

- `CENTRAL_USERNAME`
- `CENTRAL_PASSWORD`

These values are token credentials, not the normal login password.

### Snapshot artifacts in a release bundle

If `build/staging-deploy` contains `SNAPSHOT`, the release version was not passed correctly.

Use:

```bash
./gradlew publishAllPublicationsToLocalBuildRepository -PreleaseVersion=0.1.0
```

### Bundle contains `./`

Central Portal rejects bundles that contain a root `./` entry or directory entries.

Use:

```bash
cd build/staging-deploy
find io -type f | LC_ALL=C sort | zip -@ ../central-bundle-0.1.0.zip
cd ../..
```
