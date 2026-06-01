package com.edu.infnet.pb.users.infrastructure.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.edu.infnet.pb.users.domain.models.User;

public interface ProfileRepository extends JpaRepository<User, UUID> {

}
