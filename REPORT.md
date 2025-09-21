# Lab 1 Git Race -- Project Report

## Description of Changes
The aspects that have been implemented in this practice are the following:

**- Time-based greeting:** When the buttons *Test Web Page* or *Test /api/hello* are pressed, a greeting is generated based on the current time. This greeting can be *Good Morning*, *Good Afternoon* or *Good Evening*. Specifically:  
  - Between 7:00 and 13:00 → *Good Morning*  
  - Between 14:00 and 20:00 → *Good Afternoon*  
  - At other times → *Good Evening*

**- Dark/Light mode:** A new button has been added to the navigation bar to toggle between dark and light themes. Clicking the button switches the theme, adapting the entire user interface accordingly. Thanks to local storage, the last selected theme is preserved even if the server is stopped and restarted.

**- Greeting History:** Users can view the last 50 greetings generated on the server, each with its corresponding timestamp. Whenever a greeting is created, it is stored in an array that serves as the history. Note that when the server is stopped and restarted, the array is cleared, due to the lack of persistance in the server (there is no database).

**- Github Actions:** The existing tests have been updated to reflect the modifications made to the controllers. Additionally, new tests have been created for the new greeting history controller.

**- OpenAPI documentation:** Springdoc has been added as a dependency to enable Swagger documentation. All endpoints of the REST controllers (*HelloApiController* and *HistoryApiController*) have been documented using annotations in the code. This documentation can be accessed at: http://localhost:8080/swagger-ui/index.html

## Technical Decisions

**- Swagger / OpenAPI:** Given the experience and successful use of Swagger/OpenAPI in previous projects, this standard was adopted once again for API documentation. Its implementation promotes consistency, leverages existing team knowledge, and minimizes the onboarding time for new developers.

## Learning Outcomes

**Kotlin & Spring Boot**
- Incorporated Kotlin into a project for the first time, gaining practical experience with this technology.
- Explored how Spring Boot organizes applications through its layered architecture:
  - Services: Encapsulating business logic.
  - Controllers: Processing HTTP requests and generating responses.

**Server-Side Web Development**
- Acquired knowledge on building server-rendered applications using Spring Boot in combination with Thymeleaf.
- Practiced integrating backend functionality directly into frontend templates, rather than adopting a fully decoupled frontend-backend structure.

**Testing**
- Strengthened understanding of different testing strategies and their objectives:
  - MockMvc Tests: Evaluating controller behavior without deploying a full server.
  - Integration Tests: Verifying interactions across multiple layers of the system.

## AI Disclosure
### AI Tools Used
- ChatGPT

### AI-Assisted Work

**- Describe what was generated with AI assistance:** All the KDoc documentation and the OpenAPI/Swagger annotations added to *HelloController.kt*; the implemented modifications and newly added tests under *src/main/test*; and the additional CSS rules introduced in *styles.css*.

**- Percentage of AI-assisted vs. original work:** Approximately 50% AI-assisted / 50% human-authored and reviewed — the AI generated initial KDoc blocks, OpenAPI annotations and test/style scaffolding; a human developer performed review, corrections, completion, integration and final validation.

**- Any modifications made to AI-generated code:**
  - Content corrections in KDoc blocks (clarified summaries).
  - Import reordering, formatting adjustments and minor refactorings to satisfy project linting and style rules.
  - Hardening and completion of generated tests (additional assertions, fixtures and teardown/cleanup).
  - Refinement of CSS selectors and rules to ensure visual consistency and prevent regressions with the existing design system.
  - Integration adjustments to ensure the generated artifacts build and run within the project's CI pipeline.

### Original Work

**- Core application logic:** Implemented significant changes within controllers and the business layer to refine and enhance the application’s functionality.

**- Learning Process:** Gained practical experience with Kotlin and Spring Boot’s layered architecture, as well as testing workflows through GitHub Actions. This learning was reinforced by actively integrating and adapting AI-driven recommendations into the project.