// build.gradle.kts (root)
plugins {
    id("com.android.application") version "8.3.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

// Fuerza versiones coherentes en TODOS los subproyectos
subprojects {
    configurations.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                // Alinear TODA la stdlib y módulos de Kotlin con el plugin
                useVersion("1.9.22")
                because("Evitar mezclar Kotlin 2.1.x con plugin 1.9.22")
            }
            if (requested.group == "androidx.databinding") {
                // Empatar DataBinding con tu AGP 8.3.2
                useVersion("8.3.2")
                because("Evitar databinding-ktx 8.12.1 con AGP 8.3.2")
            }
        }
    }
}
