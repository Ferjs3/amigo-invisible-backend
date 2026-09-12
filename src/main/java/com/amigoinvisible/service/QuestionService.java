package com.amigoinvisible.service;

import com.amigoinvisible.dto.QuestionDtos.AnswerQuestionRequest;
import com.amigoinvisible.dto.QuestionDtos.AskQuestionRequest;
import com.amigoinvisible.dto.QuestionDtos.QuestionResponse;
import com.amigoinvisible.entity.Question;
import com.amigoinvisible.entity.Room;
import com.amigoinvisible.entity.User;
import com.amigoinvisible.exception.ConflictException;
import com.amigoinvisible.exception.ForbiddenException;
import com.amigoinvisible.exception.ResourceNotFoundException;
import com.amigoinvisible.repository.QuestionRepository;
import com.amigoinvisible.repository.RoomParticipantRepository;
import com.amigoinvisible.repository.RoomRepository;
import com.amigoinvisible.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<QuestionResponse> getWall(User viewer, Long roomId, Long targetUserId) {
        Room room = requireMember(viewer, roomId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // El listado publico del muro solo debe mostrar preguntas ya respondidas
        // (salvo para el propio dueno, que necesita ver las pendientes para responderlas).
        boolean isOwner = viewer.getId().equals(targetUserId);

        return questionRepository.findByRoomAndTargetUserOrderByCreatedAtDesc(room, target).stream()
                .filter(q -> isOwner || q.isAnswered())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public QuestionResponse ask(User asker, Long roomId, Long targetUserId, AskQuestionRequest request) {
        Room room = requireMember(asker, roomId);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!participantRepository.existsByRoomAndUser(room, target)) {
            throw new ConflictException("Ese usuario no es parte de esta sala");
        }
        if (asker.getId().equals(targetUserId)) {
            throw new ConflictException("No podes dejarte una pregunta a vos mismo");
        }

        Question question = Question.builder()
                .room(room)
                .targetUser(target)
                .asker(asker)
                .questionText(request.questionText())
                .build();
        question = questionRepository.save(question);

        return toResponse(question);
    }

    @Transactional
    public QuestionResponse answer(User owner, Long questionId, AnswerQuestionRequest request) {
        Question question = questionRepository.findByIdAndTargetUser(questionId, owner)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada"));

        if (!question.getTargetUser().getId().equals(owner.getId())) {
            throw new ForbiddenException("Solo el dueno del muro puede responder");
        }

        question.setAnswerText(request.answerText());
        question.setAnswered(true);
        question.setAnsweredAt(Instant.now());
        question = questionRepository.save(question);

        return toResponse(question);
    }

    private Room requireMember(User user, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Sala no encontrada"));
        if (!participantRepository.existsByRoomAndUser(room, user)) {
            throw new ForbiddenException("No sos parte de esta sala");
        }
        return room;
    }

    // El mapeo a DTO es, a proposito, el unico lugar donde se decide que se
    // expone: nunca incluye asker, es la garantia de anonimato.
    private QuestionResponse toResponse(Question q) {
        return new QuestionResponse(
                q.getId(),
                q.getQuestionText(),
                q.getAnswerText(),
                q.isAnswered(),
                q.getCreatedAt()
        );
    }
}
