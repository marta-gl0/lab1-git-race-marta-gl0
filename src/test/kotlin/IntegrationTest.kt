package es.unizar.webeng.hello

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class IntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var client: WebTestClient

    @BeforeEach
    fun setup() {
        client = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun `should return home page with modern title and client-side HTTP debug`() {
        client.get().uri("/")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .consumeWith { res ->
                val body = res.responseBody ?: ""
                assertThat(body).contains("<title>Modern Web App</title>")
                assertThat(body).contains("Welcome to Modern Web App")
                assertThat(body).contains("Interactive HTTP Testing & Debug")
                assertThat(body).contains("Client-Side Educational Tool")
            }
    }

    @Test
    fun `should return personalized greeting when name is provided`() {
        client.get().uri { b -> b.path("/").queryParam("name", "Developer").build() }
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .consumeWith { res ->
                val body = res.responseBody ?: ""
                assertThat(body).containsAnyOf(
                    "Good Morning, Developer!",
                    "Good Afternoon, Developer!",
                    "Good Evening, Developer!"
                )
            }
    }

    @Test
    fun `should return API response with timestamp`() {
        client.get().uri { b -> b.path("/api/hello").queryParam("name", "Test").build() }
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody(String::class.java)
            .consumeWith { res ->
                val body = res.responseBody ?: ""
                assertThat(body).containsAnyOf(
                    "Good Morning, Test!",
                    "Good Afternoon, Test!",
                    "Good Evening, Test!"
                )
                assertThat(body).contains("timestamp")
            }
    }

    @Test
    fun `should serve Bootstrap CSS correctly`() {
        client.get().uri("/webjars/bootstrap/5.3.3/css/bootstrap.min.css")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.valueOf("text/css"))
            .expectBody(String::class.java)
            .consumeWith { res ->
                val body = res.responseBody ?: ""
                assertThat(body).contains("body")
            }
    }

    @Test
    fun `should expose actuator health endpoint`() {
        client.get().uri("/actuator/health")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .consumeWith { res ->
                val body = res.responseBody ?: ""
                assertThat(body).contains("UP")
            }
    }

    @Test
    fun `should display client-side HTTP debug interface`() {
        client.get().uri { b -> b.path("/").queryParam("name", "Student").build() }
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .consumeWith { res ->
                val body = res.responseBody ?: ""
                assertThat(body).contains("Interactive HTTP Testing & Debug")
                assertThat(body).contains("Client-Side Educational Tool")
                assertThat(body).contains("Web Page Greeting")
                assertThat(body).contains("API Endpoint")
                assertThat(body).contains("Health Check")
                assertThat(body).contains("Learning Notes:")
            }
    }

    @Test
    fun `should return greeting history when requested`() {
        client.get().uri { b -> b.path("/api/hello").queryParam("name", "Seed").build() }
            .exchange()
            .expectStatus().isOk

        client.get().uri("/api/history")
            .exchange()
            .expectStatus().isOk
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
            .expectBody(String::class.java)
            .consumeWith { res ->
                val body = res.responseBody ?: ""
                assertThat(body).contains("\"history\"")
            }
    }
}
