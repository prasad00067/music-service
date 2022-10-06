package com.demo.musicservice.service;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class MusicServiceImpl {

    private static final String ENWIKI = "enwiki";

    private static final String WIKIDATA = "wikidata";

    private static final Logger LOGGER = LoggerFactory.getLogger(MusicServiceImpl.class);

    @Autowired
    public WebClient webClient;

    /**
     * To get the data from musicbrainz API
     * 
     * @param uuid
     * @return MusicBrainzResponse
     */
    private MusicBrainzResponse getDataFromMusicBrainz(final String uuid) {
        LOGGER.debug("Fetching information from Musicbrainz", uuid);
        return webClient.get()
                .uri("http://musicbrainz.org/ws/2/artist/{id}?&fmt=json&inc=url-rels+release-groups", uuid)
                .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(MusicBrainzResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)).filter(ex -> {
                    if (ex instanceof WebClientResponseException) {
                        WebClientResponseException se = (WebClientResponseException) ex;
                        return (se.getRawStatusCode() == 400) ? true : false;
                    }
                    return false;
                })).block();
    }

    /**
     * To get the data from wikidata api
     * 
     * @param relation
     * @return WikiDataEntity
     * @throws MusicServiceException
     */
    private WikiDataEntity getDataFromWikidata(final String relation) throws MusicServiceException {
        LOGGER.debug("Fetching information from wikidata");
        String wikdata = webClient.get().uri("https://www.wikidata.org/wiki/Special:EntityData/{id}.json", relation)
                .retrieve().bodyToMono(String.class).block();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(wikdata).get("entities");
            return mapper.readValue(jsonNode.get(relation).toString(), WikiDataEntity.class);
        } catch (JsonProcessingException e) {
            LOGGER.error("Something went wrong while processing the data: {}", wikdata);
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
        LOGGER.debug("Fetching information from wikipedia: (%s)", title);
        WikipediaResponse wikipediaResponse = webClient.get()
                .uri("https://en.wikipedia.org/api/rest_v1/page/summary/{title}", title)
                .accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(WikipediaResponse.class).block();
        return wikipediaResponse;
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
        List<Album> albums = Flux.fromIterable(releaseGroups).flatMap(this::getAlbum).collectList().block();
        return albums;
    }

    public Mono<Album> getAlbum(ReleaseGroup group) {
        Mono<Album> mono = webClient.get().uri("http://coverartarchive.org/release-group/{id}", group.getId())
                .accept(MediaType.APPLICATION_JSON).acceptCharset(Charset.forName("UTF-8")).retrieve()
                .bodyToMono(CoverArtApiResponse.class).map(resp -> {
                    Image image = resp.getImages().get(resp.getImages().size() - 1);
                    return new Album(group.getId(), group.getTitle(), image.getImage());
                }).cast(Album.class);
        return mono;
    }
}
