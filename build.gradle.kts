plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.16"
}

group = "com.homo.tadokoro"
version = "1.14.514"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}