package com.amigoinvisible.service;

import com.amigoinvisible.dto.RoomDtos.*;
import com.amigoinvisible.entity.*;
import com.amigoinvisible.exception.ConflictException;
import com.amigoinvisible.exception.ForbiddenException;
import com.amigoinvisible.exception.ResourceNotFoundException;
import com.amigoinvisible.repository.*;
import com.amigoinvisible.util.RoomCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final RoomExclusionRepository exclusionRepository;
    private final AssignmentRepository assignmentRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final QuestionRepository questionRepository;
    private final BudgetVoteRepository budgetVoteRepository;
    private final UserRepository userRepository;
    private final RoomCodeGenerator roomCodeGenerator;
    private final DrawService drawService;

    @Transactional
    public RoomDetailResponse createRoom(User admin, CreateRoomRequest request) {
        if (request.eventDate() != null && request.eventDate().isBefore(java.time.LocalDate.now())) {
            throw new ConflictException("La fecha del evento no puede ser anterior a hoy");
        }

        Room room = Room.builder()
                .name(request.name())
                .code(roomCodeGenerator.generateUniqueCode())
                .admin(admin)
                .suggestedBudget(request.suggestedBudget())
                .eventDate(request.eventDate())
                .place(request.place())
                .notes(request.notes())
                .status(RoomStatus.OPEN)
                .build();
        room = roomRepository.save(room);

        // El admin ya arranca como participante, y confirmado.
        RoomParticipant adminParticipant = RoomParticipant.builder()
                .room(room)
                .user(admin)
                .status(ParticipantStatus.READY)
                .build();
        participantRepository.save(adminParticipant);

        return toDetail(room, admin);
    }

    @Transactional(readOnly = true)
    public List<RoomSummaryResponse> listMyRooms(User user) {
        return participantRepository.findByUserOrderByJoinedAtDesc(user).stream()
                .map(RoomParticipant::getRoom)
                .map(room -> new RoomSummaryResponse(
                        room.getId(),
                        room.getName(),
                        room.getCode(),
                        room.getStatus(),
                        room.getEventDate(),
                        room.getAdmin().getId().equals(user.getId()),
                        (int) participantRepository.countByRoom(room)
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomPreviewResponse previewByCode(String code) {
        Room room = findByCode(code);
        return new RoomPreviewResponse(
                room.getName(),
                room.getAdmin().getUsername(),
                room.getEventDate(),
                room.getSuggestedBudget(),
                (int) participantRepository.countByRoom(room),
                room.getStatus() == RoomStatus.OPEN
        );
    }

    @Transactional
    public RoomDetailResponse joinByCode(User user, String code) {
        Room room = findByCode(code);

        if (room.getStatus() != RoomStatus.OPEN) {
            throw new ConflictException("Esta sala ya sorteo, no se pueden sumar participantes");
        }
        if (participantRepository.existsByRoomAndUser(room, user)) {
            throw new ConflictException("Ya sos parte de esta sala");
        }

        RoomParticipant participant = RoomParticipant.builder()
                .room(room)
                .user(user)
                .status(ParticipantStatus.PENDING)
                .build();
        participantRepository.save(participant);

        return toDetail(room, user);
    }

    @Transactional(readOnly = true)
    public RoomDetailResponse getDetail(User user, Long roomId) {
        Room room = findByIdAsMember(roomId, user);
        return toDetail(room, user);
    }

    @Transactional
    public RoomDetailResponse setReady(User user, Long roomId, boolean ready) {
        Room room = findByIdAsMember(roomId, user);
        RoomParticipant participant = participantRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new ResourceNotFoundException("No sos parte de esta sala"));

        participant.setStatus(ready ? ParticipantStatus.READY : ParticipantStatus.PENDING);
        participantRepository.save(participant);

        return toDetail(room, user);
    }

    @Transactional
    public void leaveRoom(User user, Long roomId) {
        Room room = findByIdAsMember(roomId, user);
        RoomParticipant participant = participantRepository.findByRoomAndUser(room, user)
                .orElseThrow(() -> new ResourceNotFoundException("No sos parte de esta sala"));

        participantRepository.delete(participant);
        budgetVoteRepository.deleteByRoomAndVoter(room, user);

        // Regla de negocio: si alguien se baja de una sala ya sellada, se descarta entera.
        if (room.getStatus() == RoomStatus.SEALED) {
            room.setStatus(RoomStatus.DISCARDED);
            roomRepository.save(room);
        }
    }

    @Transactional
    public RoomDetailResponse removeParticipant(User admin, Long roomId, Long targetUserId) {
        Room room = requireAdmin(admin, roomId);
        requireOpen(room);

        if (admin.getId().equals(targetUserId)) {
            throw new ConflictException("El admin no puede eliminarse a si mismo de esta forma. Si queres irte, usa 'Salir de la sala'.");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        RoomParticipant participant = participantRepository.findByRoomAndUser(room, target)
                .orElseThrow(() -> new ResourceNotFoundException("Ese usuario no es parte de esta sala"));

        participantRepository.delete(participant);
        budgetVoteRepository.deleteByRoomAndVoter(room, target);

        return toDetail(room, admin);
    }

    // Borra la sala entera y todo lo que depende de ella. Accion irreversible,
    // solo el admin puede hacerla, en cualquier estado de la sala.
    @Transactional
    public void deleteRoom(User admin, Long roomId) {
        Room room = requireAdmin(admin, roomId);

        assignmentRepository.deleteAll(assignmentRepository.findByRoom(room));
        exclusionRepository.deleteAll(exclusionRepository.findByRoom(room));
        wishlistItemRepository.deleteAll(wishlistItemRepository.findByRoom(room));
        questionRepository.deleteAll(questionRepository.findByRoom(room));
        budgetVoteRepository.deleteByRoom(room);
        participantRepository.deleteAll(participantRepository.findByRoomOrderByJoinedAtAsc(room));
        roomRepository.delete(room);
    }

    @Transactional
    public ExclusionResponse addExclusion(User admin, Long roomId, ExclusionRequest request) {
        Room room = requireAdmin(admin, roomId);
        requireOpen(room);

        if (request.giverId().equals(request.receiverId())) {
            throw new ConflictException("No tiene sentido excluir a alguien de si mismo");
        }

        User giver = userRepository.findById(request.giverId())
                .orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));
        User receiver = userRepository.findById(request.receiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));

        if (!participantRepository.existsByRoomAndUser(room, giver)
                || !participantRepository.existsByRoomAndUser(room, receiver)) {
            throw new ConflictException("Ambos deben ser participantes de la sala");
        }

        RoomExclusion exclusion = RoomExclusion.builder()
                .room(room)
                .giver(giver)
                .receiver(receiver)
                .build();
        exclusion = exclusionRepository.save(exclusion);

        return new ExclusionResponse(exclusion.getId(), giver.getId(), giver.getUsername(),
                receiver.getId(), receiver.getUsername());
    }

    @Transactional(readOnly = true)
    public List<ExclusionResponse> listExclusions(User user, Long roomId) {
        Room room = findByIdAsMember(roomId, user);
        return exclusionRepository.findByRoom(room).stream()
                .map(e -> new ExclusionResponse(e.getId(), e.getGiver().getId(), e.getGiver().getUsername(),
                        e.getReceiver().getId(), e.getReceiver().getUsername()))
                .toList();
    }

    @Transactional
    public void removeExclusion(User admin, Long roomId, Long exclusionId) {
        Room room = requireAdmin(admin, roomId);
        requireOpen(room);
        exclusionRepository.deleteByIdAndRoom(exclusionId, room);
    }

    @Transactional
    public RoomDetailResponse drawRoom(User admin, Long roomId) {
        Room room = requireAdmin(admin, roomId);
        requireOpen(room);

        List<RoomParticipant> participants = participantRepository.findByRoomOrderByJoinedAtAsc(room);

        if (participants.size() < 3) {
            throw new ConflictException("Se necesitan al menos 3 participantes para sortear");
        }
        boolean todosListos = participants.stream().allMatch(p -> p.getStatus() == ParticipantStatus.READY);
        if (!todosListos) {
            throw new ConflictException("Todavia hay participantes que no marcaron estar listos");
        }

        List<Long> participantIds = participants.stream().map(p -> p.getUser().getId()).toList();
        List<RoomExclusion> exclusions = exclusionRepository.findByRoom(room);

        Map<Long, Long> result = drawService.draw(participantIds, exclusions);

        Map<Long, User> usersById = participants.stream()
                .collect(java.util.stream.Collectors.toMap(p -> p.getUser().getId(), RoomParticipant::getUser));

        result.forEach((giverId, receiverId) -> {
            Assignment assignment = Assignment.builder()
                    .room(room)
                    .giver(usersById.get(giverId))
                    .receiver(usersById.get(receiverId))
                    .build();
            assignmentRepository.save(assignment);
        });

        room.setStatus(RoomStatus.SEALED);
        room.setDrawnAt(java.time.Instant.now());
        roomRepository.save(room);

        return toDetail(room, admin);
    }

    @Transactional(readOnly = true)
    public MyAssignmentResponse getMyAssignment(User user, Long roomId) {
        Room room = findByIdAsMember(roomId, user);
        if (room.getStatus() != RoomStatus.SEALED) {
            throw new ConflictException("Esta sala todavia no sorteo");
        }
        Assignment assignment = assignmentRepository.findByRoomAndGiver(room, user)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro tu asignacion"));

        return new MyAssignmentResponse(assignment.getReceiver().getId(), assignment.getReceiver().getUsername());
    }

    // --- helpers internos ---

    private Room findByCode(String code) {
        return roomRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No existe ninguna sala con ese codigo"));
    }

    private Room findByIdAsMember(Long roomId, User user) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));
        if (!participantRepository.existsByRoomAndUser(room, user)) {
            throw new ForbiddenException("No sos parte de esta sala");
        }
        return room;
    }

    private Room requireAdmin(User user, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));
        if (!room.getAdmin().getId().equals(user.getId())) {
            throw new ForbiddenException("Solo el admin de la sala puede hacer esto");
        }
        return room;
    }

    private void requireOpen(Room room) {
        if (room.getStatus() != RoomStatus.OPEN) {
            throw new ConflictException("La sala ya no esta abierta");
        }
    }

    private RoomDetailResponse toDetail(Room room, User viewer) {
        List<ParticipantResponse> participants = participantRepository.findByRoomOrderByJoinedAtAsc(room).stream()
                .map(p -> new ParticipantResponse(
                        p.getUser().getId(),
                        p.getUser().getUsername(),
                        p.getStatus(),
                        p.getUser().getId().equals(viewer.getId())
                ))
                .toList();

        return new RoomDetailResponse(
                room.getId(),
                room.getName(),
                room.getCode(),
                room.getStatus(),
                room.getSuggestedBudget(),
                room.getEventDate(),
                room.getPlace(),
                room.getNotes(),
                room.getAdmin().getId().equals(viewer.getId()),
                participants
        );
    }
}
