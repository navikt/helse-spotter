plugins {
    kotlin("jvm") version "1.6.10"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

val junitJupiterVersion = "5.8.2"
val rapidsAndRiversCliVersion = "1.520584e"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.navikt:rapids-and-rivers-cli:$rapidsAndRiversCliVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("io.mockk:mockk:1.12.3")

    implementation("io.prometheus:simpleclient_common:0.15.0")
    implementation("io.prometheus:simpleclient_hotspot:0.15.0")
}


tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    withType<Wrapper> {
        gradleVersion = "7.4.2"
    }
    withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    named<Jar>("jar") {
        archiveFileName.set("app.jar")

        manifest {
            attributes["Main-Class"] = "no.nav.helse.spotter.AppKt"
            attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
                it.name
            }
        }

        doLast {
            configurations.runtimeClasspath.get().forEach {
                val file = File("$buildDir/libs/${it.name}")
                if (!file.exists())
                    it.copyTo(file)
            }
        }
    }
}
