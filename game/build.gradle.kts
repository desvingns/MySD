plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    api("dev.myengine:engine-core:0.0.1")
    implementation("dev.myengine:engine-world:0.0.1")
    implementation("dev.myengine:engine-content:0.0.1")
    implementation("dev.myengine:engine-entities:0.0.1")
    implementation("dev.myengine:engine-defense:0.0.1")
    implementation("dev.myengine:engine-render:0.0.1")
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
}
