package com.amigoinvisible.controller;

import com.amigoinvisible.dto.QuestionDtos.AnswerQuestionRequest;
import com.amigoinvisible.dto.QuestionDtos.AskedQuestionResponse;
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

    // Lo que YO pregunte (con sus respuestas, y a quien se lo pregunte).
    @GetMapping("/api/rooms/{roomId}/questions/asked")
    public ResponseEntity<List<AskedQuestionResponse>> getAskedByMe(@PathVariable Long roomId) {
        return ResponseEntity.ok(questionService.getAskedByMe(currentUser.get(), roomId));
    }

    // Lo que ME preguntaron (para responder). Nunca se sabe quien pregunto.
    @GetMapping("/api/rooms/{roomId}/questions/received")
    public ResponseEntity<List<QuestionResponse>> getReceivedByMe(@PathVariable Long roomId) {
        return ResponseEntity.ok(questionService.getReceivedByMe(currentUser.get(), roomId));
    }

    @PostMapping("/api/rooms/{roomId}/questions/ask")
    public ResponseEntity<AskedQuestionResponse> ask(@PathVariable Long roomId,
                                                      @Valid @RequestBody AskQuestionRequest request) {
        var response = questionService.askMyAssignedFriend(currentUser.get(), roomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/api/questions/{questionId}/answer")
    public ResponseEntity<QuestionResponse> answer(@PathVariable Long questionId,
                                                     @Valid @RequestBody AnswerQuestionRequest request) {
        return ResponseEntity.ok(questionService.answer(currentUser.get(), questionId, request));
    }
}
