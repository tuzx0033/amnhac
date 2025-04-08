package com.example.demo.music.controller;

import com.example.demo.music.entity.Playlist;
import com.example.demo.music.entity.Song;
import com.example.demo.music.repository.PlaylistRepository;
import com.example.demo.music.repository.SongRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/playlists")
public class PlaylistController {

    @Autowired
    private PlaylistRepository playlistRepository;

    @Autowired
    private SongRepository songRepository;

    @GetMapping
    public String showPlaylists(Model model) {
        List<Playlist> playlists = playlistRepository.findAll();
        model.addAttribute("playlists", playlists);
        return "playlists";
    }

    @PostMapping("/create")
    public String createPlaylist(@RequestParam("name") String name, Model model) {
        Playlist playlist = new Playlist(name);
        playlistRepository.save(playlist);
        model.addAttribute("message", "Playlist created: " + name);
        return showPlaylists(model);
    }

    @PostMapping("/add-song")
    public String addSongToPlaylist(
            @RequestParam("playlistId") Long playlistId,
            @RequestParam("songId") Long songId,
            Model model) {
        System.out.println("Received playlistId: " + playlistId + ", songId: " + songId);
        try {
            if (playlistId == null || songId == null) {
                model.addAttribute("message", "Please select a playlist and song.");
                return "redirect:/api/projects/music";
            }
            Playlist playlist = playlistRepository.findById(playlistId)
                    .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));
            Song song = songRepository.findById(songId)
                    .orElseThrow(() -> new IllegalArgumentException("Song not found"));
            playlist.addSong(song);
            playlistRepository.save(playlist);
            model.addAttribute("message", "Added '" + song.getTitle() + "' to playlist '" + playlist.getName() + "'");
        } catch (Exception e) {
            model.addAttribute("message", "Error: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/api/projects/music";
    }

    @PostMapping("/remove-song")
    public String removeSongFromPlaylist(
            @RequestParam("playlistId") Long playlistId,
            @RequestParam("songId") Long songId,
            Model model) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new IllegalArgumentException("Song not found"));
        playlist.removeSong(song);
        playlistRepository.save(playlist);
        model.addAttribute("message", "Song removed from playlist: " + playlist.getName());
        return showPlaylists(model);
    }

    @PostMapping("/delete")
    public String deletePlaylist(@RequestParam("playlistId") Long playlistId, Model model) {
        try {
            Playlist playlist = playlistRepository.findById(playlistId)
                    .orElseThrow(() -> new IllegalArgumentException("Playlist not found"));
            playlistRepository.delete(playlist);
            model.addAttribute("message", "Playlist '" + playlist.getName() + "' deleted successfully.");
        } catch (Exception e) {
            model.addAttribute("message", "Error deleting playlist: " + e.getMessage());
        }
        return showPlaylists(model);
    }
}
