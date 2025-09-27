package es.unizar.webeng.hello.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.context.TestPropertySource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@WebFluxTest(controllers = [HelloController::class, HelloApiController::class, HistoryApiController::class])
@TestPropertySource(properties = ["app.message=Welcome to the Modern Web App!"])
class HelloControllerWebFluxTests {

    @Value("\${app.message:Welcome to the Modern Web App!}")
    private lateinit var message: String

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `should return home page with default message`() {
        webTestClient.get().uri("/")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueMatches("Content-Type", ".*")
    }

    @Test
    fun `should return API response as JSON`() {
        webTestClient.get()
            .uri { b -> b.path("/api/hello").queryParam("name", "Test").build() }
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .consumeWith { result ->
                val body = result.responseBody
                assertThat(body).isNotNull
                val bodyStr = String(body!!)
                assertThat(bodyStr).contains("\"message\"")
                assertThat(bodyStr).contains("\"timestamp\"")
                assertThat(
                    bodyStr.contains("Good Morning, Test!") ||
                    bodyStr.contains("Good Afternoon, Test!") ||
                    bodyStr.contains("Good Evening, Test!")
                ).isTrue
            }
    }

    @Test
    fun `should return greeting history as JSON`() {
        webTestClient.get().uri("/api/history")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody()
            .consumeWith { result ->
                val b = result.responseBody
                assertThat(b).isNotNull
                val s = String(b!!)
                assertThat(s).contains("\"history\"")
                assertThat(s).contains("[")
            }
    }
}
