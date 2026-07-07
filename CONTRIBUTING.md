# How to Contribute

We'd love to accept your patches and contributions to this project. Pull requests are part of
what makes open source great. There are just a few small guidelines you need to follow.

## Code of Conduct

Participation in this project comes under the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md)

## Submitting code via Pull Requests

- We follow the [Github Pull Request Model](https://help.github.com/articles/about-pull-requests/) for
  all contributions.
- For large bodies of work, we recommend creating an issue outlining the feature that you wish to build,
  and describing how it will be implemented. This gives a chance for review to happen early, and ensures
  no wasted effort occurs.
- All submissions, will require review before being merged.
- Finally - *Thanks* for considering submitting code to the project!

## Formatting

When submitting pull requests, make sure to do the following:

- Maintain the same code style as the rest of the project.
- Remove trailing whitespace. Many editors will do this automatically.
- Ensure any new files have [a trailing newline](https://stackoverflow.com/questions/5813311/no-newline-at-end-of-file)

Java source is formatted with Palantir Java Format:

```bash
tools/java_format.sh write
```

Check formatting without changing files:

```bash
tools/java_format.sh check
```

## Bazel build

This project uses Bazel. The pinned Bazel version is recorded in `.bazelversion`.

Run the full local verification gate before submitting changes:

```bash
tools/check.sh
```

The full gate regenerates bazel-depgen outputs, checks Buildifier formatting, checks Java formatting, builds all Bazel
targets, runs tests, and enforces the current coverage baseline: 72% line coverage and 62% branch coverage.

Useful focused commands:

```bash
tools/update_java_deps.sh
bazel run //:buildifier
bazel run //:buildifier_check
bazel build //...
bazel test //...
```

After editing `third_party/java/dependencies.yml` or `tools/java-format/dependencies.yml`, run:

```bash
tools/update_java_deps.sh
```

## How to speed the merging of pull requests

* Describe your changes in the CHANGELOG.md (if present).
* Give yourself some credit in the appropriate place (usually the CHANGELOG.md).
* Make commits of logical units.
* Ensure your commit messages help others understand what you are doing and why.
* Check for unnecessary whitespace with `git diff --check` before committing.
* Maintain the same code style.
* Maintain the same level of test coverage or improve it.

## Maven Central release

The Maven Central release workflow is documented in [tools/release/README.md](tools/release/README.md). Run the full
readiness check before preparing a release:

```bash
tools/release/check_ready.sh
```

To release the next version:

1. Add all user-visible changes to `CHANGELOG.md` under `### Unreleased`.
2. Determine the next version:

   ```bash
   tools/release/next_version.sh
   ```

3. Preview the release metadata update:

   ```bash
   tools/release/prepare_release.sh <version> --dry-run
   ```

4. Prepare the release for real:

   ```bash
   tools/release/prepare_release.sh <version>
   ```

5. Package and upload the Maven Central bundle:

   ```bash
   tools/package_maven_central.sh <version>
   tools/release/upload_maven_central.sh <version>
   ```

6. Finalize the release after Central reports publication success:

   ```bash
   tools/release/finalize_release.sh <version>
   ```

The normal all-in-one path is:

```bash
tools/release/perform_release.sh <version>
```

## Additional Resources

* [General GitHub documentation](http://help.github.com/)
* [How to write a good Git Commit message](https://chris.beams.io/posts/git-commit/) -
  Great way to make sure your Pull Requests get accepted.
* [An Open Source Etiquette Guidebook](https://css-tricks.com/open-source-etiquette-guidebook/#article-header-id-1)
