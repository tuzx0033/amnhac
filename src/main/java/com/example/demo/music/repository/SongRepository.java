package com.example.demo.music.repository;

import com.example.demo.music.entity.Song;
import org.springframework.data.domain.Page; // Thêm import này
import org.springframework.data.domain.Pageable; // Thêm import này
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SongRepository extends JpaRepository<Song, Long> {
    @Query("SELECT s FROM Song s WHERE LOWER(s.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(s.artist) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Song> searchByKeyword(String keyword, Pageable pageable);

    Page<Song> findAll(Pageable pageable);
}