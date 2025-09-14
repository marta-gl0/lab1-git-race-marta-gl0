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
    
    @GetMapping("/api/hello", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun helloApi(@RequestParam(defaultValue = "World") name: String): Map<String, String> {
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

    @GetMapping("/api/history", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getHistory(): Map<String, Any> {
        return mapOf(
            "history" to greetingHistory
        )
    }
}
