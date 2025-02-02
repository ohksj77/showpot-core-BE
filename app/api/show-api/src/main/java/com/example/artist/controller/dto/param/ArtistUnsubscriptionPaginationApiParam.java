package com.example.artist.controller.dto.param;

import com.example.artist.service.dto.param.ArtistUnsubscriptionPaginationServiceParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public record ArtistUnsubscriptionPaginationApiParam(
    @Schema(description = "아티스트 ID")
    UUID id,
    @Schema(description = "아티스트의 스포티파이 ID")
    String spotifyId,
    @Schema(description = "아티스트 이미지 URL")
    String imageURL,
    @Schema(description = "아티스트 이름")
    String name
) {

    public static ArtistUnsubscriptionPaginationApiParam from(
        ArtistUnsubscriptionPaginationServiceParam param
    ) {
        return new ArtistUnsubscriptionPaginationApiParam(
            param.id(),
            param.spotifyId(),
            param.image(),
            param.name()
        );
    }
}
