plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "pt.uptomar.dam2026.perdidosachados"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "pt.uptomar.dam2026.perdidosachados"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // View Binding: acesso aos elementos dos layouts sem findViewById
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Retrofit: cliente HTTP para consumir a API REST (https://square.github.io/retrofit/)
    // Usa-se a versão 2.9.0 em vez da 2.3.0 das aulas porque a 2.3.0 traz um OkHttp
    // antigo, com problemas em ligações HTTPS nas versões recentes do Android.
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Conversor Gson: transforma o JSON da API em objetos Kotlin (https://github.com/google/gson)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Glide: descarrega e mostra imagens, com cache (https://github.com/bumptech/glide)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
