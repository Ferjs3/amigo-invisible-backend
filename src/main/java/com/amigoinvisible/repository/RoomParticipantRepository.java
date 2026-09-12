package com.amigoinvisible.repository;

import com.amigoinvisible.entity.Room;
import com.amigoinvisible.entity.RoomParticipant;
import com.amigoinvisible.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {
    List<RoomParticipant> findByRoomOrderByJoinedAtAsc(Room room);
    List<RoomParticipant> findByUserOrderByJoinedAtDesc(User user);
    Optional<RoomParticipant> findByRoomAndUser(Room room, User user);
    boolean existsByRoomAndUser(Room room, User user);
    long countByRoom(Room room);
}
