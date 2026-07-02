package com.edu.infnet.pb.store.repository;

import com.edu.infnet.pb.store.domain.usuario.UserReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserReferenceRepository extends JpaRepository<UserReference, Long> {

    Optional<UserReference> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);
}
