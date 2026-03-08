plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

includeBuild("build-logic")

rootProject.name = "shorturl"

include(":server")
include(":admin")
