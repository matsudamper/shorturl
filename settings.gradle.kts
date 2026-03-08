plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

includeBuild("build-logic")

rootProject.name = "shorturl"

include(":server")
include(":admin")
