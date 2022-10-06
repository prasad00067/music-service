package com.demo.musicservice.model;

import java.util.Map;

import lombok.Data;

public @Data class Sitelinks {
    private Map<String, Wiki> sitelinks;
}
