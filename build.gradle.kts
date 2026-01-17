plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.euaek"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Твой сервер
    compileOnly(files("libs/HytaleServer.jar"))

    implementation(kotlin("stdlib"))

    implementation("org.graalvm.polyglot:polyglot:23.1.2")
    implementation("org.graalvm.polyglot:js:23.1.2")
    implementation("org.graalvm.truffle:truffle-api:23.1.2")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

    testImplementation(kotlin("test"))
}

tasks {
    shadowJar {
        archiveClassifier.set("")

        // Это склеивает служебные файлы GraalVM, чтобы он видел движок JS внутри JAR
        mergeServiceFiles()

        manifest {
            attributes["Main-Class"] = "me.euaek.Plugin"
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

tasks.register<Copy>("deploy") {
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.get().archiveFile)
    into("E:/HSytale/Hyscript/server/mods")

    doLast {
        println("✅ Сборка завершена, плагин отправлен в mods!")
    }
}

tasks.test {
    useJUnitPlatform()
}