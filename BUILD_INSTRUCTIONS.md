# Building Kelps AH into a jar

This is a Fabric mod for Minecraft 26.2 (needs Java 25+ on whatever machine builds it).
I can't compile it myself — Fabric mods need to download the Minecraft game jar
and Fabric API from Mojang/Fabric's servers during the build, and my sandbox's
network doesn't have access to those. You'll need to build it on a machine (or via
GitHub Actions) that does.

## Option A — GitHub Actions (no local setup needed)

1. Create a new repository on GitHub (can be private).
2. Upload every file in this folder, keeping the folder structure
   (including the hidden `.github/workflows/build.yml` file).
3. GitHub will automatically run the build. Go to the **Actions** tab of your repo,
   click the latest run, and download the `kelps_ah-jar` artifact once it finishes
   (usually 2-5 minutes).
4. Unzip that download — inside is `kelps_ah-5.3.1-persistent-cancel.jar`.
5. Drop that jar into your server's `mods/` folder (make sure Fabric API is also
   installed on the server — same as before).

## Option B — Build locally

Requirements: JDK 25+ and internet access on the machine you build on.

1. Open a terminal in this folder.
2. Run:
   ```
   gradle build
   ```
   (If you don't have Gradle installed, install it first, or run
   `gradle wrapper --gradle-version 9.5.1` once to generate a `./gradlew` you can
   use instead.)
3. The finished jar will be in `build/libs/kelps_ah-5.3.1-persistent-cancel.jar`.
4. Drop it into your server's `mods/` folder.

## What's in the mod

This is your full `KelpsAhMod.java` — the Auction House, shop, `/pay <player> <amount>`
command, and the balance/listing persistence (`config/kelps_ah/balances.txt` and
`listings.txt`) are all part of this single mod, so they build and ship together
as one jar.
