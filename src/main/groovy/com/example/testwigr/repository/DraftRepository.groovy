package com.example.testwigr.repository

import com.example.testwigr.model.Draft
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface DraftRepository extends MongoRepository<Draft, String> {
    List<Draft> findByOwnerId(String ownerId)
    Optional<Draft> findByIdAndOwnerId(String id, String ownerId)
    long deleteByIdAndOwnerId(String id, String ownerId)
}
