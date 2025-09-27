package es.unizar.webeng.hello.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.time.LocalTime
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.media.ExampleObject
import java.util.concurrent.ConcurrentLinkedDeque
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Keeps a rolling history of recent greetings.
 *
 * Each entry is a `Map<String, String>` with the keys:
 *  - "message": the greeting text
 *  - "timestamp": the time the entry was added (LocalTime.toString())
 *
 * Implementation details:
 * - Stored in a double-ended queue (`ConcurrentLinkedDeque`) to allow fast insertions at the front
 * and removals from the back.
 * - The history is capped at `MAX_HISTORY` (50). When the limit is exceeded, the oldest entries
 * are discarded.
 * - Modifications are wrapped with `historyMutex` (a coroutine `Mutex`) to ensure thread-safety
 * across concurrent coroutines.
 */
private val MAX_HISTORY = 50
private val greetingHistory = ConcurrentLinkedDeque<Map<String, String>>()
private val historyMutex = Mutex()

/**
 * Returns a greeting string based on the current local time.
 *
 * Behavior:
 *  - Hours 07..13 (07:00 through 13:59) -> "Good Morning"
 *  - Hours 14..20 (14:00 through 20:59) -> "Good Afternoon"
 *  - All other hours -> "Good Evening"
 *
 * This function uses `LocalTime.now()` and selects the greeting purely from the
 * current local hour (no localization of wording is performed).
 *
 * @return One of `"Good Morning"`, `"Good Afternoon"`, or `"Good Evening"`.
 * @see java.time.LocalTime
 */
fun getGreeting(): String {
    val now = LocalTime.now()
    val greeting = when (now.hour) {
        in 7..13 -> "Good Morning"
        in 14..20 -> "Good Afternoon"
        else -> "Good Evening"
    }
    return greeting
}

/**
 * Appends a greeting message with the current time to [greetingHistory]. 
 * Suspends while acquiring the mutex to append.
 *
 * Behavior:
 * - Creates an entry with keys `message` (the provided message) and `timestamp` (`LocalTime.now().toString()`).
 * - Uses `historyMutex.withLock` to safely modify the queue across coroutines.
 * - Adds the entry at the front of the queue and removes entries from the back until the size
 * is within `MAX_HISTORY`.
 *
 * Implementation notes:
 * - This function is `suspend` because it acquires a coroutine `Mutex`.
 * - `timestamp` uses `LocalTime`; it does not contain time zone or absolute time information.
 *
 * @param message The greeting text to store in the history.
 */
suspend fun addGreetingToHistory(message: String) {
    val entry = mapOf(
        "message" to message,
        "timestamp" to LocalTime.now().toString()
    )

    historyMutex.withLock {
        greetingHistory.addFirst(entry)
        while (greetingHistory.size > MAX_HISTORY) {
            greetingHistory.removeLast()
        }
    }
}

/**
 * MVC controller that handles requests to the root path ("/") and returns the "welcome" view.
 *
 * The controller injects a configurable fallback message from the `app.message` property,
 * defaulting to `"Hello World"` when the property is not provided.
 *
 * Behavior:
 *  - If the request `name` parameter is blank -> the view receives the injected fallback `[message]`.
 *  - If the request `name` parameter is non-blank -> a personalized greeting is created using
 *    `[getGreeting]` and the `name`, the greeting is recorded via `[addGreetingToHistory]`, and
 *    the personalized greeting is passed to the view.
 *
 * Notes:
 * - The controller is stateless, but it calls `addGreetingToHistory`, which mutates the shared
 * `greetingHistory`.
 * - The method is `suspend` because it may call the coroutine-based history function.
 *
 * @property message The fallback message injected from the Spring property `app.message`.
 * Example property:
 * ```properties
 * app.message=Welcome to the site!
 * ```
 * If the property is absent, the default value `"Hello World"` is used.
 *
 * @see getGreeting
 * @see addGreetingToHistory
 */
@Controller
class HelloController(
    @param:Value("\${app.message:Hello World}") 
    private val message: String
) {
    
    /**
     * Handles GET requests to `/` and populates the model for the `welcome` view.
     *
     * Request handling details:
     *  - Reads the optional `name` request parameter.
     *  - If `name` is not blank, computes `"{getGreeting()}, {name}!"` and records it in history.
     *  - Adds two model attributes:
     *      * `"message"` -> the greeting text that the view will render
     *      * `"name"` -> the raw `name` parameter (may be empty)
     *
     * Example HTTP request:
     * ```
     * GET /?name=Alice
     * ```
     *
     * @param model Spring MVC `Model` used to expose attributes to the view layer.
     * @param name Optional request parameter. Defaults to an empty string when not provided.
     * @return Logical view name `"welcome"` which should be resolved by the view resolver.
     *
     * @see Model
     * @see RequestParam
     */
    @GetMapping("/")
    suspend fun welcome(
        model: Model,
        @RequestParam(defaultValue = "") name: String
    ): String {
        val greeting = if (name.isNotBlank()) "${getGreeting()}, $name!" else message
        model.addAttribute("message", greeting)
        model.addAttribute("name", name)
        if (name.isNotBlank()) {
            addGreetingToHistory(greeting)
        }
        return "welcome"
    }
}

