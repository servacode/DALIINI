// The injection annotations shared code may carry (DECISION-087). On Android they are
// javax.inject's own, so Hilt builds a shared class exactly as it builds any other; on iOS they
// mean nothing and the iPhone app constructs what it needs itself.
plugins {
    id("serva.kmp.library")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            // `api`: whoever compiles a class carrying the alias needs the annotation it names.
            api(libs.javax.inject)
        }
    }
}
