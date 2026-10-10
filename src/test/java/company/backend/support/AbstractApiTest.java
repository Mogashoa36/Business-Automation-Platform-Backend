package company.backend.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Shared plumbing for the API tests.
 *
 * <p>Tokens are obtained by actually signing in rather than by injecting a mock
 * principal. That way every authenticated test also exercises the login endpoint,
 * the JWT filter and the permission lookup, so a break anywhere in the chain shows
 * up here instead of hiding behind a convenient stub.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractApiTest {

    protected static final String ADMIN_USERNAME = "admin";
    protected static final String ADMIN_PASSWORD = "TestAdmin123!";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String adminToken;

    @BeforeEach
    void signInAsAdministrator() throws Exception {
        adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
    }

    /** Signs in and returns the raw access token. */
    protected String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", username, "password", password))))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        JsonNode data = body.path("data");
        String token = data.path("accessToken").asString(null);
        if (token == null) {
            throw new IllegalStateException(
                    "Login failed for " + username + ": " + result.getResponse().getContentAsString());
        }
        return token;
    }

    /** Adds the bearer token to a request. */
    protected MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return request.header("Authorization", "Bearer " + token);
    }

    protected MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
        return withToken(request, adminToken);
    }
}