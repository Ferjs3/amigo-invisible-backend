package com.amigoinvisible.repository;

import com.amigoinvisible.entity.Room;
import com.amigoinvisible.entity.RoomExclusion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoomExclusionRepository extends JpaRepository<RoomExclusion, Long> {
    List<RoomExclusion> findByRoom(Room room);
    void deleteByIdAndRoom(Long id, Room room);
}
