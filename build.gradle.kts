plugins {
    kotlin("jvm") version "1.6.21"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

val junitJupiterVersion = "5.8.2"
val rapidsAndRiversCliVersion = "1.520584e"
val prometheusSimpleclientVersion = "0.15.0"
val jvmTargetVersion = "17"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.github.navikt:rapids-and-rivers-cli:$rapidsAndRiversCliVersion")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("io.mockk:mockk:1.12.3")

    implementation("io.prometheus:simpleclient_common:$prometheusSimpleclientVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusSimpleclientVersion")
}


tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = jvmTargetVersion
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = jvmTargetVersion
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
