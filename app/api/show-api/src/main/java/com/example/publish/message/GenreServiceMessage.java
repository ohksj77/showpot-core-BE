package com.example.publish.message;

import java.util.UUID;
import lombok.Builder;
import org.example.entity.GenreSubscription;
import org.example.entity.genre.Genre;

@Builder
public record GenreServiceMessage(
    UUID id,
    String name
) {

    public static GenreServiceMessage from(Genre genre) {
        return GenreServiceMessage.builder()
            .id(genre.getId())
            .name(genre.getName())
            .build();
    }

    public static GenreServiceMessage toUnsubscribe(GenreSubscription genreSubscription) {
        return GenreServiceMessage.builder()
            .id(genreSubscription.getGenreId())
            .name("empty")
            .build();
    }
}
