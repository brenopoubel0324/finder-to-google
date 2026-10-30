import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseKeystore = providers.environmentVariable("FINDER_RELEASE_KEYSTORE").orNull
val releaseAlias = providers.environmentVariable("FINDER_RELEASE_KEY_ALIAS").orNull
val releaseStorePassword = providers.environmentVariable("FINDER_RELEASE_STORE_PASSWORD").orNull
val releaseKeyPassword = providers.environmentVariable("FINDER_RELEASE_KEY_PASSWORD").orNull

android {
    namespace = "br.com.findertogoogle"
    compileSdk = 36
    defaultConfig {
        applicationId = "br.com.findertogoogle"
        minSdk = 29
        targetSdk = 36
        versionCode = 4
        versionName = "1.1.0-rc.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { buildConfig = true }
    signingConfigs {
        create("release") {
            storeFile = releaseKeystore?.let { file(it) }
            keyAlias = releaseAlias
            storePassword = releaseStorePassword
            keyPassword = releaseKeyPassword
        }
    }
    buildTypes {
        debug { versionNameSuffix = "-debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

// A tarefa nativa de assinatura pode nem existir se a configuração estiver incompleta.
// Esta verificação é sempre registrada e protege o release antes do empacotamento.
val verifyReleaseSigning by tasks.registering {
    doLast {
        check(listOf(releaseKeystore, releaseAlias, releaseStorePassword, releaseKeyPassword).all { !it.isNullOrBlank() }) {
            "Assinatura release ausente. Configure as quatro variáveis FINDER_RELEASE_* fora do repositório."
        }
        check(file(requireNotNull(releaseKeystore)).isFile) { "O keystore release configurado não está disponível." }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(verifyReleaseSigning) }

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.11.0")
    implementation("com.google.android.material:material:1.13.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
}
