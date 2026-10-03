// A module shared with the iPhone app whose Android side draws screens (DECISION-089): the
// repositories, use cases and rules in common code; the Compose screens, the Hilt-built view
// models and the strings in androidMain.
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    id("serva.kmp.hilt")
    id("org.jetbrains.kotlin.plugin.compose")
}

composeCompiler {
    // The screens are Android's until they move to Compose Multiplatform; nothing on iOS is
    // composable yet.
    targetKotlinPlatforms.set(setOf(KotlinPlatformType.androidJvm))
}

kotlin {
    android {
        // The screens' strings and drawables, under the module's own R as before.
        androidResources {
            enable = true
        }
    }
}
