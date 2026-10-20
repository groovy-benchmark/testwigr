package com.example.testwigr.repository

import com.example.testwigr.model.Post
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface PostRepository extends MongoRepository<Post, String> {
    Page<Post> findByUserId(String userId, Pageable pageable)
    Page<Post> findByUserIdIn(Collection<String> userIds, Pageable pageable)
}
