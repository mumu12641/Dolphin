import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinx.serialization)
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
//            implementation(libs.m3color)
//            implementation("com.github.Kyant0:m3color:2025.4")

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


compose.desktop {
    application {
        mainClass = "io.github.mumu12641.dolphin.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            modules("jdk.unsupported")
            modules("jdk.unsupported.desktop")
            packageName = "io.github.mumu12641.dolphin"
            packageVersion = "1.0.0"

        }
    }
}
