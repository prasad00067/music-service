package com.demo.musicservice.model;

import java.util.List;

import lombok.Data;

public @Data class Wiki {
    private String site;
    private String title;
    private List<String> badges;
    private String url;
}
