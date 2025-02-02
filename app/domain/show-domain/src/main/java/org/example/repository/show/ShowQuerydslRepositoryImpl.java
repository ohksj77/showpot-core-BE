package org.example.repository.show;

import static com.querydsl.core.group.GroupBy.groupBy;
import static com.querydsl.core.group.GroupBy.list;
import static com.querydsl.core.group.GroupBy.set;
import static org.example.entity.artist.QArtist.artist;
import static org.example.entity.genre.QGenre.genre;
import static org.example.entity.show.QShow.show;
import static org.example.entity.show.QShowArtist.showArtist;
import static org.example.entity.show.QShowGenre.showGenre;
import static org.example.entity.show.QShowTicketingTime.showTicketingTime;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.dto.artist.response.ArtistDomainResponse;
import org.example.dto.artist.response.ArtistNameDomainResponse;
import org.example.dto.genre.response.GenreDomainResponse;
import org.example.dto.genre.response.GenreNameDomainResponse;
import org.example.dto.show.param.ShowWithTicketingTimesDomainParam;
import org.example.dto.show.request.ShowPaginationDomainRequest;
import org.example.dto.show.response.ShowDetailDomainResponse;
import org.example.dto.show.response.ShowDomainResponse;
import org.example.dto.show.response.ShowInfoDomainResponse;
import org.example.dto.show.response.ShowTicketingDomainResponse;
import org.example.dto.show.response.ShowTicketingPaginationDomainResponse;
import org.example.dto.show.response.ShowTicketingTimeDomainResponse;
import org.example.entity.show.Show;
import org.example.util.SliceUtil;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ShowQuerydslRepositoryImpl implements ShowQuerydslRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<ShowDetailDomainResponse> findShowDetailById(UUID id) {
        return Optional.ofNullable(
            createShowJoinArtistAndGenreQuery()
                .where(show.id.eq(id))
                .transform(
                    groupBy(show.id).as(getShowDetailConstructor())
                )
                .get(id)
        );
    }

    @Override
    public List<ShowWithTicketingTimesDomainParam> findShowDetailWithTicketingTimes() {
        return jpaQueryFactory.selectFrom(show)
            .join(showTicketingTime).on(isShowTicketingEqualShowAndIsDeletedFalse())
            .where(show.isDeleted.isFalse())
            .transform(
                groupBy(show.id).list(
                    Projections.constructor(
                        ShowWithTicketingTimesDomainParam.class,
                        Projections.constructor(
                            ShowDomainResponse.class,
                            show.id,
                            show.title,
                            show.content,
                            show.startDate,
                            show.endDate,
                            show.location,
                            show.image,
                            show.lastTicketingAt,
                            show.viewCount,
                            show.seatPrices,
                            show.ticketingSites
                        ),
                        list(
                            Projections.constructor(
                                ShowTicketingTimeDomainResponse.class,
                                showTicketingTime.ticketingType,
                                showTicketingTime.ticketingAt
                            )
                        )
                    )
                )
            );
    }

    @Override
    public Optional<ShowInfoDomainResponse> findShowInfoById(UUID id) {
        return Optional.ofNullable(
            createShowJoinArtistAndGenreQuery()
                .where(show.id.eq(id))
                .transform(
                    groupBy(show.id).as(getShowInfoConstructor())
                )
                .get(id)
        );
    }

    @Override
    public ShowTicketingPaginationDomainResponse findShows(ShowPaginationDomainRequest request) {
        List<ShowTicketingDomainResponse> result = jpaQueryFactory
            .select(
                Projections.constructor(
                    ShowTicketingDomainResponse.class,
                    show.id,
                    show.title,
                    show.endDate,
                    showTicketingTime.ticketingAt,
                    show.location,
                    show.image
                )
            )
            .from(show)
            .join(showTicketingTime).on(showTicketingTime.show.id.eq(show.id))
            .where(getShowAlertsInCursorPagination(request))
            .orderBy(getOrderSpecifier(request))
            .limit(request.size() + 1)
            .fetch();

        Slice<ShowTicketingDomainResponse> slice = SliceUtil.makeSlice(request.size(), result);

        return ShowTicketingPaginationDomainResponse.builder()
            .data(slice.getContent())
            .hasNext(slice.hasNext())
            .build();
    }

    @Override
    public long findTerminatedTicketingShowsCount(List<UUID> showIds, LocalDateTime now) {
        Long result = jpaQueryFactory
            .select(show.id.countDistinct())
            .from(show)
            .where(
                show.isDeleted.isFalse()
                    .and(show.id.in(showIds))
                    .and(show.lastTicketingAt.before(now))
            )
            .fetchFirst();

        return result == null ? 0 : result;
    }

    private Predicate getShowAlertsInCursorPagination(ShowPaginationDomainRequest request) {
        BooleanExpression wherePredicate = getDefaultPredicateExpression();

        if (request.onlyOpenSchedule()) {
            return wherePredicate.and(showTicketingTime.ticketingAt.after(request.now()));
        } else {
            wherePredicate.and(show.endDate.after(LocalDate.now()));
        }

        if (request.cursorId() == null) {
            return wherePredicate;
        }

        switch (request.sort()) {
            case RECENT -> {
                return wherePredicate.and(createRecentPredicate(request.cursorId()));
            }
            default -> {
                return wherePredicate.and(createPopularPredicate(request.cursorId()));
            }
        }
    }

    private BooleanExpression getDefaultPredicateExpression() {
        return show.isDeleted.isFalse().and(showTicketingTime.isDeleted.isFalse());
    }

    private BooleanExpression createRecentPredicate(UUID cursorId) {
        Tuple cursor = jpaQueryFactory
            .select(showTicketingTime.id, showTicketingTime.ticketingAt)
            .from(showTicketingTime)
            .where(showTicketingTime.show.id.eq(cursorId))
            .fetchFirst();

        LocalDateTime cursorValue = cursor.get(showTicketingTime.ticketingAt);
        UUID cursorIdValue = cursor.get(showTicketingTime.id);

        return showTicketingTime.ticketingAt.gt(cursorValue)
            .or(showTicketingTime.ticketingAt.eq(cursorValue)
                .and(showTicketingTime.id.gt(cursorIdValue)));
    }

    private BooleanExpression createPopularPredicate(UUID cursorId) {
        Tuple cursor = jpaQueryFactory
            .select(show.id, show.viewCount)
            .from(show)
            .where(show.id.eq(cursorId))
            .fetchFirst();

        Integer cursorValue = cursor.get(show.viewCount);
        UUID cursorIdValue = cursor.get(show.id);

        return show.viewCount.lt(cursorValue)
            .or(show.viewCount.eq(cursorValue)
                .and(show.id.gt(cursorIdValue)));
    }

    private OrderSpecifier<?>[] getOrderSpecifier(ShowPaginationDomainRequest request) {
        return switch (request.sort()) {
            case RECENT -> new OrderSpecifier<?>[]{
                showTicketingTime.ticketingAt.asc(),
                showTicketingTime.id.asc()
            };
            default -> new OrderSpecifier<?>[]{
                show.viewCount.desc(),
                show.id.asc()
            };
        };
    }

    private ConstructorExpression<ShowDetailDomainResponse> getShowDetailConstructor() {
        return Projections.constructor(
            ShowDetailDomainResponse.class,
            Projections.constructor(
                ShowDomainResponse.class,
                show.id,
                show.title,
                show.content,
                show.startDate,
                show.endDate,
                show.location,
                show.image,
                show.lastTicketingAt,
                show.viewCount,
                show.seatPrices,
                show.ticketingSites
            ),
            set(
                Projections.constructor(
                    ArtistDomainResponse.class,
                    artist.id,
                    artist.name,
                    artist.image
                )
            ),
            set(
                Projections.constructor(
                    GenreDomainResponse.class,
                    genre.id,
                    genre.name
                )
            ),
            set(
                Projections.constructor(
                    ShowTicketingTimeDomainResponse.class,
                    showTicketingTime.ticketingType,
                    showTicketingTime.ticketingAt
                )
            )
        );
    }

    private ConstructorExpression<ShowInfoDomainResponse> getShowInfoConstructor() {
        return Projections.constructor(
            ShowInfoDomainResponse.class,
            Projections.constructor(
                ShowDomainResponse.class,
                show.id,
                show.title,
                show.content,
                show.startDate,
                show.endDate,
                show.location,
                show.image,
                show.lastTicketingAt,
                show.viewCount,
                show.seatPrices,
                show.ticketingSites
            ),
            set(
                Projections.constructor(
                    ArtistNameDomainResponse.class,
                    artist.id,
                    artist.name
                )
            ),
            set(
                Projections.constructor(
                    GenreNameDomainResponse.class,
                    genre.id,
                    genre.name
                )
            ),
            set(
                Projections.constructor(
                    ShowTicketingTimeDomainResponse.class,
                    showTicketingTime.ticketingType,
                    showTicketingTime.ticketingAt
                )
            )
        );
    }

    private JPAQuery<Show> createShowJoinArtistAndGenreQuery() {
        return jpaQueryFactory
            .selectFrom(show)
            .join(showArtist).on(isShowArtistEqualShowIdAndIsDeletedFalse())
            .join(artist).on(isArtistIdEqualShowArtistAndIsDeletedFalse())
            .join(showGenre).on(isShowGenreEqualShowIdAndIsDeletedFalse())
            .join(genre).on(isGenreIdEqualShowGenreAndIsDeletedFalse())
            .join(showTicketingTime).on(isShowTicketingEqualShowAndIsDeletedFalse())
            .where(show.isDeleted.isFalse());
    }

    private BooleanExpression isShowArtistEqualShowIdAndIsDeletedFalse() {
        return showArtist.showId.eq(show.id).and(showArtist.isDeleted.isFalse());
    }

    private BooleanExpression isArtistIdEqualShowArtistAndIsDeletedFalse() {
        return artist.id.eq(showArtist.artistId).and(artist.isDeleted.isFalse());
    }

    private BooleanExpression isShowGenreEqualShowIdAndIsDeletedFalse() {
        return showGenre.showId.eq(show.id).and(showGenre.isDeleted.isFalse());
    }

    private BooleanExpression isGenreIdEqualShowGenreAndIsDeletedFalse() {
        return genre.id.eq(showGenre.genreId).and(genre.isDeleted.isFalse());
    }

    private BooleanExpression isShowTicketingEqualShowAndIsDeletedFalse() {
        return showTicketingTime.show.id.eq(show.id).and(showTicketingTime.isDeleted.isFalse());
    }
}
