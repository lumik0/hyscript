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

    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")

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
        println("✅ Сборка завершена, плагин отправлен в mods!")
    }
}

tasks.register<JavaExec>("runServer") {
    group = "hytale"
    description = "Копирует плагин и запускает сервер Hytale"

    dependsOn("deploy")

    mainClass.set("-jar")
    workingDir = file("E:/HSytale/Hyscript/server") // Папка, где лежит сервер

    args("E:\\HSytale\\Hyscript\\libs\\HytaleServer.jar", "--assets", "../Assets.zip")

    jvmArgs("-XX:+UnlockExperimentalVMOptions", "-XX:+EnableJVMCI", "-Dpolyglot.engine.WarnInterpreterOnly=false", "-Xmx4G", "-Xms2G")

    standardInput = System.`in`
    standardOutput = System.out
    errorOutput = System.err
}

tasks.test {
    useJUnitPlatform()
}