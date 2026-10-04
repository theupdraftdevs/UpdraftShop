# UPDRAFT SHOP

**The official shop plugin for Updraft Network**



## Features

- Shop GUI with a configurable amount of categories.
- Hypixel style menus: dark grey titles joined by a grey `»`, all caps headers, and
  prices written as `BUY - $1,000`.
- Prices drop the decimals when an amount is whole, so a diamond reads `$500` and a
  fraction reads `$12.50`.
- Per item buy and sell prices, set in `config.yml` instead of hardcoded.
- A trade screen with amount buttons (1 / 8 / 16 / 32 / 64 / max) so nothing is
  bought by accident.
- Works with any economy plugin through [Vault](https://github.com/milkbowl/Vault), and falls back to a built in
  balance system when Vault is not installed.
- Shop menus are read-only: nothing can be shift clicked, dragged or number key
  swapped.
- Coloured names, messages and menu text are all configurable.
- Optional soft click sound on menu navigation and trades. (`gui.sounds`)

## Setup

1. Drop the jar in `plugins/`.
2. Start the server once to generate `plugins/UpdraftShop/config.yml`.
3. Edit the categories, items and prices to taste.
4. `/shop` opens the menu once the commands are implemented.

## Look and feel

Everything a player reads comes from `config.yml`, and every window title is derived
from a single `gui.shop-name` key:

| screen    | title                                  |
|-----------|----------------------------------------|
| main menu | `§8Updraft Shop`                       |
| category  | `§8Updraft Shop §7» §8BLOCKS`          |
| trade     | `§8Updraft Shop §7» §8DRAGON'S BREATH` |

The category and item names are stripped of their colour codes and upper cased before
they go into a title, so a category can be any colour without tinting the menu.

To restyle the shop, edit these in `config.yml`:

- `prefix` — prepended to every chat message
- `gui.shop-name`, `gui.sounds`, `gui.back-item`, `gui.close-item` and their `*-icon`
  companions
- `categories.<id>.display-name` — the coloured, bold header
- `categories.<id>.icon` — the material shown in the main menu
- `categories.<id>.description` — the lines under a category
- `items.<id>.name` — the coloured item name in the trade screen
- `messages.*` — every chat message

Set an item's `buy: 0.0` to make it sell only, or `sell: 0.0` to make it buy only.

## Building

```bash
./gradlew build
```

Produces `build/libs/UpdraftShop-<version>.jar`. The build needs any JDK 21 or newer;
it compiles with `--release 21`, so it does not insist on one exact JDK being
installed. Jars are reproducible: the same source always produces the same bytes.

## Updating dependencies

Dependency versions live in `gradle/libs.versions.toml`, and every resolved artifact
is pinned by SHA-256 in `gradle/verification-metadata.xml`. A build that resolves
different bytes than the file records fails rather than silently using them.

Paper only publishes its API as an `R0.1-SNAPSHOT`, and Gradle cannot record a
snapshot's checksums automatically, so bumping `paper-api` is a two step job:

1. Change `paperApi` in `gradle/libs.versions.toml`.
2. Regenerate the checksums:

   ```bash
   ./gradlew --write-verification-metadata sha256 --refresh-dependencies --rerun-tasks compileJava
   ```

   then paste the `paper-api` entry back into `gradle/verification-metadata.xml` by
   hand, which is noted in that file.

Expect CI to fail on a dependency PR until step 2 is done. That failure is the lock
working, not a bug.

## Releases

Push a tag and the release workflow builds checks the tag to see if it matches the version in
`build.gradle.kts`, then publishes the jar with generated release notes:

```bash
git tag v1.0.0 && git push --tags
```

`plugin.yml` picks the version up from the build automatically, so the tag, the jar
name and the version the server reports can never drift apart.

## Automation

`dependabot.yml` watches Gradle and GitHub Actions weekly. Semver buckets are split
into groups so patch and minor updates arrive as a single pull request and land
automatically, while major updates get a human to look at them.
