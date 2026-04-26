package com.innowise.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.orderservice.dto.ItemDto;
import com.innowise.orderservice.entity.Item;
import com.innowise.orderservice.repository.ItemDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;

class ItemControllerTest extends BaseIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ItemDao itemDao;

    @BeforeEach
    void setUp() {
        itemDao.deleteAll();
    }

    @Nested
    @DisplayName("Create Item Integration Tests")
    class CreateItemTests {

        @Test
        @DisplayName("Should create item and save it to PostgreSQL")
        void shouldCreateItemSuccessfully() throws Exception {
            ItemDto itemDto = new ItemDto(null, "Drill", 2500L);

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/items")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(itemDto)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isCreated())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").isNotEmpty())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.name").value("Drill"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.price").value(2500));

            assertEquals(1, itemDao.findAll().size());
            assertTrue(itemDao.existsByName("Drill"));
        }

        @Test
        @DisplayName("Should return error when creating item with existing name")
        void shouldReturnErrorWhenItemNameExists() throws Exception {
            com.innowise.orderservice.entity.Item existingItem = com.innowise.orderservice.entity.Item.builder()
                    .name("Saw")
                    .price(1000L)
                    .build();
            itemDao.save(existingItem);

            ItemDto duplicateDto = new ItemDto(null, "Saw", 5000L);

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/items")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(duplicateDto)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict());
        }

        @Test
        @DisplayName("Should return 400 when name is null (Validation check)")
        void shouldReturnBadRequestWhenNameIsNull() throws Exception {
            ItemDto invalidDto = new ItemDto(null, null, 100L);

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/items")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get Item By Id Integration Tests")
    class GetItemByIdTests {

        @Test
        @DisplayName("Should return item when it exists in database")
        void shouldReturnItemWhenExists() throws Exception {
            Item item = Item.builder()
                    .name("Screwdriver")
                    .price(450L)
                    .build();
            Item savedItem = itemDao.save(item);
            Long id = savedItem.getId();

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/items/{id}", id))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(id))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.name").value("Screwdriver"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.price").value(450));
        }

        @Test
        @DisplayName("Should return 404 when item does not exist")
        void shouldReturn404WhenNotFound() throws Exception {
            Long nonExistentId = 999L;

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/items/{id}", nonExistentId))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when ID is invalid (e.g. string instead of long)")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/items/abc"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Get All Items Integration Tests")
    class GetAllItemsTests {

        @Test
        @DisplayName("Should return page of items with default pagination and sorting")
        void shouldReturnAllItemsWithDefaultPagination() throws Exception {
            itemDao.save(Item.builder().name("Axe").price(1000L).build());
            itemDao.save(Item.builder().name("Hammer").price(500L).build());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/items"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content.length()").value(2))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalElements").value(2))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].name").value("Axe"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[1].name").value("Hammer"));
        }

        @Test
        @DisplayName("Should filter items by name correctly")
        void shouldFilterItemsByName() throws Exception {
            itemDao.save(Item.builder().name("Drill").price(2000L).build());
            itemDao.save(Item.builder().name("Saw").price(1500L).build());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/items")
                            .param("name", "Drill"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content.length()").value(1))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content[0].name").value("Drill"));
        }

        @Test
        @DisplayName("Should return empty page when no items match criteria")
        void shouldReturnEmptyPage() throws Exception {
            itemDao.save(Item.builder().name("Pliers").price(300L).build());

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/items")
                            .param("name", "NonExistent"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.content").isEmpty())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Should apply custom pagination parameters")
        void shouldApplyCustomPagination() throws Exception {
            for (int i = 1; i <= 3; i++) {
                itemDao.save(Item.builder().name("Item " + i).price(100L * i).build());
            }

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/items")
                            .param("size", "1")
                            .param("page", "0"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.size").value(1))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.numberOfElements").value(1));
        }
    }

    @Nested
    @DisplayName("Update Item Integration Tests")
    class UpdateItemTests {

        @Test
        @DisplayName("Should successfully update item in database")
        void shouldUpdateItemSuccessfully() throws Exception {
            com.innowise.orderservice.entity.Item existingItem = com.innowise.orderservice.entity.Item.builder()
                    .name("Old Name")
                    .price(100L)
                    .build();
            existingItem = itemDao.save(existingItem);
            Long id = existingItem.getId();

            ItemDto updateDto = new ItemDto(id, "New Name", 500L);

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/items")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.id").value(id))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.name").value("New Name"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.price").value(500));

            com.innowise.orderservice.entity.Item updatedInDb = itemDao.findById(id).orElseThrow();
            assertEquals("New Name", updatedInDb.getName());
            assertEquals(500L, updatedInDb.getPrice());
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent item")
        void shouldReturn404WhenItemNotFound() throws Exception {
            ItemDto updateDto = new ItemDto(999L, "Ghost Item", 1000L);

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/items")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when price is negative")
        void shouldReturn400WhenValidationFails() throws Exception {
            ItemDto invalidDto = new ItemDto(1L, "Broken Item", -100L);

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/items")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto)))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Delete Item Integration Tests")
    class DeleteItemTests {

        @Test
        @DisplayName("Should delete item successfully from database")
        void shouldDeleteItemSuccessfully() throws Exception {
            com.innowise.orderservice.entity.Item item = com.innowise.orderservice.entity.Item.builder()
                    .name("Sledgehammer")
                    .price(3000L)
                    .build();
            item = itemDao.save(item);
            Long id = item.getId();

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/items/{id}", id))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNoContent());

            assertFalse(itemDao.existsById(id));
        }

        @Test
        @DisplayName("Should return 404 when deleting non-existent item")
        void shouldReturn404WhenNotFound() throws Exception {
            Long nonExistentId = 999L;

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/items/{id}", nonExistentId))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 when ID format is invalid")
        void shouldReturn400WhenIdIsInvalid() throws Exception {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/items/invalid-id"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        }
    }
}