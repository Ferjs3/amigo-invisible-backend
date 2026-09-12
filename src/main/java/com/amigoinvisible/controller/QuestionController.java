package com.amigoinvisible.controller;

import com.amigoinvisible.dto.QuestionDtos.AnswerQuestionRequest;
import com.amigoinvisible.dto.QuestionDtos.AskQuestionRequest;
import com.amigoinvisible.dto.QuestionDtos.QuestionResponse;
import com.amigoinvisible.security.CurrentUser;
import com.amigoinvisible.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final CurrentUser currentUser;

    @GetMapping("/api/rooms/{roomId}/questions/{targetUserId}")
    public ResponseEntity<List<QuestionResponse>> getWall(@PathVariable Long roomId, @PathVariable Long targetUserId) {
        return ResponseEntity.ok(questionService.getWall(currentUser.get(), roomId, targetUserId));
    }

    @PostMapping("/api/rooms/{roomId}/questions/{targetUserId}")
    public ResponseEntity<QuestionResponse> ask(@PathVariable Long roomId, @PathVariable Long targetUserId,
                                                 @Valid @RequestBody AskQuestionRequest request) {
        var response = questionService.ask(currentUser.get(), roomId, targetUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Ruta a nivel raiz (no anidada en /rooms) porque solo el dueno responde
    // y no necesitamos volver a validar membresia de sala aca: alcanza con
    // ser el target_user_id de la pregunta.
    @PatchMapping("/api/questions/{questionId}/answer")
    public ResponseEntity<QuestionResponse> answer(@PathVariable Long questionId,
                                                     @Valid @RequestBody AnswerQuestionRequest request) {
        return ResponseEntity.ok(questionService.answer(currentUser.get(), questionId, request));
    }
}
