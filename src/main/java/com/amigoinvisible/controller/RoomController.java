package com.amigoinvisible.controller;

import com.amigoinvisible.dto.RoomDtos.*;
import com.amigoinvisible.security.CurrentUser;
import com.amigoinvisible.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final CurrentUser currentUser;

    @PostMapping
    public ResponseEntity<RoomDetailResponse> create(@Valid @RequestBody CreateRoomRequest request) {
        var response = roomService.createRoom(currentUser.get(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RoomSummaryResponse>> myRooms() {
        return ResponseEntity.ok(roomService.listMyRooms(currentUser.get()));
    }

    // Publico: para mostrar una vista previa antes de unirse (sin necesitar ser miembro).
    @GetMapping("/{code}/preview")
    public ResponseEntity<RoomPreviewResponse> preview(@PathVariable String code) {
        return ResponseEntity.ok(roomService.previewByCode(code));
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<RoomDetailResponse> join(@PathVariable String code) {
        return ResponseEntity.ok(roomService.joinByCode(currentUser.get(), code));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomDetailResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getDetail(currentUser.get(), id));
    }

    @PatchMapping("/{id}/participants/me")
    public ResponseEntity<RoomDetailResponse> setReady(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean ready = Boolean.TRUE.equals(body.get("ready"));
        return ResponseEntity.ok(roomService.setReady(currentUser.get(), id, ready));
    }

    @DeleteMapping("/{id}/participants/me")
    public ResponseEntity<Void> leave(@PathVariable Long id) {
        roomService.leaveRoom(currentUser.get(), id);
        return ResponseEntity.noContent().build();
    }

    // Eliminar a otro participante (solo admin, solo mientras la sala esta abierta).
    @DeleteMapping("/{id}/participants/{userId}")
    public ResponseEntity<RoomDetailResponse> removeParticipant(@PathVariable Long id, @PathVariable Long userId) {
        return ResponseEntity.ok(roomService.removeParticipant(currentUser.get(), id, userId));
    }

    @GetMapping("/{id}/exclusions")
    public ResponseEntity<List<ExclusionResponse>> listExclusions(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.listExclusions(currentUser.get(), id));
    }

    @PostMapping("/{id}/exclusions")
    public ResponseEntity<ExclusionResponse> addExclusion(@PathVariable Long id, @Valid @RequestBody ExclusionRequest request) {
        var response = roomService.addExclusion(currentUser.get(), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/exclusions/{exclusionId}")
    public ResponseEntity<Void> removeExclusion(@PathVariable Long id, @PathVariable Long exclusionId) {
        roomService.removeExclusion(currentUser.get(), id, exclusionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/draw")
    public ResponseEntity<RoomDetailResponse> draw(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.drawRoom(currentUser.get(), id));
    }

    @GetMapping("/{id}/my-assignment")
    public ResponseEntity<MyAssignmentResponse> myAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getMyAssignment(currentUser.get(), id));
    }
}
