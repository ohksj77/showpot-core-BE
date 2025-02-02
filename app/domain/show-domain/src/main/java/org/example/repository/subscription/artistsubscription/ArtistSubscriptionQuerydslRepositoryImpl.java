package org.example.repository.subscription.artistsubscription;


import static org.example.entity.usershow.QArtistSubscription.artistSubscription;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.entity.usershow.ArtistSubscription;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ArtistSubscriptionQuerydslRepositoryImpl implements
    ArtistSubscriptionQuerydslRepository {

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<ArtistSubscription> findSubscriptionList(UUID userId) {
        return jpaQueryFactory.selectFrom(artistSubscription)
            .where(artistSubscription.userId.eq(userId)
                .and(artistSubscription.isDeleted.isFalse())
            ).fetch();
    }
}
