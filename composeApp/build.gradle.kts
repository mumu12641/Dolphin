import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)

            implementation(libs.compose.materialIcons) // For core icons
            implementation(libs.compose.materialIconsExtended) // For extended icons
            implementation(libs.mpfilepicker)
            implementation(libs.precompose)
            implementation(libs.precompose.viewmodel)
            implementation(libs.datastore.core)
            implementation(libs.datastore.preferences)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.bouncycastle.prov)
            implementation(libs.slf4j.simple)

            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.gson.core)

            implementation(libs.compose.colorpicker)
            implementation(libs.materialKolor)



        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspJvm", libs.androidx.room.compiler)
}
compose.desktop {
    application {
        mainClass = "io.github.mumu12641.dolphin.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            modules("jdk.unsupported")
            modules("jdk.unsupported.desktop")
            packageName = "Dolphin"
            packageVersion = "1.0.0"

            windows {
                iconFile.set(project.layout.projectDirectory.file("src/jvmMain/resources/icon.ico"))
            }
        }
    }
}
