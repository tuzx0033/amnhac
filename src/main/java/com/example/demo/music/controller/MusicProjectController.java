package com.example.demo.music.controller;

import com.example.demo.music.entity.Playlist;
import com.example.demo.music.entity.Song;
import com.example.demo.music.repository.PlaylistRepository;
import com.example.demo.music.repository.SongRepository;
import com.example.demo.music.service.SupabaseStorageService;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.HttpClientErrorException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List; // Thêm dòng này
@Controller
@RequestMapping("/api/projects")
public class MusicProjectController {

    @Autowired
    private SupabaseStorageService supabaseStorageService;

    @Autowired
    private SongRepository songRepository;

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.bucket}")
    private String supabaseBucket;

    @Autowired
    private PlaylistRepository playlistRepository;

    @GetMapping("/music")
    public String showMusicPage(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Song> songPage = songRepository.findAll(pageable);
        List<Playlist> playlists = playlistRepository.findAll(); // Lấy danh sách playlist
        model.addAttribute("files", songPage.getContent());
        model.addAttribute("currentPage", songPage.getNumber());
        model.addAttribute("totalPages", songPage.getTotalPages());
        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseBucket", supabaseBucket);
        model.addAttribute("playlists", playlists); // Truyền vào giao diện
        return "music";
    }

    @PostMapping("/upload")
    public String uploadMusicProject(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "artist", required = false) String artist,
            Model model) throws IOException {
        File tempFile = File.createTempFile("temp", ".mp3");
        try {
            Files.write(tempFile.toPath(), file.getBytes());
            Integer duration;
            try {
                AudioFile audioFile = AudioFileIO.read(tempFile);
                duration = audioFile.getAudioHeader().getTrackLength();
            } catch (Exception e) {
                duration = 180;
                System.out.println("Error reading audio duration: " + e.getMessage());
            }
            String fileUrl = supabaseStorageService.uploadFile(file);
            System.out.println("Uploaded file URL: " + fileUrl);
            Song song = new Song(title, artist, duration, fileUrl);
            songRepository.save(song);
            model.addAttribute("message", "File uploaded successfully: " + fileUrl);
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
        Pageable pageable = PageRequest.of(0, 5); // Trang đầu sau khi upload
        Page<Song> songPage = songRepository.findAll(pageable);
        model.addAttribute("files", songPage.getContent());
        model.addAttribute("currentPage", songPage.getNumber());
        model.addAttribute("totalPages", songPage.getTotalPages());
        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseBucket", supabaseBucket);
        return "music";
    }

    @PostMapping("/delete")
    public String deleteFile(
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {
        System.out.println("Deleting file: " + fileName);
        try {
            supabaseStorageService.deleteFile(fileName);
            Song songToDelete = songRepository.findAll().stream()
                    .filter(song -> song.getFileUrl().endsWith(fileName))
                    .findFirst()
                    .orElse(null);
            if (songToDelete != null) {
                songRepository.deleteById(songToDelete.getId());
                model.addAttribute("message", "File deleted successfully: " + fileName);
            } else {
                model.addAttribute("message", "File not found in database: " + fileName);
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                model.addAttribute("message", "File not found in storage: " + fileName);
                Song songToDelete = songRepository.findAll().stream()
                        .filter(song -> song.getFileUrl().endsWith(fileName))
                        .findFirst()
                        .orElse(null);
                if (songToDelete != null) {
                    songRepository.deleteById(songToDelete.getId());
                }
            } else {
                throw e;
            }
        }
        Pageable pageable = PageRequest.of(page, 5);
        Page<Song> songPage = songRepository.findAll(pageable);
        model.addAttribute("files", songPage.getContent());
        model.addAttribute("currentPage", songPage.getNumber());
        model.addAttribute("totalPages", songPage.getTotalPages());
        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseBucket", supabaseBucket);
        return "music";
    }

    @PostMapping("/search")
    public String searchSongs(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            Model model) {
        Pageable pageable = PageRequest.of(page, 5);
        Page<Song> songPage;
        if (keyword == null || keyword.trim().isEmpty()) {
            songPage = songRepository.findAll(pageable);
        } else {
            songPage = songRepository.searchByKeyword(keyword, pageable);
        }
        model.addAttribute("files", songPage.getContent());
        model.addAttribute("currentPage", songPage.getNumber());
        model.addAttribute("totalPages", songPage.getTotalPages());
        model.addAttribute("keyword", keyword); // Giữ từ khóa để hiển thị lại
        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseBucket", supabaseBucket);
        return "music";
    }

// Trong MusicProjectController.java
    @PostMapping("/edit")
    public String editSong(
            @RequestParam("id") Long id,
            @RequestParam("title") String title,
            @RequestParam(value = "artist", required = false) String artist,
            Model model) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Song not found"));
        song.setTitle(title);
        song.setArtist(artist);
        songRepository.save(song);
        model.addAttribute("message", "Song updated successfully: " + title);

        // Tải lại trang hiện tại
        Pageable pageable = PageRequest.of(0, 5); // Có thể giữ trang hiện tại nếu cần
        Page<Song> songPage = songRepository.findAll(pageable);
        model.addAttribute("files", songPage.getContent());
        model.addAttribute("currentPage", songPage.getNumber());
        model.addAttribute("totalPages", songPage.getTotalPages());
        model.addAttribute("supabaseUrl", supabaseUrl);
        model.addAttribute("supabaseBucket", supabaseBucket);
        return "music";
    }
}
