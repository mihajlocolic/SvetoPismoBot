/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    `java-library`
    `maven-publish`
    application
}

application {
    mainClass.set("org.mihajlo.Main")
}


repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    implementation("org.javacord:javacord:3.8.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
}

group = "org.mihajlo"
version = "0.1.1"
description = "SvetoPismoBot"
java.sourceCompatibility = JavaVersion.VERSION_21

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

