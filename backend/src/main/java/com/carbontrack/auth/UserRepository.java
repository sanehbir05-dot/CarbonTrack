package com.carbontrack.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u join fetch u.company where u.email = :email")
    Optional<User> findByEmail(String email);

    @Query("select u from User u join fetch u.company where u.id = :id")
    Optional<User> findWithCompanyById(Long id);

    boolean existsByEmail(String email);
}
