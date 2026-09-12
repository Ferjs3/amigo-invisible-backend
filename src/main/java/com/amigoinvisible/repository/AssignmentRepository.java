package com.amigoinvisible.repository;

import com.amigoinvisible.entity.Assignment;
import com.amigoinvisible.entity.Room;
import com.amigoinvisible.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByRoom(Room room);
    Optional<Assignment> findByRoomAndGiver(Room room, User giver);
}
