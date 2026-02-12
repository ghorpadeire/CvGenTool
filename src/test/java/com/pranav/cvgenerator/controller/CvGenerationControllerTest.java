/**
 * CvGenerationControllerTest.java
 *
 * Integration tests for CvGenerationController.
 * Tests the REST API endpoints using MockMvc.
 *
 * @author Pranav Ghorpade
 */
package com.pranav.cvgenerator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pranav.cvgenerator.model.CvGenerationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the CV Generation REST API.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CvGenerationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/generate should return 400 for empty job description")
    void shouldRejectEmptyJobDescription() throws Exception {
        // Given
        CvGenerationRequest request = CvGenerationRequest.builder()
                .jobDescription("")
                .build();

        // When/Then
        mockMvc.perform(post("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/generate should return 400 for short job description")
    void shouldRejectShortJobDescription() throws Exception {
        // Given
        CvGenerationRequest request = CvGenerationRequest.builder()
                .jobDescription("Too short")
                .build();

        // When/Then
        mockMvc.perform(post("/api/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/status/{id} should return 404 for non-existent ID")
    void shouldReturn404ForNonExistentId() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/status/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/status/{id} should return 400 for invalid UUID")
    void shouldReturn400ForInvalidUuid() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/status/invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/result/{id} should return 404 for non-existent ID")
    void shouldReturn404ForNonExistentResult() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/result/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/cache/{id} should return 404 for non-existent ID")
    void shouldReturn404ForDeletingNonExistent() throws Exception {
        // When/Then
        mockMvc.perform(delete("/api/cache/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Home page should be accessible")
    void homePageShouldBeAccessible() throws Exception {
        // When/Then
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    @DisplayName("History page should be accessible")
    void historyPageShouldBeAccessible() throws Exception {
        // When/Then
        mockMvc.perform(get("/history"))
                .andExpect(status().isOk())
                .andExpect(view().name("history"));
    }

    @Test
    @DisplayName("Result page should return error for non-existent CV")
    void resultPageShouldHandleMissingCv() throws Exception {
        // When/Then
        mockMvc.perform(get("/result/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("error"));
    }
}
