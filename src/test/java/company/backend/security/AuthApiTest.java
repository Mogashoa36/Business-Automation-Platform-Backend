package company.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import company.backend.support.AbstractApiTest;

import tools.jackson.databind.JsonNode;

/**
 * End-to-end coverage of the authentication chain. These are the tests that matter
 * most in a template: everything else assumes a caller has already been let in.
 *
 * <p>Tokens are obtained by really signing in, so the login endpoint, the JWT
 * filter and the permission lookup are all exercised rather than stubbed.
 */
class AuthApiTest extends AbstractApiTest {

    @Test
    @DisplayName("signing in with valid credentials returns a token and the caller's permissions")
    void loginSucceeds() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", ADMIN_USERNAME, "password", ADMIN_PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.data.user.permissions").isNotEmpty())
                .andReturn();

        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(data.path("expiresAt").asString()).isNotBlank();
    }

    @Test
    @DisplayName("the login response never contains a password field")
    void loginResponseOmitsCredentials() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", ADMIN_USERNAME, "password", ADMIN_PASSWORD))))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(ADMIN_PASSWORD);
        assertThat(body.toLowerCase(java.util.Locale.ROOT)).doesNotContain("\"password\"");
    }

    @Test
    @DisplayName("a wrong password is rejected and no token is issued")
    void loginWithWrongPasswordFails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", ADMIN_USERNAME, "password", "not-the-password"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("an unknown account is indistinguishable from a wrong password")
    void loginForUnknownUserIsIndistinguishable() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "ghost", "password", "whatever-long-enough"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("repeated failures lock the account")
    void repeatedFailuresLockTheAccount() throws Exception {
        createUser("lock-me");

        for (int attempt = 0; attempt < 6; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("username", "lock-me", "password", "wrong-password"))))
                    .andExpect(status().isBadRequest());
        }

        // Even the correct password is now refused.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "lock-me", "password", "CorrectHorse1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("locked")));
    }

    @Test
    @DisplayName("protected endpoints reject an anonymous caller")
    void anonymousAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a malformed or forged token is rejected")
    void invalidTokensAreRejected() throws Exception {
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());

        // Correctly structured but signed with a different key.
        String forged = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.wrong-signature";
        mockMvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("an authenticated caller reaches a permission-protected endpoint")
    void authenticatedAccessIsAllowed() throws Exception {
        mockMvc.perform(asAdmin(get("/api/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("the caller's own identity and permissions are readable")
    void currentUserIsReadable() throws Exception {
        mockMvc.perform(asAdmin(get("/api/profile")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.data.roles").isArray())
                .andExpect(jsonPath("$.data.permissions").isNotEmpty());
    }

    @Test
    @DisplayName("a refresh token can be exchanged exactly once")
    void refreshTokenIsSingleUse() throws Exception {
        createUser("rotate-me");
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "rotate-me", "password", "CorrectHorse1"))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(loginResult.getResponse().getContentAsString()).path("data");
        String refreshToken = data.path("refreshToken").asString();

        // First use succeeds and returns a new token pair.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isOk());

        // Replaying the consumed token is treated as a credential compromise.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    @DisplayName("forgot-password never reveals whether an account exists")
    void forgotPasswordIsNotEnumerable() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", ADMIN_USERNAME + "@test.local"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "nobody@nowhere.local"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("validation failures are reported per field")
    void validationErrorsAreStructured() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors[0].field").exists())
                .andExpect(jsonPath("$.fieldErrors[0].message").exists());
    }

    /** Creates a user through the API and returns its id. */
    private Long createUser(String username) throws Exception {
        MvcResult result = mockMvc.perform(asAdmin(post("/api/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "email", username + "@test.local",
                                "password", "CorrectHorse1",
                                "firstName", "Test",
                                "lastName", "User",
                                "roleCodes", List.of("EMPLOYEE")))))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();
    }
}