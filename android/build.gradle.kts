// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

// Pasta "build" fora do projeto: o OneDrive bloqueia ficheiros dentro da pasta
// sincronizada e faz falhar a compilação. Fica em ~/.gradle-builds/PerdidosAchados
allprojects {
    layout.buildDirectory.set(
        file("${System.getProperty("user.home")}/.gradle-builds/PerdidosAchados/${project.name}")
    )
}