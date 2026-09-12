package com.amigoinvisible.controller;

import com.amigoinvisible.dto.BudgetVoteDtos.BudgetVoteStatusResponse;
import com.amigoinvisible.dto.BudgetVoteDtos.CastVoteRequest;
import com.amigoinvisible.dto.BudgetVoteDtos.ResolveTieRequest;
import com.amigoinvisible.security.CurrentUser;
import com.amigoinvisible.service.BudgetVoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms/{roomId}/budget-vote")
@RequiredArgsConstructor
public class BudgetVoteController {

    private final BudgetVoteService budgetVoteService;
    private final CurrentUser currentUser;

    @GetMapping
    public ResponseEntity<BudgetVoteStatusResponse> status(@PathVariable Long roomId) {
        return ResponseEntity.ok(budgetVoteService.getStatus(currentUser.get(), roomId));
    }

    @PostMapping("/open")
    public ResponseEntity<BudgetVoteStatusResponse> open(@PathVariable Long roomId) {
        return ResponseEntity.ok(budgetVoteService.openVote(currentUser.get(), roomId));
    }

    @PostMapping
    public ResponseEntity<BudgetVoteStatusResponse> castVote(@PathVariable Long roomId,
                                                              @Valid @RequestBody CastVoteRequest request) {
        return ResponseEntity.ok(budgetVoteService.castVote(currentUser.get(), roomId, request.amount()));
    }

    @PostMapping("/resolve-tie")
    public ResponseEntity<BudgetVoteStatusResponse> resolveTie(@PathVariable Long roomId,
                                                                @Valid @RequestBody ResolveTieRequest request) {
        return ResponseEntity.ok(budgetVoteService.resolveTie(currentUser.get(), roomId, request.amount()));
    }
}
