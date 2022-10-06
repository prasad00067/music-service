# Music Service

REST API for providing clients with information about a specific music artist.
The information is collected from 4 different sources: MusicBrainz, Wikidata, Wikipedia and Cover Art Archive.

## TechStack

- Java
- Maven
- Spring Boot

## Build and Run

Build project:

`mvn clean install`

To run the war file directly:

`java -jar music-service-0.0.1.war`

From the root of the project run:

 `mvn spring-boot:run `

## API Endpoints

`/musify/music-artist/details/{mbid}`

mmid is a unique id of the music artist

Example:  `curl -X GET  http://localhost:8080/musify/music-artist/details/f27ec8db-af05-4f36-916e-3d57f91ecf5e`
