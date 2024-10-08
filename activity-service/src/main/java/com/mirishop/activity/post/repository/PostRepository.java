package com.mirishop.activity.post.repository;

import com.mirishop.activity.post.entity.Post;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByMemberNumberAndIsDeletedFalse(Long memberNumber, Pageable pageable);

    @Query("SELECT p.memberNumber FROM Post p WHERE p.memberNumber = :memberNumber")
    List<Long> findByMemberNumberAndIsDeletedFalse(@Param("memberNumber") Long memberNumber);

    Optional<Post> findByPostIdAndIsDeletedFalse(Long postId);
}