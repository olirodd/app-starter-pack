plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
}

tasks.register("coverage") {
    dependsOn(
        ":networking:coverageReport",
        ":persistence:coverageReport",
        ":analytics:coverageReport",
        ":util:coverageReport"
    )
}
