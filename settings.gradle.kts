rootProject.name = "altinn-client"

pluginManagement {
    val kotlinVersion: String by settings
    val kotlinterVersion: String by settings
    val sonarqubeVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jmailen.kotlinter") version kotlinterVersion
        id("org.sonarqube") version sonarqubeVersion
        id("maven-publish")
    }
}
