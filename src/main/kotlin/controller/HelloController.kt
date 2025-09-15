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

val greetingHistory = mutableListOf<Map<String, String>>()

fun getGreeting(): String {
    val now = LocalTime.now()
    val greeting = when (now.hour) {
        in 7..13 -> "Good Morning"
        in 14..20 -> "Good Afternoon"
        else -> "Good Evening"
    }
    return greeting
}

fun addGreetingToHistory(message: String) {
    val entry = mapOf(
        "message" to message,
        "timestamp" to LocalTime.now().toString()
    )
    greetingHistory.add(entry)
    
    // Only 50 elements
    if (greetingHistory.size > 50) {
        greetingHistory.removeAt(0)
    }
}

@Controller
class HelloController(
    @param:Value("\${app.message:Hello World}") 
    private val message: String
) {
    
    @GetMapping("/")
    fun welcome(
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

@RestController
class HelloApiController {

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
    fun helloApi(
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

@RestController
class HistoryApiController {

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
    fun getHistory(): Map<String, Any> {
        return mapOf(
            "history" to greetingHistory
        )
    }
}
