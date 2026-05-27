package com.edu.infnet.pb.users.infrastructure.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.edu.infnet.pb.users.domain.models.User;

@Repository
public interface AuthRepository extends JpaRepository<User, UUID> {

}
