package com.demo.musicservice.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.demo.musicservice.exception.MusicServiceException;
import com.demo.musicservice.model.Album;
import com.demo.musicservice.model.CoverArtApiResponse;
import com.demo.musicservice.model.Image;
import com.demo.musicservice.model.MusicBrainzResponse;
import com.demo.musicservice.model.MusicServiceResponse;
import com.demo.musicservice.model.Relation;
import com.demo.musicservice.model.ReleaseGroup;
import com.demo.musicservice.model.WikiDataEntity;
import com.demo.musicservice.model.WikipediaResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class MusicServiceImpl {

    private static final String ENWIKI = "enwiki";

    private static final String WIKIDATA = "wikidata";

    private static final Logger LOGGER = LoggerFactory.getLogger(MusicServiceImpl.class);

    @Autowired
    public RestTemplate template;

    /**
     * To get the data from musicbrainz API
     * 
     * @param uuid
     * @return MusicBrainzResponse
     */
    private MusicBrainzResponse getDataFromMusicBrainz(final String uuid) {
        String url = String.format("http://musicbrainz.org/ws/2/artist/%s?&fmt=json&inc=url-rels+release-groups", uuid);
        LOGGER.debug("Fetching information from Musicbrainz, URL:{}", url);
        return template.getForObject(url, MusicBrainzResponse.class);
    }

    /**
     * To get the data from wikidata api
     * 
     * @param relation
     * @return WikiDataEntity
     * @throws MusicServiceException
     */
    private WikiDataEntity getDataFromWikidata(final String relation) throws MusicServiceException {
        String url = String.format("https://www.wikidata.org/wiki/Special:EntityData/%s.json", relation);
        LOGGER.debug("Fetching information from wikidata, URL:{}", url);
        String entity = template.getForObject(url, String.class);
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(entity).get("entities");
            return mapper.readValue(jsonNode.get(relation).toString(), WikiDataEntity.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Something went wrong while processing the data: {}", entity);
            throw new MusicServiceException(e.getMessage());
        }
    }

    /**
     * To get the data from wikipedia using title
     * 
     * @param title
     * @return
     */
    private WikipediaResponse getDataFromWikipedia(final String title) {
        String url = String.format("https://en.wikipedia.org/api/rest_v1/page/summary/%s", title);
        LOGGER.debug("Fetching information from wikipedia, URL:{}", url);
        return template.getForObject(url, WikipediaResponse.class);
    }

    /**
     * To get the image URL information from coverartarchive
     * 
     * @param mmid - MBID
     * @return
     */
    private CoverArtApiResponse getDataFromCoverArtArchive(final String mmid) {
        String url = String.format("http://coverartarchive.org/release-group/%s", mmid);
        LOGGER.debug("Fetching information from coverartarchive, URL:{}", url);
        CoverArtApiResponse response = template.getForObject(url, CoverArtApiResponse.class);
        return response;
    }

    /**
     * To get the artist information from different external sources
     * 
     * @param mbid
     * @return MusicServiceResponse
     */
    public MusicServiceResponse getArtistInformation(final String mbid) throws MusicServiceException {
        LOGGER.info("Fetching artist information using id: {}", mbid);
        MusicBrainzResponse musicBrainzResponse = this.getDataFromMusicBrainz(mbid);
        WikiDataEntity wikidataResponse = null;
        for (Relation rel : musicBrainzResponse.getRelations()) {
            if (rel.getType().equals(WIKIDATA)) {
                String resource = rel.getUrl().getResource();
                String idStr = resource.substring(resource.lastIndexOf('/') + 1);
                wikidataResponse = this.getDataFromWikidata(idStr);
                break;
            }
        }
        String title = wikidataResponse.getSitelinks().get(ENWIKI).getTitle();
        WikipediaResponse resp = this.getDataFromWikipedia(title);
        List<Album> albums = this.getAlbums(musicBrainzResponse.getReleaseGroup());
        return new MusicServiceResponse(mbid, musicBrainzResponse.getName(), musicBrainzResponse.getGender(),
                musicBrainzResponse.getCountry(), musicBrainzResponse.getDisambiguation(), resp.getExtract_html(),
                albums);
    }

    /**
     * To get the list of albums information
     * 
     * @param releaseGroups
     * @return List<Album>
     */
    private List<Album> getAlbums(final List<ReleaseGroup> releaseGroups) {
        List<Album> albums = new ArrayList<>();
        for (ReleaseGroup group : releaseGroups) {
            List<Image> images = this.getDataFromCoverArtArchive(group.getId()).getImages();
            Image image = images.get(images.size() - 1);
            albums.add(new Album(group.getId(), group.getTitle(), image.getImage()));
        }
        return albums;
    }

}
