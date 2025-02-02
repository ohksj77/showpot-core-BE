package org.example.dto.show.response;

import java.util.Set;
import org.example.dto.artist.response.ArtistNameDomainResponse;
import org.example.dto.genre.response.GenreNameDomainResponse;

public record ShowInfoDomainResponse(
    ShowDomainResponse show,
    Set<ArtistNameDomainResponse> artistNameResponses,
    Set<GenreNameDomainResponse> genreNameResponses,
    Set<ShowTicketingTimeDomainResponse> ticketingTimes
) {

}
