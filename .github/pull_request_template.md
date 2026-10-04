## What changed

<!-- One or two sentences. What does this PR do? -->

## Why

<!-- Link the issue if there is one: Closes #123 -->

## Checklist

- [ ] `./gradlew build` passes locally
- [ ] Tested on a Paper server, not just compiled
- [ ] New config keys are documented in `config.yml`
- [ ] New or changed messages exist in the `messages` section of `config.yml`

## Dependencies

- [ ] Not applicable, no dependency changes

If you bumped `paper-api` in `gradle/libs.versions.toml`:

- [ ] Checksums in `gradle/verification-metadata.xml` refreshed and the build passes
      from a clean cache (Paper republishes snapshots often, so this is required)
- [ ] Checked that nothing in the plugin uses a method removed in the new Paper API

## Screenshots

<!-- Before / after for anything visual, especially GUI changes. -->