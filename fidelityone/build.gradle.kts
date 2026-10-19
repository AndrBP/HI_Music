// Fidelity One — build raiz
// Toolchain: AGP 8.9.0 + Gradle 8.12 (wrapper) — funciona com JDK 17 e JDK 21 (recomendado: 21 LTS).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose.compiler) apply false
}
