package kz.iitu.csse.group34.repositories;

import kz.iitu.csse.group34.entities.Comments;
import kz.iitu.csse.group34.entities.NewsPost;
import kz.iitu.csse.group34.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsPostRepository extends JpaRepository<NewsPost, Long> {
    Optional<NewsPost> findById(Long id);
    List<NewsPost> findAllByAuthor(Users u);
}
