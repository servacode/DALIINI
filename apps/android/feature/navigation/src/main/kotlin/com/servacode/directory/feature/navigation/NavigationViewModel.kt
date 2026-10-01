package com.servacode.directory.feature.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.servacode.directory.core.location.LocationProvider
import com.servacode.directory.core.location.LocationResult
import com.servacode.directory.core.maps.GeoMath
import com.servacode.directory.core.maps.MapPoint
import com.servacode.directory.core.maps.NavigationRoute
import com.servacode.directory.core.maps.RouteChoices
import com.servacode.directory.core.maps.RoutingException
import com.servacode.directory.core.maps.RoutingProfile
import com.servacode.directory.core.maps.RoutingProvider
import com.servacode.directory.core.model.DirectoryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class NavigationUiState(
    val navigation: NavigationState = NavigationState.Idle,
    val warning: String? = null,
    /** The way the person is travelling, which they may change without leaving the screen. */
    val profile: RoutingProfile = RoutingProfile.DRIVING,
    /**
     * True while a new route is being fetched for a mode just chosen.
     *
     * Separate from [NavigationState.Routing] on purpose: the route already on screen stays
     * there while the next one is fetched, so choosing a mode does not empty the map.
     */
    val switching: Boolean = false,
    /** Which way the person is facing, for the mark that stands where they are. */
    val bearingDegrees: Float? = null,
    /** True when the readings are made up rather than measured, so the screen can say so. */
    val simulated: Boolean = false,
    /**
     * True while nothing is known about where the person is and a reading is being waited on.
     *
     * The screen used to say it was computing a route throughout this, which it was not: the
     * engine answers in about twenty milliseconds and the provider is allowed eight seconds.
     * Whatever is being waited for, saying the other thing is how a wait becomes a fault.
     */
    val locating: Boolean = false,
    /**
     * The ways there worth choosing between, the engine's own preference first.
     *
     * Empty until a route arrives, and one long when the engine found nothing else worth
     * offering — a short errand down a single street usually has no second way.
     */
    val choices: List<NavigationRoute> = emptyList(),
    /** Which of [choices] is being followed. */
    val chosenIndex: Int = 0,
) {
    /** True only when there is an actual decision to put in front of someone. */
    val hasChoice: Boolean get() = RouteChoices.isAChoice(choices)

    /** The route as it is right now, whichever state holds it. */
    val progress: NavigationProgress?
        get() = when (navigation) {
            is NavigationState.Navigating -> navigation.progress
            is NavigationState.Rerouting -> navigation.progress
            else -> null
        }

    val route: NavigationRoute?
        get() = progress?.route ?: (navigation as? NavigationState.Arrived)?.route
}

