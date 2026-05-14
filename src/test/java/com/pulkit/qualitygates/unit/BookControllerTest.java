package com.pulkit.qualitygates.unit;

import com.pulkit.qualitygates.controller.BookController;
import com.pulkit.qualitygates.exception.ResourceNotFoundException;
import com.pulkit.qualitygates.model.Book;
import com.pulkit.qualitygates.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookService service;

    @InjectMocks
    private BookController controller;

    private Book book;

    @BeforeEach
    void setUp() {
        book = new Book("Clean Code", "Robert Martin", "978-0132350884", 35.99);
        book.setId(1L);
    }

    @Nested
    @DisplayName("getAll")
    class GetAll {
        @Test
        @DisplayName("returns all books")
        void returnsAllBooks() {
            when(service.findAll()).thenReturn(List.of(book));

            List<Book> result = controller.getAll();

            assertThat(result).hasSize(1).contains(book);
            verify(service).findAll();
        }

        @Test
        @DisplayName("returns empty list when no books exist")
        void returnsEmptyList() {
            when(service.findAll()).thenReturn(List.of());

            List<Book> result = controller.getAll();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {
        @Test
        @DisplayName("returns book with 200 status")
        void returnsBook() {
            when(service.findById(1L)).thenReturn(book);

            ResponseEntity<Book> result = controller.getById(1L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo(book);
            verify(service).findById(1L);
        }

        @Test
        @DisplayName("throws when book not found")
        void throwsWhenNotFound() {
            when(service.findById(99L)).thenThrow(new ResourceNotFoundException("Book not found"));

            assertThatThrownBy(() -> controller.getById(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {
        @Test
        @DisplayName("returns created book with 201 status")
        void createsBook() {
            when(service.create(any(Book.class))).thenReturn(book);

            ResponseEntity<Book> result = controller.create(book);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(result.getBody()).isEqualTo(book);
            verify(service).create(book);
        }
    }

    @Nested
    @DisplayName("update")
    class Update {
        @Test
        @DisplayName("returns updated book with 200 status")
        void updatesBook() {
            Book updated = new Book("Updated Title", "Author", "978-0132350884", 39.99);
            when(service.update(eq(1L), any(Book.class))).thenReturn(updated);

            ResponseEntity<Book> result = controller.update(1L, updated);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(result.getBody()).isEqualTo(updated);
            verify(service).update(1L, updated);
        }

        @Test
        @DisplayName("throws when book not found")
        void throwsWhenNotFound() {
            when(service.update(eq(99L), any(Book.class)))
                    .thenThrow(new ResourceNotFoundException("Book not found"));

            assertThatThrownBy(() -> controller.update(99L, book))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {
        @Test
        @DisplayName("returns 204 No Content")
        void deletesBook() {
            ResponseEntity<Void> result = controller.delete(1L);

            assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(service).delete(1L);
        }

        @Test
        @DisplayName("throws when book not found")
        void throwsWhenNotFound() {
            doThrow(new ResourceNotFoundException("Book not found"))
                    .when(service).delete(99L);

            assertThatThrownBy(() -> controller.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
