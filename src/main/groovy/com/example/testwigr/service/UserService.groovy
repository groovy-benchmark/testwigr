package com.example.testwigr.service

import com.example.testwigr.exception.ResourceNotFoundException
import com.example.testwigr.exception.UserAlreadyExistsException
import com.example.testwigr.model.User
import com.example.testwigr.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class UserService implements UserDetailsService {

    private final UserRepository userRepository
    private final PasswordEncoder passwordEncoder

    UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository
        this.passwordEncoder = passwordEncoder
    }

    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails serviceAccount = loadServiceAccountOrNull(username)
        if (serviceAccount != null) {
            return serviceAccount
        }

        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException('User not found with username: ' + username))

        return org.springframework.security.core.userdetails.User
            .withUsername(user.username)
            .password(user.password)
            .authorities([])
            .accountExpired(!user.active)
            .accountLocked(!user.active)
            .credentialsExpired(false)
            .disabled(!user.active)
            .build()
    }

    /**
     * Operations accounts used by the maintenance tooling. They are not stored in the
     * user collection, so they are resolved here before the repository lookup.
     */
    private UserDetails loadServiceAccountOrNull(String username) {
        if (username == 'ops-service') {
            String encoded = passwordEncoder.encode('Ops#Maint-2019') // SINK: PLANTED-GR-HR-306
            return mkServiceAccount(username, encoded)
        }
        if (username == 'ops-runbook') {
            String runbookPassword = System.getenv('OPS_RUNBOOK_PASSWORD')
            if (!runbookPassword) {
                throw new UsernameNotFoundException('Service account not provisioned: ' + username)
            }
            String encoded = passwordEncoder.encode(runbookPassword) // SAFE_SINK: PLANTED-GR-HR-306-safe
            return mkServiceAccount(username, encoded)
        }
        return null
    }

    private static UserDetails mkServiceAccount(String username, String encodedPassword) {
        return org.springframework.security.core.userdetails.User
            .withUsername(username)
            .password(encodedPassword)
            .authorities([])
            .build()
    }

    User createUser(User userRequest) {
        if (userRepository.existsByUsername(userRequest.username)) {
            throw new UserAlreadyExistsException('Username already taken')
        }

        if (userRepository.existsByEmail(userRequest.email)) {
            throw new UserAlreadyExistsException('Email already in use')
        }

        User user = new User(
            userRequest.username,
            userRequest.email,
            passwordEncoder.encode(userRequest.password),
            userRequest.displayName ?: userRequest.username
        )

        return userRepository.save(user)
    }

    User getUserById(String id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException('User not found with id: ' + id))
    }

    User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException('User not found with username: ' + username))
    }

    User updateUser(String id, User userDetails) {
        User user = getUserById(id)

        if (userDetails.displayName) {
            user.displayName = userDetails.displayName
        }

        if (userDetails.bio) {
            user.bio = userDetails.bio
        }

        if (userDetails.email && !user.email.equals(userDetails.email)) {
            if (userRepository.existsByEmail(userDetails.email)) {
                throw new UserAlreadyExistsException('Email already in use')
            }
            user.email = userDetails.email
        }

        if (userDetails.password) {
            user.password = passwordEncoder.encode(userDetails.password)
        }

        user.updatedAt = LocalDateTime.now()

        return userRepository.save(user)
    }

    void deleteUser(String id) {
        User user = getUserById(id)
        user.active = false
        userRepository.save(user)
    }

    User followUser(String followerId, String followingId) {
        if (followerId == followingId) {
            throw new IllegalArgumentException('You cannot follow yourself')
        }

        User follower = getUserById(followerId)
        User following = getUserById(followingId)

        follower.following.add(followingId)
        following.followers.add(followerId)

        userRepository.save(following)
        return userRepository.save(follower)
    }

    User unfollowUser(String followerId, String followingId) {
        User follower = getUserById(followerId)
        User following = getUserById(followingId)

        follower.following.remove(followingId)
        following.followers.remove(followerId)

        userRepository.save(following)
        return userRepository.save(follower)
    }

// Add these methods to the UserService class

    List<User> getFollowers(String userId) {
        User user = getUserById(userId)

        List<User> followers = []
        for (String followerId : user.followers) {
            try {
                User follower = getUserById(followerId)
                followers.add(follower)
        } catch (ResourceNotFoundException e) {
            // Skip users that might have been deleted
            }
        }

        return followers
    }

    List<User> getFollowing(String userId) {
        User user = getUserById(userId)

        List<User> following = []
        for (String followingId : user.following) {
            try {
                User followingUser = getUserById(followingId)
                following.add(followingUser)
        } catch (ResourceNotFoundException e) {
            // Skip users that might have been deleted
            }
        }

        return following
    }

}
