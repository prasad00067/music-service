package com.demo.musicservice.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public @Data class MusicServiceResponse {
    private String mbid;
    private String name;
    private String gender;
    private String country;
    private String disambiguation;
    private String description;
    private List<Album> albums;
}