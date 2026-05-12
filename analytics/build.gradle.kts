plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
    jacoco
}

group = "io.appstarterpack"
version = "1.0.0"

kotlin {
    jvmToolchain(21)
    jvm()
    androidLibrary {
        namespace = "io.appstarterpack.analytics"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

tasks.named<Test>("jvmTest").configure {
    finalizedBy("coverageReport")
}

tasks.register<JacocoReport>("coverageReport") {
    dependsOn(tasks.named("jvmTest"))
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("classes/kotlin/jvm/main"))
    )
    sourceDirectories.setFrom(files("src/commonMain/kotlin", "src/jvmMain/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory).include("jacoco/jvmTest.exec"))
    reports {
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/coverage"))
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(group.toString(), "analytics", version.toString())
    pom {
        name = "App Starter Pack — Analytics"
        description = "KMP analytics module: AnalyticsClient, ErrorReporter, Tracker."
        url = "https://github.com/olirodd/app-starter-pack"
    }
}
