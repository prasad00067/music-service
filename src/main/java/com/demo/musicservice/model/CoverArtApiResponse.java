package com.demo.musicservice.model;

import java.util.List;

import lombok.Data;

public @Data class CoverArtApiResponse {
    private List<Image> images;
    private String release;
}
