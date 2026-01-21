plugins {
    kotlin("jvm") version "2.1.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.euaek"
version = "1.0.2-SNAPSHOT"

kotlin {
    jvmToolchain(23)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(23))
    }
}

repositories {
    maven(url = "https://www.cursemaven.com")
    mavenCentral()
}

dependencies {
    compileOnly(files("libs/HytaleServer.jar"))

    implementation(kotlin("stdlib"))

    implementation("curse.maven:hyui-1431415:7479623")

    implementation("org.graalvm.polyglot:polyglot:23.1.2")
    implementation("org.graalvm.polyglot:js:23.1.2")
    implementation("org.graalvm.truffle:truffle-api:23.1.2")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    testImplementation(kotlin("test"))
}

tasks {
    shadowJar {
        archiveClassifier.set("")

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
        // thanks gemini
        println("✅ Сборка завершена, плагин отправлен в mods!")
    }
}

tasks.test {
    useJUnitPlatform()
}