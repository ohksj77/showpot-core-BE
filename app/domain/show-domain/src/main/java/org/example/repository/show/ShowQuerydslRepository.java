package org.example.repository.show;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.example.dto.show.param.ShowWithTicketingTimesDomainParam;
import org.example.dto.show.request.ShowPaginationDomainRequest;
import org.example.dto.show.response.ShowDetailDomainResponse;
import org.example.dto.show.response.ShowInfoDomainResponse;
import org.example.dto.show.response.ShowTicketingPaginationDomainResponse;

public interface ShowQuerydslRepository {

    Optional<ShowDetailDomainResponse> findShowDetailById(UUID id);

    List<ShowWithTicketingTimesDomainParam> findShowDetailWithTicketingTimes();

    ShowTicketingPaginationDomainResponse findShows(ShowPaginationDomainRequest request);

    Optional<ShowInfoDomainResponse> findShowInfoById(UUID id);

    long findTerminatedTicketingShowsCount(List<UUID> showIds, LocalDateTime now);
}
