pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MySD"

val configuredEnginePath = providers.gradleProperty("myengine.path").orNull
    ?: System.getenv("MYENGINE_PATH")
    ?: "../MyEngine"
val engineCheckout = file(configuredEnginePath)
require(engineCheckout.resolve("settings.gradle.kts").isFile) {
    "MyEngine checkout not found at " + engineCheckout.absolutePath +
        ". Set -Pmyengine.path=<path> or MYENGINE_PATH."
}

includeBuild(engineCheckout)
include(":game")
include(":app")
