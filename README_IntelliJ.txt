Uncrafter 2 (NeoForge 1.21.1, mod id uncrafter_2, author pingk)

1. Open the folder containing build.gradle in IntelliJ IDEA and trust the project.
2. Set the Project SDK and the Gradle JVM to JDK 21, then wait for the Gradle sync.
3. Run the neoforge > runClient task to test, or build > build for the jar (build/libs).

JEI is only needed to compile and run the dev client (see jei_version in gradle.properties).
The built jar works without it.

Config: config/uncrafter_2-common.toml
