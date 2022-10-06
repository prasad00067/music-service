package com.demo.musicservice.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class WikiDataEntity {
    private int pageid;
    private String title;
    private Map<String, Wiki> sitelinks;
}
