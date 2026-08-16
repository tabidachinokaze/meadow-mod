import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("net.fabricmc.fabric-loom")
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "2.4.10"
    id("io.ktor.plugin") version "3.5.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.10"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
    // Add repositories to retrieve artifacts from in here.
    // You should only use this when depending on other mods because
    // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
    // See https://docs.gradle.org/current/userguide/declaring_repositories.html
    // for more information about repositories.
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register("meadow") {
            sourceSet(sourceSets.main.get())
            sourceSet(sourceSets.getByName("client"))
        }
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${providers.gradleProperty("minecraft_version").get()}")
    implementation("net.fabricmc:fabric-loader:${providers.gradleProperty("loader_version").get()}")

    // Fabric API. This is technically optional, but you probably want it anyway.
    implementation("net.fabricmc.fabric-api:fabric-api:${providers.gradleProperty("fabric_api_version").get()}")
    implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

    // ktor client（Fabric mod 需用 include() 打包进 jar，否则生产环境 NoClassDefFoundError）
    val ktorVersion = providers.gradleProperty("ktor_version").get()
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    // 打包全部 ktor 运行时构件（含传递依赖：ktor-utils/ktor-io/ktor-http/ktor-network/ktor-websockets/
    // ktor-serialization/ktor-sse 等），避免运行时 NoClassDefFoundError（如 io.ktor.util.PlatformUtils）
    listOf(
        "ktor-client-core", "ktor-client-cio", "ktor-client-content-negotiation",
        "ktor-client-logging", "ktor-client-websockets", "ktor-serialization-kotlinx-json",
        "ktor-http", "ktor-http-cio", "ktor-utils", "ktor-io",
        "ktor-network", "ktor-network-tls", "ktor-events", "ktor-websockets",
        "ktor-websocket-serialization", "ktor-serialization", "ktor-serialization-kotlinx", "ktor-sse",
    ).forEach { module ->
        include("io.ktor:$module-jvm:$ktorVersion")
    }

    // ktor 运行时依赖的 kotlinx 库：必须打包匹配版本，否则与 Fabric 环境自带的旧版冲突
    // （NoSuchMethodError: Job.cancel$default / invokeOnCompletion$default 等）
    val kotlinxVersion = "1.11.0"
    include("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$kotlinxVersion")
    include("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinxVersion")
    include("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$kotlinxVersion")
    include("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$kotlinxVersion")
    include("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$kotlinxVersion")
    include("org.jetbrains.kotlinx:kotlinx-serialization-json-io-jvm:$kotlinxVersion")
    include("org.jetbrains.kotlinx:kotlinx-io-core-jvm:0.9.1")
    include("org.jetbrains.kotlinx:kotlinx-io-bytestring-jvm:0.9.1")

    testImplementation(kotlin("test"))
}

tasks.processResources {
    val version = version
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand("version" to version)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_25
    }
}

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25

    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.jar {
    val projectName = project.name
    inputs.property("projectName", projectName)

    from("LICENSE") {
        rename { "${it}_$projectName" }
    }
}

// configure the maven publication
publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}

// io.ktor.plugin 引入的 shadow 需要 mainClass（面向 Ktor 应用）；Fabric mod 用 remapJar 分发，禁用 shadow
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    enabled = false
}
