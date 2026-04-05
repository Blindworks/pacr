package com.trainingsplan.repository;

import com.trainingsplan.entity.LoginMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginMessageRepository extends JpaRepository<LoginMessage, Long> {

    List<LoginMessage> findAllByOrderByCreatedAtDesc();

    List<LoginMessage> findByPublishedTrue();
}
