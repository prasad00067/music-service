package com.demo.musicservice.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

import com.demo.musicservice.exception.MusicServiceException;
import com.demo.musicservice.model.MusicServiceResponse;
import com.demo.musicservice.service.MusicServiceImpl;

@RestController
public class MusicServiceController {
    private static final Logger LOGGER = LoggerFactory.getLogger(MusicServiceController.class);

    @Autowired
    private MusicServiceImpl musicService;

    @GetMapping("/musify/music-artist/details/{mbid}")
    public MusicServiceResponse getMusicArtistInfo(@PathVariable("mbid") final String mbid) {
        LOGGER.info("Artist mbid: {}", mbid);
        try {
            MusicServiceResponse response = musicService.getArtistInformation(mbid);
            LOGGER.info("Successfully fetched artist information. mbid:{}, Name: {}", mbid, response.getName());
            return response;
        } catch (MusicServiceException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), e.getResponseBodyAsString());
        }
    }
}