/**
 * REST controller that exposes a simple greeting API.
 *
 * The controller exposes a single endpoint `/api/hello` that returns a JSON object
 * containing a greeting message and a timestamp. The greeting is always produced by
 * combining the localized time-based result of [getGreeting] with the provided `name`.
 *
 * The controller calls [addGreetingToHistory] to record each generated greeting in the
 * shared `greetingHistory`.
 *
 * @see getGreeting
 * @see addGreetingToHistory
 */
@RestController
class HelloApiController {

    /**
     * GET endpoint that returns a JSON map with keys `"message"` and `"timestamp"`.
     *
     * Behavior:
     *  - Builds a greeting using `[getGreeting]` and the provided `name`. Example: `"Good Morning, Alice!"`.
     *  - Records the generated greeting via `[addGreetingToHistory]`.
     *  - Returns a `Map<String, String>` with:
     *      * `"message"`: the greeting text
     *      * `"timestamp"`: `LocalTime.now().toString()` at the moment of response creation
     *
     * OpenAPI / Documentation notes:
     *  - The method is annotated with OpenAPI metadata (summary, description, responses)
     *    so the generated API docs will include a JSON example and response schema.
     *
     * Example successful JSON response:
     * ```
     * {"message":"Good Morning, World!","timestamp":"11:03:50.856953900"}
     * ```
     *
     * @param name Name of the person to greet. If omitted, defaults to `"World"`.
     * @return A `Map<String, String>` that will be serialized as JSON:
     *  - `"message"` -> greeting text (string)
     *  - `"timestamp"` -> string representation of the local time when the response was created
     *
     * @see LocalTime
     */
    @Operation(
        summary = "Greets user",
        description = "Returns a personalized greeting message with a timestamp",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Greeting generated correctly",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(
                        description = "Object with keys 'message' and 'timestamp' (string values)",
                        type = "object"
                    ),
                    additionalPropertiesSchema = Schema(
                        description = "Values of the map (all strings)",
                        type = "string",
                        example = "Good Morning, World!"
                    ),
                    examples = [ExampleObject(
                        value = """{"message":"Good Morning, World!","timestamp":"11:03:50.856953900"}"""
                    )]
                )]
            )
        ]
    )
    @GetMapping("/api/hello", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun helloApi(
            @Parameter(description = "Name of who receives the greeting", required = false)
            @RequestParam(defaultValue = "World") name: String
        ): Map<String, String> {
        val greeting = "${getGreeting()}, $name!"
        addGreetingToHistory(greeting)

        return mapOf(
            "message" to greeting,
            "timestamp" to LocalTime.now().toString()
        )
    }
}

/**
 * REST controller that provides access to the greetings history.
 * The controller returns a JSON object with a single key `"history"` whose value is the
 * current snapshot of the shared `[greetingHistory]`. Each element inside that list is a
 * `Map<String, String>` with the keys:
 *  - `"message"`: the greeting text
 *  - `"timestamp"`: the time the greeting was created (as `LocalTime.toString()`)
 *
 * Implementation notes:
 * - `greetingHistory` is capped at `MAX_HISTORY` entries.
 * - Uses `ArrayList(greetingHistory)` to take a snapshot and avoid exposing the concurrent queue
 * directly to the JSON serializer.
 *
 * @see greetingHistory
 */
@RestController
class HistoryApiController {

    /**
     * GET endpoint that returns the list of recent greetings.
     *
     * Behavior:
     *  - Returns a `Map<String, Any>` where the `"history"` key maps to the current value of
     *    `[greetingHistory]`.
     *  - The response is serialized as JSON with media type `application/json`.
     *
     * Example successful JSON response:
     * ```
     * {
     *   "history": [
     *     {"message":"Good Morning, World!","timestamp":"11:32:56.134772300"},
     *     {"message":"Good Morning, World!","timestamp":"11:32:57.903462500"},
     *     {"message":"Good Morning, World!","timestamp":"11:32:58.712779700"}
     *   ]
     * }
     * ```
     *
     * @return A `Map<String, Any>` containing the `"history"` key whose value is the current list
     *         of greeting entries (each entry is a `Map<String, String>` with `message` and `timestamp`).
     *
     * @see MediaType.APPLICATION_JSON_VALUE
     * @see greetingHistory
     */
    @Operation(
        summary = "Get greetings history",
        description = "Returns the list of previously generated greetings",
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "History retrieved correctly",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(
                            description = "Object with key 'history' which contains a list of greetings (strings)",
                            type = "object"
                        ),
                        additionalPropertiesSchema = Schema(
                            description = "'history' is an array of greeting objects with 'message' and 'timestamp'",
                            type = "array",
                            example = "[{\"message\":\"Good Morning, World!\",\"timestamp\":\"11:32:56.134772300\"},{\"message\":\"Good Morning, World!\",\"timestamp\":\"11:32:57.903462500\"},{\"message\":\"Good Morning, World!\",\"timestamp\":\"11:32:58.712779700\"}]"
                        ),
                        examples = [ExampleObject(
                            value = "{\"history\":[{\"message\":\"Good Morning, World!\",\"timestamp\":\"11:32:56.134772300\"},{\"message\":\"Good Morning, World!\",\"timestamp\":\"11:32:57.903462500\"},{\"message\":\"Good Morning, World!\",\"timestamp\":\"11:32:58.712779700\"}]}"
                        )]
                    )
                ]
            )
        ]
    )
    @GetMapping("/api/history", produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getHistory(): Map<String, Any> {
        return mapOf("history" to ArrayList(greetingHistory))
    }
}
