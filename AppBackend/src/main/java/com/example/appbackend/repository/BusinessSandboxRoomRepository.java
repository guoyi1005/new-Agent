package com.example.appbackend.repository;

import com.example.appbackend.entity.BusinessSandboxRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BusinessSandboxRoomRepository extends JpaRepository<BusinessSandboxRoom, Long> {
    List<BusinessSandboxRoom> findAllByOrderByUpdatedAtDesc();
    boolean existsByRoomCode(String roomCode);
}