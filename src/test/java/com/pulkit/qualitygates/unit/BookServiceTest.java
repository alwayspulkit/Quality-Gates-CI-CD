package com.pulkit.qualitygates.unit;

import com.pulkit.qualitygates.exception.DuplicateResourceException;
import com.pulkit.qualitygates.exception.ResourceNotFoundException;
import com.pulkit.qualitygates.model.Book;
import com.pulkit.qualitygates.repository.BookRepository;
import com.pulkit.qualitygates.service.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository repository;

    @InjectMocks
    private BookService service;

    private Book book;

    @BeforeEach
    void setUp() {
        book = new Book("Clean Code", "Robert Martin", "978-0132350884", 35.99);
        book.setId(1L);
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {
        @Test
        @DisplayName("returns all books from repository")
        void returnsAll() {
            when(repository.findAll()).thenReturn(List.of(book));
            assertThat(service.findAll()).hasSize(1).contains(book);
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {
        @Test
        @DisplayName("returns book when found")
        void returnsBook() {
            when(repository.findById(1L)).thenReturn(Optional.of(book));
            assertThat(service.findById(1L)).isEqualTo(book);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void throwsWhenNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {
        @Test
        @DisplayName("saves and returns new book")
        void savesBook() {
            when(repository.existsByIsbn(book.getIsbn())).thenReturn(false);
            when(repository.save(book)).thenReturn(book);
            assertThat(service.create(book)).isEqualTo(book);
            verify(repository).save(book);
        }

        @Test
        @DisplayName("throws DuplicateResourceException when ISBN exists")
        void throwsOnDuplicateIsbn() {
            when(repository.existsByIsbn(book.getIsbn())).thenReturn(true);
            assertThatThrownBy(() -> service.create(book))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining(book.getIsbn());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {
        @Test
        @DisplayName("updates fields and saves")
        void updatesBook() {
            Book updated = new Book("Clean Code 2nd Ed", "Robert Martin", "978-0132350884", 39.99);
            when(repository.findById(1L)).thenReturn(Optional.of(book));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            Book result = service.update(1L, updated);

            assertThat(result.getTitle()).isEqualTo("Clean Code 2nd Ed");
            assertThat(result.getPrice()).isEqualTo(39.99);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when book does not exist")
        void throwsWhenNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(99L, book))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {
        @Test
        @DisplayName("deletes book by id")
        void deletesBook() {
            when(repository.findById(1L)).thenReturn(Optional.of(book));
            service.delete(1L);
            verify(repository).deleteById(1L);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when book does not exist")
        void throwsWhenNotFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