@HiltViewModel
class NavigationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val routingProvider: RoutingProvider,
    private val locationProvider: LocationProvider,
    private val voice: NavigationVoice,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<DirectoryRoute.BuiltInNavigation>()
    private val destination = MapPoint(route.latitude, route.longitude)

    /**
     * Whether this trip is being driven by made-up readings.
     *
     * A demonstration, offered only by a debug build, so that guidance — the map following, the
     * maneuvers advancing, the voice speaking — can be watched without anyone getting into a car.
     */
    private val simulated = route.simulated

    private val engine = NavigationEngine()
    private val planner = NavigationVoicePlanner()
    private val _state = MutableStateFlow(
        NavigationUiState(
            profile = runCatching { RoutingProfile.valueOf(route.profile) }
                .getOrDefault(RoutingProfile.DRIVING),
            simulated = simulated,
        ),
    )
    // Internal like the state it carries: only this module's screen reads it.
    internal val state: StateFlow<NavigationUiState> = _state.asStateFlow()

    private var locationJob: Job? = null
    private var routeJob: Job? = null

    /**
     * The last place the person was known to be.
     *
     * Kept so that changing the travel mode is one request to the routing engine and nothing
     * else. Asking the receiver again costs up to eight seconds of "جارٍ حساب المسار…" for a
     * position that has not moved since the question was last asked.
     */
    private var origin: MapPoint? = null

    /** The last reading, so a way chosen mid-trip is followed from here. */
    private var here: MapPoint? = null

    init {
        start()
    }

    fun retry() {
        locationJob?.cancel()
        routeJob?.cancel()
        planner.reset()
        origin = null
        start()
    }

    /** Walk it, ride it or drive it — the same destination, a different set of streets. */
    fun selectProfile(profile: RoutingProfile) {
        if (profile == _state.value.profile) return
        _state.update { it.copy(profile = profile) }
        planner.reset()
        val from = origin
        if (from == null) {
            start()
            return
        }
        computeRoute(from, profile, switching = true)
    }

    /**
     * Take one of the other ways there.
     *
     * The engine is restarted on the chosen line from where the person actually is, so the trip
     * continues from here rather than from where it was first computed, and the planner forgets
     * what it had already said — the first turn of a different route is news again.
     *
     * No new request is made: every way offered was fetched together, and asking again for one
     * of them would be asking a moving engine a question it has already answered.
     */
    fun selectRoute(index: Int) {
        val current = _state.value
        if (index == current.chosenIndex) return
        val chosen = current.choices.getOrNull(index) ?: return
        _state.update { it.copy(chosenIndex = index) }
        planner.reset()
        voice.stop()
        val at = here ?: origin ?: chosen.geometry.first()
        publish(engine.start(chosen, at, System.currentTimeMillis()))
    }

    private fun start() {
        viewModelScope.launch {
            _state.update { it.copy(navigation = engine.routing(), warning = null) }
            // A position already known is a route now. Waiting for a fresh reading costs the
            // eight seconds the provider is allowed, while the way itself is computed in about
            // twenty milliseconds — so the wait on this screen was never the route's. The way
            // is drawn from what is already known, and the readings that follow correct it the
            // way they correct every other reading, by rerouting if it turns out to be wrong.
            val known = locationProvider.lastKnown()
            if (known != null) {
                val from = MapPoint(known.latitude, known.longitude)
                origin = from
                computeRoute(from, _state.value.profile, switching = false)
                return@launch
            }
            _state.update { it.copy(locating = true) }
            when (val current = locationProvider.current()) {
                is LocationResult.Available -> {
                    val from = MapPoint(current.fix.latitude, current.fix.longitude)
                    origin = from
                    _state.update { it.copy(locating = false) }
                    computeRoute(from, _state.value.profile, switching = false)
                }
                LocationResult.PermissionDenied -> {
                    _state.update { it.copy(locating = false) }
                    fail("LOCATION_PERMISSION_REQUIRED")
                }
                LocationResult.Unavailable -> {
                    _state.update { it.copy(locating = false) }
                    val last = locationProvider.lastKnown()
                    if (last == null) {
                        fail("LOCATION_UNAVAILABLE")
                    } else {
                        val from = MapPoint(last.latitude, last.longitude)
                        origin = from
                        computeRoute(from, _state.value.profile, switching = false)
                    }
                }
            }
        }
    }

    private fun fail(reason: String) {
        _state.update {
            it.copy(navigation = NavigationState.Error(reason), switching = false)
        }
    }

    private fun computeRoute(from: MapPoint, profile: RoutingProfile, switching: Boolean) {
        routeJob?.cancel()
        // Only the very first route empties the screen. A mode chosen afterwards keeps the way
        // that is already drawn until the next one is ready, so the map never blinks.
        _state.update {
            if (switching) it.copy(switching = true, warning = null) else it.copy(warning = null)
        }
        routeJob = viewModelScope.launch {
            // A demonstration is one trip being shown, not a decision being offered, so it asks
            // for the one way it is going to drive.
            val request = suspend {
                if (simulated) {
                    listOf(demonstrableRoute(from, profile))
                } else {
                    RouteChoices.worthOffering(
                        routingProvider.routes(
                            origin = from,
                            destination = destination,
                            profile = profile,
                            alternates = RouteChoices.MAX_OFFERED - 1,
                        ),
                    )
                }
            }
            runCatching { request() }
                .onSuccess { offered ->
                    // The shortest of the ways worth offering, not the engine's own first
                    // answer: across Raqqa that answer is the longer way more often than not.
                    val chosen = RouteChoices.preferred(offered)
                    val found = offered[chosen]
                    // A made-up trip starts where its own route starts, which is not where the
                    // reader is standing: the engine would otherwise open three kilometres off
                    // its own line and spend the demonstration recomputing.
                    val at = if (simulated) found.geometry.first() else from
                    publish(engine.start(found, at, System.currentTimeMillis()))
                    _state.update {
                        it.copy(switching = false, choices = offered, chosenIndex = chosen)
                    }
                    if (simulated) driveTheRoute(found) else collectLocationUpdates()
                }
                .onFailure { cause ->
                    engine.fail((cause as? RoutingException)?.failure?.name ?: "ROUTING_UNAVAILABLE")
                    _state.update {
                        it.copy(navigation = engine.currentState(), switching = false)
                    }
                }
        }
    }

    /**
     * The route the demonstration is run on, which is not always the reader's own.
     *
     * A reader standing across the street from the pharmacy asks for a demonstration and gets a
     * fifty-metre line with no turn in it, over before the map has settled and too short for
     * guidance to say a word — the planner's earliest warning is a hundred and fifty metres out.
     * That is not a demonstration of anything, and it is what this screen showed.
     *
     * So when the real trip is too short to show guidance working, the demonstration is run from a
     * point up the road the reader is coming from, far enough to carry turns and the three
     * sentences that go with each of them. The point is put on a bearing and handed to the routing
     * engine, which snaps it to a real street — nothing here guesses where roads are. If that
     * second request fails, the short trip is shown rather than nothing.
     */
    private fun demoStartMeters(profile: RoutingProfile): Double = when (profile) {
        RoutingProfile.WALKING -> WALKING_DEMO_METERS
        RoutingProfile.MOTORCYCLE -> MOTORCYCLE_DEMO_METERS
        RoutingProfile.DRIVING -> DRIVING_DEMO_METERS
    }

    private suspend fun demonstrableRoute(from: MapPoint, profile: RoutingProfile): NavigationRoute {
        val real = routingProvider.route(from, destination, profile)
        if (real.distanceMeters >= DEMO_MIN_METERS) return real
        val away = GeoMath.bearingDegrees(destination, from)
        val start = GeoMath.pointAtBearing(destination, away, demoStartMeters(profile))
        return runCatching { routingProvider.route(start, destination, profile) }
            .getOrDefault(real)
    }

    /**
     * Walk the route that was just computed, as though someone were on it.
     *
     * Played faster than life so a trip across a city can be watched in a couple of minutes;
     * the readings themselves are spaced as real ones are, so everything that times itself by
     * seconds — which is all of the voice guidance — behaves exactly as it would on the road.
     */
    private fun driveTheRoute(found: NavigationRoute) {
        locationJob?.cancel()
        locationJob = viewModelScope.launch {
            val profile = _state.value.profile
            val travellerSpeed = NavigationReplay.speedFor(profile).toDouble()
            val fixes = NavigationReplay.fixes(found.geometry, travellerSpeed.toFloat())
            // How fast to play it is two questions, and the slower answer wins.
            //
            // One is patience: a walk across a neighbourhood is half an hour of readings, and
            // nobody watches half an hour to see whether the voice speaks. The other is speech:
            // guidance leaves a gap between its sentences that is generous on the road — ten
            // seconds by car, half a minute on foot — and playing faster divides it. Divided
            // below the length of a sentence, an instruction can only arrive while the last one
            // is still being said. That was true of all three travellers here: the recordings
            // average four and a third seconds, and the gaps on screen were 1.6, 3.0 and 3.3.
            //
            // Neither question has one answer for every traveller, which is why neither is a
            // constant. Both are asked of the profile actually being demonstrated.
            val seconds = fixes.size * NavigationReplay.DEFAULT_STEP_SECONDS
            val watchable = seconds / DEMO_SECONDS
            val speakable = planner.tightestCueSeconds(travellerSpeed) / SPOKEN_SENTENCE_SECONDS
            val speed = maxOf(PLAYBACK_SPEED, minOf(watchable, speakable))
            val interval = (NavigationReplay.DEFAULT_STEP_SECONDS * 1000 / speed).toLong()
            fixes.forEach { fix ->
                delay(interval)
                handleLocation(
                    fix.point,
                    fix.accuracyMeters,
                    fix.speedMetersPerSecond,
                    fix.bearingDegrees,
                )
                if (_state.value.navigation is NavigationState.Arrived) return@launch
            }
        }
    }

    private fun collectLocationUpdates() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            locationProvider.updates().collectLatest { result ->
                when (result) {
                    is LocationResult.Available -> {
                        origin = MapPoint(result.fix.latitude, result.fix.longitude)
                        // The reading's own accuracy travels with it: the engine weighs a vague
                        // fix differently from a sharp one rather than believing both exactly.
                        handleLocation(origin!!, result.fix.accuracyMeters)
                    }
                    LocationResult.PermissionDenied -> fail("LOCATION_PERMISSION_REQUIRED")
                    LocationResult.Unavailable ->
                        _state.update { it.copy(warning = "LOCATION_TEMPORARILY_UNAVAILABLE") }
                }
            }
        }
    }

    private suspend fun handleLocation(
        point: MapPoint,
        accuracyMeters: Float = 0f,
        speedMetersPerSecond: Float? = null,
        bearingDegrees: Float? = null,
    ) {
        // Where the trip is now, so that changing to another way there carries on from here
        // instead of restarting at the place the route was first asked for.
        here = point
        val update = engine.update(point, System.currentTimeMillis(), accuracyMeters)
        publish(update, speedMetersPerSecond = speedMetersPerSecond, bearing = bearingDegrees)
        if (!update.rerouteRequired) return
        val profile = _state.value.profile
        runCatching { routingProvider.route(point, destination, profile) }
            .onSuccess { rerouted ->
                publish(
                    engine.applyReroute(rerouted, point, System.currentTimeMillis(), accuracyMeters),
                    speedMetersPerSecond = speedMetersPerSecond,
                    bearing = bearingDegrees,
                )
            }
            .onFailure {
                publish(
                    engine.rerouteFailed(System.currentTimeMillis()),
                    warning = "REROUTE_NETWORK_FAILED",
                )
            }
    }

    private fun publish(
        update: NavigationUpdate,
        warning: String? = null,
        speedMetersPerSecond: Float? = null,
        bearing: Float? = null,
    ) {
        _state.update {
            it.copy(
                navigation = update.state,
                warning = warning,
                switching = false,
                bearingDegrees = bearing ?: it.bearingDegrees,
            )
        }
        // What is due now, and nothing else. The planner decides whether this reading crossed a
        // threshold; speaking on every advance is what used to make guidance arrive after the
        // junction and never before it.
        planner.onState(update.state, speedMetersPerSecond).forEach(voice::say)
    }

    override fun onCleared() {
        voice.stop()
        super.onCleared()
    }

    private companion object {
        /**
         * How much faster than life a demonstration runs, at the very least.
         *
         * A floor, so a trip is never shown in real time, and a low one: above roughly twice
         * life nothing the voice says can be heard whole, because that is what the gap guidance
         * leaves between two sentences divided by the length of one comes to. A floor higher
         * than that does not make a demonstration livelier, it makes it silent in the middle.
         */
        const val PLAYBACK_SPEED = 2.0

        /**
         * How long one of the recorded sentences takes to say.
         *
         * Measured over the pack's 307 clips rather than guessed: they average 4.31 seconds and
         * the longest, a roundabout exit announced 250 m ahead, runs to 6.74. Four and a half
         * is a little above the average, so the usual instruction finishes with room and the
         * longest of them is the one occasionally overtaken.
         */
        const val SPOKEN_SENTENCE_SECONDS = 4.5

        /**
         * And how long the whole of it may take to watch, however long the trip is.
         *
         * It is a ceiling on the watching, not a second speed, but the two meet: the speed used
         * is whichever of the two is higher. At forty-five this cap decided every demonstration
         * — a 2.5 km drive is about 227 readings, so 227/45 is 5.04 — and quietly imposed the
         * five times that the constant above exists to refuse. It also left the voice less time
         * between instructions than an instruction takes to say, so each one cut the last.
         * Ninety keeps a demonstration of that length at the three it is meant to run at, and
         * still bounds an hour-long walk to something somebody will sit through.
         */
        const val DEMO_SECONDS = 90.0

        /**
         * Below this a trip has nothing to demonstrate: no turn, and no distance for the voice
         * to speak into. The planner's earliest warning is 150 m ahead of a maneuver, so 600 m
         * is about the shortest trip that can carry one.
         */
        const val DEMO_MIN_METERS = 600.0

        /**
         * How far up the road a demonstration starts when the real trip is shorter than that.
         *
         * A distance per traveller, because 2.5 km is a short drive and a half-hour walk. The
         * same number for all three made the walk the one that had to be played twenty times
         * life to stay watchable, which is the speed at which its voice was cut to pieces.
         */
        const val WALKING_DEMO_METERS = 800.0
        const val MOTORCYCLE_DEMO_METERS = 1_800.0
        const val DRIVING_DEMO_METERS = 2_500.0
    }
}
