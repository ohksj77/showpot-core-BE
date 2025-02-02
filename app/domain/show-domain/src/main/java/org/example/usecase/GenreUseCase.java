package org.example.usecase;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.example.dto.genre.request.GenrePaginationDomainRequest;
import org.example.dto.genre.response.GenreNamesWithShowIdDomainParam;
import org.example.dto.genre.response.GenrePaginationDomainResponse;
import org.example.entity.BaseEntity;
import org.example.entity.artist.ArtistGenre;
import org.example.entity.genre.Genre;
import org.example.entity.show.ShowGenre;
import org.example.repository.artist.artistgenre.ArtistGenreRepository;
import org.example.repository.genre.GenreRepository;
import org.example.repository.show.showgenre.ShowGenreRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GenreUseCase {

    private final GenreRepository genreRepository;
    private final ArtistGenreRepository artistGenreRepository;
    private final ShowGenreRepository showGenreRepository;

    @Transactional
    public void save(Genre genre) {
        genreRepository.save(genre);
    }

    public List<Genre> findAllGenres() {
        return genreRepository.findAllByIsDeletedFalse();
    }

    @Transactional
    public void updateGenre(UUID id, String name) {
        Genre genre = findGenreById(id);
        genre.updateGenre(name);
    }

    @Transactional
    public void deleteGenre(UUID id) {
        Genre genre = findGenreById(id);
        genre.softDelete();

        List<ArtistGenre> artistGenres = artistGenreRepository.findAllByGenreIdAndIsDeletedFalse(
            genre.getId());
        artistGenres.forEach(BaseEntity::softDelete);

        List<ShowGenre> showGenres = showGenreRepository.findAllByGenreIdAndIsDeletedFalse(
            genre.getId());
        showGenres.forEach(BaseEntity::softDelete);
    }

    public Genre findGenreById(UUID id) {
        return genreRepository.findById(id).orElseThrow(NoSuchElementException::new);
    }

    public List<GenreNamesWithShowIdDomainParam> findGenreNamesWithShowId() {
        return showGenreRepository.findGenreNamesWithShowId();
    }

    public List<Genre> findAllGenresInIds(List<UUID> genreIds) {
        return genreRepository.findAllInId(genreIds);
    }

    public GenrePaginationDomainResponse findGenreWithCursorPagination(
        GenrePaginationDomainRequest request
    ) {
        return genreRepository.findAllWithCursorPagination(request);
    }
}
