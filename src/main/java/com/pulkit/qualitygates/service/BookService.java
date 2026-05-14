package com.pulkit.qualitygates.service;

import com.pulkit.qualitygates.exception.DuplicateResourceException;
import com.pulkit.qualitygates.exception.ResourceNotFoundException;
import com.pulkit.qualitygates.model.Book;
import com.pulkit.qualitygates.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public List<Book> findAll() {
        return repository.findAll();
    }

    public Book findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + id));
    }

    public Book create(Book book) {
        if (repository.existsByIsbn(book.getIsbn())) {
            throw new DuplicateResourceException("Book already exists with ISBN: " + book.getIsbn());
        }
        return repository.save(book);
    }

    public Book update(Long id, Book updated) {
        Book existing = findById(id);
        existing.setTitle(updated.getTitle());
        existing.setAuthor(updated.getAuthor());
        existing.setPrice(updated.getPrice());
        return repository.save(existing);
    }

    public void delete(Long id) {
        findById(id);
        repository.deleteById(id);
    }
}
