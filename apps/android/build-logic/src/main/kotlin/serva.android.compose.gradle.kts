plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

// AGP 9 removed the type parameters from CommonExtension and exposes buildFeatures
// as a plain property, so the AGP 8 style `CommonExtension<*, *, *, *, *, *>` with a
// `buildFeatures { }` block no longer resolves.
extensions.configure<com.android.build.api.dsl.CommonExtension> {
    buildFeatures.compose = true
}
