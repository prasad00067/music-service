package com.demo.musicservice.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

public @Data class MusicBrainzResponse {
    private String name;
    private String disambiguation;
    private String gender;
    private String country;
    private List<Relation> relations;
    @JsonProperty("release-groups")
    private List<ReleaseGroup> releaseGroup;

}
