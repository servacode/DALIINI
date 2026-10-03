package com.servacode.directory.core.maps

/** The lifecycle calls a MapLibre `MapView` takes. */
interface MapViewCalls {
    fun create()
    fun start()
    fun resume()
    fun pause()
    fun stop()
    fun destroy()
}

/**
 * Moves a map view between lifecycle levels one step at a time, so it gets every call in
 * order and exactly once, whatever order the events arrive in.
 *
 * A screen's view is created with the screen and destroyed when the screen leaves, which can
 * happen while it is still resumed. Going straight to `onDestroy` from there skips `onPause`
 * and `onStop`, and `onStop` is where MapLibre releases its connectivity receiver and file
 * source; a second `onDestroy` from the lifecycle observer would follow. [destroy] steps down
 * through pause and stop first and does nothing the second time.
 */
class MapLifecycleSteps(private val calls: MapViewCalls) {
    enum class Level { NONE, CREATED, STARTED, RESUMED, DESTROYED }

    var level: Level = Level.NONE
        private set

    fun moveTo(target: Level) {
        if (level == Level.DESTROYED) return
        if (target == Level.DESTROYED) {
            destroy()
            return
        }
        while (level < target) {
            when (level) {
                Level.NONE -> calls.create().also { level = Level.CREATED }
                Level.CREATED -> calls.start().also { level = Level.STARTED }
                Level.STARTED -> calls.resume().also { level = Level.RESUMED }
                else -> return
            }
        }
        while (level > target) {
            when (level) {
                Level.RESUMED -> calls.pause().also { level = Level.STARTED }
                Level.STARTED -> calls.stop().also { level = Level.CREATED }
                // A created view goes down only by being destroyed.
                else -> return
            }
        }
    }

    fun destroy() {
        when (level) {
            Level.DESTROYED -> return
            // Never created: there is nothing to release.
            Level.NONE -> Unit
            else -> {
                moveTo(Level.CREATED)
                calls.destroy()
            }
        }
        level = Level.DESTROYED
    }
}
