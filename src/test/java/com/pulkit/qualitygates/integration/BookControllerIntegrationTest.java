package com.pulkit.qualitygates.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulkit.qualitygates.model.Book;
import com.pulkit.qualitygates.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/books returns empty list when no books exist")
    void getAllEmpty() throws Exception {
        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("POST /api/books creates a book and returns 201")
    void createBook() throws Exception {
        Book book = new Book("The Pragmatic Programmer", "David Thomas", "978-0135957059", 49.99);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("The Pragmatic Programmer")))
                .andExpect(jsonPath("$.isbn", is("978-0135957059")));
    }

    @Test
    @DisplayName("POST /api/books returns 409 when ISBN already exists")
    void createDuplicateBook() throws Exception {
        Book book = new Book("The Pragmatic Programmer", "David Thomas", "978-0135957059", 49.99);
        repository.save(book);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/books returns 400 when title is blank")
    void createInvalidBook() throws Exception {
        Book book = new Book("", "Author", "978-0000000000", 10.0);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/books/{id} returns 404 when book not found")
    void getByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/books/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/books/{id} returns book when found")
    void getById() throws Exception {
        Book saved = repository.save(new Book("Refactoring", "Martin Fowler", "978-0201485677", 44.99));

        mockMvc.perform(get("/api/books/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Refactoring")));
    }

    @Test
    @DisplayName("PUT /api/books/{id} updates the book")
    void updateBook() throws Exception {
        Book saved = repository.save(new Book("Old Title", "Author", "978-1111111111", 20.0));
        Book updated = new Book("New Title", "Author", "978-1111111111", 25.0);

        mockMvc.perform(put("/api/books/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("New Title")));
    }

    @Test
    @DisplayName("DELETE /api/books/{id} removes the book")
    void deleteBook() throws Exception {
        Book saved = repository.save(new Book("To Delete", "Author", "978-2222222222", 15.0));

        mockMvc.perform(delete("/api/books/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/books/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/books returns multiple books")
    void getAllWithBooks() throws Exception {
        repository.save(new Book("Book 1", "Author 1", "978-1111111111", 10.0));
        repository.save(new Book("Book 2", "Author 2", "978-2222222222", 20.0));
        repository.save(new Book("Book 3", "Author 3", "978-3333333333", 30.0));

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    @DisplayName("POST /api/books returns 400 when author is blank")
    void createBookBlankAuthor() throws Exception {
        Book book = new Book("Valid Title", "", "978-0000000000", 10.0);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/books returns 400 when ISBN is blank")
    void createBookBlankIsbn() throws Exception {
        Book book = new Book("Valid Title", "Valid Author", "", 10.0);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/books returns 400 when price is negative")
    void createBookNegativePrice() throws Exception {
        Book book = new Book("Valid Title", "Valid Author", "978-0000000000", -10.0);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/books returns 400 when price is zero")
    void createBookZeroPrice() throws Exception {
        Book book = new Book("Valid Title", "Valid Author", "978-0000000000", 0.0);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/books returns 400 with multiple validation errors")
    void createBookMultipleValidationErrors() throws Exception {
        Book book = new Book("", "", "", -5.0);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(book)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/books/{id} returns 400 when author is blank")
    void updateBookBlankAuthor() throws Exception {
        Book saved = repository.save(new Book("Original", "Original Author", "978-1111111111", 20.0));
        Book updated = new Book("Updated", "", "978-1111111111", 25.0);

        mockMvc.perform(put("/api/books/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/books/{id} returns 404 when book not found")
    void updateBookNotFound() throws Exception {
        Book updated = new Book("Updated", "Author", "978-1111111111", 25.0);

        mockMvc.perform(put("/api/books/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/books/{id} returns 404 when book not found")
    void deleteBookNotFound() throws Exception {
        mockMvc.perform(delete("/api/books/999"))
                .andExpect(status().isNotFound());
    }
}
