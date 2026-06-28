# Releasing

This document describes the release process for Spring Reactive Transaction Boundary.

## Prerequisites

Before creating a release, make sure the following requirements are met:

- JDK 21 is available locally and in GitHub Actions.
- Docker is available for Testcontainers-based integration tests.
- The Sonatype Central Portal namespace for `io.github.camilyed` is verified.
- GitHub Actions repository secrets are configured:
	- `CENTRAL_USERNAME`
	- `CENTRAL_PASSWORD`
	- `SIGNING_KEY`
	- `SIGNING_PASSWORD`

The Sonatype credentials are Central Portal user-token credentials. They are separate from the GPG
signing key used to sign Maven artifacts.

## Release flow

The first release flow is intentionally conservative:

1. Prepare the release metadata in a pull request.
2. Build a signed Central Portal bundle in GitHub Actions.
3. Download and manually upload the generated bundle to Sonatype Central Portal.
4. Publish the validated deployment in Sonatype Central Portal.
5. Tag the release in Git.
6. Create a GitHub Release.

This avoids publishing directly from CI until the manual Central Portal process is verified at least
once.

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

The demo is a standalone Gradle project and consumes the published snapshot or release artifacts like
an external application.

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

## GitHub Actions release bundle

Use the manual `Release` workflow in GitHub Actions.

Input:

```text
0.1.0
```

The workflow should:

1. check out the repository,
2. set up JDK 21,
3. run the root build and test suite,
4. publish all Maven publications to the local staging repository,
5. sign Maven artifacts,
6. create a Central Portal bundle,
7. upload the bundle as a workflow artifact.

The expected artifact name is:

```text
central-bundle-0.1.0
```

The expected file inside the artifact is:

```text
central-bundle-0.1.0.zip
```

## Publish to Maven Central

Download the generated `central-bundle-0.1.0.zip` from the GitHub Actions run.

Upload the bundle in Sonatype Central Portal.

After upload:

1. wait for Sonatype validation,
2. inspect validation errors if any appear,
3. publish the deployment when validation succeeds.

Do not tag the release before the Central Portal deployment is published successfully.

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

For the demo application, update the dependency from:

```kotlin
implementation("io.github.camilyed:reactive-transaction-spring-boot-starter:0.1.0-SNAPSHOT")
```

to:

```kotlin
implementation("io.github.camilyed:reactive-transaction-spring-boot-starter:0.1.0")
```

Remove the snapshots repository if it is no longer needed for the verification.

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

### Demo still resolves a snapshot

The demo intentionally consumes a snapshot before the first release. For post-release verification,
update the demo dependency to the release version and remove the Sonatype snapshots repository if the
test should prove Maven Central consumption only.
