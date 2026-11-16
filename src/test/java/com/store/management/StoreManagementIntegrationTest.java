package com.store.management;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full application context (controllers, services, JPA, Flyway, Actuator) against H2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoreManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads_andHealthReportsDatabaseUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"))
                .andExpect(jsonPath("$.components.db.details").doesNotExist());
    }

    @Test
    void infoEndpoint_isExposed_andEnvEndpointIsNot() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.name").value("Store Management API"));
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isNotFound());
    }

    @Test
    void openApiDocs_areAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/products']").exists())
                .andExpect(jsonPath("$.paths['/api/categories']").exists());
    }

    @Test
    void productLifecycle_withInventory() throws Exception {
        String body = """
                {"name":"Integration Lamp","sku":"it-lamp-1","category":"IT-Lighting",
                 "price":15.50,"quantity":5}
                """;
        String created = mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("IT-LAMP-1"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        // duplicate SKU (different case) is rejected
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        // search and filter
        mockMvc.perform(get("/api/products").param("name", "integration lamp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
        mockMvc.perform(get("/api/products").param("category", "it-lighting"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        // inventory
        mockMvc.perform(post("/api/products/{id}/stock/increase", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(15));
        mockMvc.perform(post("/api/products/{id}/stock/decrease", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(0));
        mockMvc.perform(post("/api/products/{id}/stock/decrease", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/products/{id}/stock", id).param("required", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));

        // update
        String update = """
                {"name":"Integration Lamp v2","sku":"IT-LAMP-1","category":"IT-Lighting",
                 "price":17.00,"quantity":3}
                """;
        mockMvc.perform(put("/api/products/{id}", id).contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Integration Lamp v2"))
                .andExpect(jsonPath("$.quantity").value(3));

        // delete
        mockMvc.perform(delete("/api/products/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void categoryLifecycle() throws Exception {
        String created = mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"IT-Garden\",\"description\":\"Outdoor\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(created);
        long id = node.get("id").asLong();

        mockMvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"it-garden\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/categories/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"IT-Garden Tools\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("IT-Garden Tools"));

        mockMvc.perform(delete("/api/categories/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/categories/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidProduct_isRejected() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"price\":0,\"quantity\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.price").value("price must be positive"));
    }

    @Test
    void unknownPath_returnsJson404() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
