import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Configuración local del backend (local.properties no se versiona). Ver README → «Backend».
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun localProperty(name: String, default: String) =
    localProperties.getProperty(name)?.trim()?.takeIf { it.isNotEmpty() } ?: default

android {
    namespace = "com.voicenoteai.wear"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.voicenoteai.wear"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // 10.0.2.2 es el localhost del PC visto desde el emulador.
        buildConfigField("String", "BACKEND_URL", "\"${localProperty("voicenoteai.backendUrl", "http://10.0.2.2:3000")}\"")
        // Token fijo de la etapa 2 (PRD §12); vacío = la app no envía nada y solo muestra el texto.
        buildConfigField("String", "DEV_API_TOKEN", "\"${localProperty("voicenoteai.devApiToken", "")}\"")
    }

    // Firma de release desde local.properties (voicenoteai.keystore.*). Sin keystore se firma con la
    // clave de debug: basta para instalar en el reloj propio por adb (ver README → «Reloj físico»).
    val keystorePath = localProperty("voicenoteai.keystore.path", "")
    signingConfigs {
        if (keystorePath.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystorePath)
                storePassword = localProperty("voicenoteai.keystore.password", "")
                keyAlias = localProperty("voicenoteai.keystore.alias", "")
                keyPassword = localProperty("voicenoteai.keystore.keyPassword", "")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Las clases bajo prueba registran con android.util.Log; en la JVM sus métodos devuelven 0.
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(libs.core.splashscreen)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.wear.tooling.preview)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.ktor.client.mock)
    debugImplementation(libs.ui.tooling)
}