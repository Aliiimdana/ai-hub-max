pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "AIHubMax"
include(":app")
include(":core:common")
include(":core:design")
include(":core:security")
include(":core:network")
include(":core:routing")
include(":core:database")
include(":core:storage")
include(":features:home")
include(":features:api_manager")
include(":features:free_ai")
include(":features:chat")
include(":features:image_studio")
include(":features:video_studio")
include(":features:agents")
include(":features:documents")
include(":features:usage")
