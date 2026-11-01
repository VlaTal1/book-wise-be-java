package com.example.readerai.repository;

import com.example.readerai.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByUserId(String userId);

    Optional<Book> findFirstByUserIdAndCatalogKey(String userId, String catalogKey);
}
