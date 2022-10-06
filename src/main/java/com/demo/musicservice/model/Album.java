package com.demo.musicservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public @Data class Album {
    private String id;
    private String title;
    private String imageUrl;
}
