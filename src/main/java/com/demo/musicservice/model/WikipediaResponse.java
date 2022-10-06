package com.demo.musicservice.model;

import lombok.Data;

public @Data class WikipediaResponse {
    private String type;
    private String title; 
    private String displaytitle;
    private String extract_html;
}
