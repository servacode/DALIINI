package com.servacode.directory.core.inject

/**
 * `javax.inject.Inject` on Android, where Hilt reads it from the compiled class and builds the
 * class as it builds any other; nothing on iOS. Shared code writes `@Inject constructor` with
 * this import instead of javax.inject's, which does not exist on iOS (DECISION-087).
 */
@Target(AnnotationTarget.CONSTRUCTOR)
@Retention(AnnotationRetention.RUNTIME)
expect annotation class Inject()

/** `javax.inject.Singleton` on Android; nothing on iOS. */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
expect annotation class Singleton()
