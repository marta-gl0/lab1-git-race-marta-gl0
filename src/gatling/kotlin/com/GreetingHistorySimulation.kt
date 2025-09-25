package com

import io.gatling.javaapi.core.CoreDsl.*
import io.gatling.javaapi.http.HttpDsl.*
import io.gatling.javaapi.core.Simulation
import java.time.Duration

class GreetingHistorySimulation : Simulation() {

    // HTTP protocol configuration
    private val httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")

    // Dynamic feeder: generates unique user names (User1, User2, ...)
    private val feeder = generateSequence(1) { it + 1 }
        .map { mapOf("name" to "User$it") }
        .iterator()

    // Define the scenario: what each virtual user will do
    private val scn = scenario("GreetingHistoryScenario")
        .feed(feeder)
        .exec(
            http("Create greeting")
                .get("/api/hello")  // First request: call /api/hello
                .queryParam("name", "#{name}")
                .check(status().`is`(200))
        )
        .pause(Duration.ofMillis(20))
        .exec(
            http("Get history")
                .get("/api/history")    // Second request: call /api/history
                .check(
                    status().`is`(200),
                    jsonPath("$.history[*]").count().lte(50)
                    // Verify that the size of "history" array is <= 50 (as per app logic)
                )
        )

    init {
        setUp(
            scn.injectOpen(
                // First phase: ramp up 100 users gradually over 30 seconds
                rampUsers(100).during(Duration.ofSeconds(30)),
                // Second phase: sustain 50 constant users per second for 60 seconds
                constantUsersPerSec(50.0).during(Duration.ofSeconds(60))
            )
        ).protocols(httpProtocol)
    }
}
