// The design system, shared with the iPhone app (DECISION-094): theme, components, icons,
// illustrations and words in common code on Compose Multiplatform. Android keeps, in androidMain,
// what only Android has: the theme's night configuration and system bars, and the resources its
// XML, widget and notifications name by id.
plugins {
    id("serva.kmp.compose")
}

kotlin {
    android {
        // R ids for what is not Compose: the launch theme and launcher (brand colours, brand_mark),
        // the widget and notifications (brand_symbol, dl_ic_phone, a few words).
        androidResources { enable = true }
    }
    sourceSets {
        commonMain {
            // The generated tokens and the icon set's path data, from the package that owns them.
            kotlin.srcDir(rootProject.file("../../packages/design-tokens/generated"))
            dependencies {
                implementation(project(":core:model"))
                implementation(libs.jetbrains.lifecycle.runtime.compose)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.androidx.core)
                // The system's back, for DirectoryBackHandler.
                implementation(libs.androidx.activity.compose)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.okhttp)
            }
        }
        iosMain.dependencies {
            // Coil 3.5 and later are built with Kotlin 2.4, which this project's 2.3 cannot read
            // on iOS; 3.4 is the newest it can (RemoteImage).
            implementation(libs.coil.ios.compose)
            implementation(libs.coil.ios.network.ktor)
            implementation(libs.ktor.client.darwin)
        }
        iosTest.dependencies {
            // The theme drawn on the iPhone simulator, its words and faces read from resources.
            implementation(libs.compose.multiplatform.ui.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.junit)
        }
    }
}

// Android resources: this module's own in src/androidMain/res (brand colours, brand_mark, the
// symbols, a few words), and the token package's, for XML that cannot read Kotlin: the colour
// tokens as resources and the icon set as vector drawables.
androidComponents {
    onVariants { variant ->
        variant.sources.res?.addStaticSourceDirectory(
            rootProject.file("../../packages/design-tokens/generated/android").path,
        )
    }
}

// Compose resources are assembled from their owners rather than copied into this module: this
// module's words and the brand symbol; the vocabulary the token package generates; the brand's
// faces from the token package (SIL OFL). One copy of each file in the repository.
val composeResourcesAssembly = tasks.register<Sync>("assembleDesignSystemComposeResources") {
    into(layout.buildDirectory.dir("assembledComposeResources/commonMain"))
    from("src/commonMain/composeResources")
    from("src/androidMain/res/drawable-nodpi/brand_symbol.webp") { into("drawable") }
    from(rootProject.file("../../packages/design-tokens/generated/android/values/directory_vocabulary.xml")) {
        into("values")
    }
    from(rootProject.file("../../packages/design-tokens/fonts/android/font")) { into("font") }
}

compose.resources {
    packageOfResClass = "com.servacode.directory.core.designsystem"
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = objects.directoryProperty()
            .fileProvider(composeResourcesAssembly.map { it.destinationDir }),
    )
}
