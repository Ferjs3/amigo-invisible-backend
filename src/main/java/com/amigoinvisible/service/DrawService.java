package com.amigoinvisible.service;

import com.amigoinvisible.entity.RoomExclusion;
import com.amigoinvisible.exception.SorteoImposibleException;
import org.springframework.stereotype.Service;

import java.util.*;

// Resuelve el "derangement" (nadie se regala a si mismo) respetando exclusiones,
// usando backtracking con orden aleatorio. A diferencia de un shuffle-and-retry,
// el backtracking SIEMPRE termina: o encuentra una asignacion valida, o agota
// las combinaciones posibles y avisa que el sorteo es imposible con las
// exclusiones actuales.
@Service
public class DrawService {

    public Map<Long, Long> draw(List<Long> participantIds, List<RoomExclusion> exclusions) {
        if (participantIds.size() < 3) {
            throw new IllegalArgumentException("Se necesitan al menos 3 participantes");
        }

        Set<Pair> excludedPairs = new HashSet<>();
        for (RoomExclusion ex : exclusions) {
            excludedPairs.add(new Pair(ex.getGiver().getId(), ex.getReceiver().getId()));
        }

        failFastIfImpossible(participantIds, excludedPairs);

        List<Long> givers = new ArrayList<>(participantIds);
        Collections.shuffle(givers);

        Map<Long, Long> assignment = new LinkedHashMap<>();
        Set<Long> used = new HashSet<>();

        if (!backtrack(0, givers, participantIds, used, assignment, excludedPairs)) {
            throw new SorteoImposibleException(
                    "No existe una asignacion valida con las exclusiones actuales. " +
                    "Revisa que nadie tenga excluidos a todos los demas participantes.");
        }

        return assignment;
    }

    // Chequeo rapido antes de recorrer el arbol de backtracking: si alguien
    // tiene a todos los demas excluidos (el mismo incluido no cuenta), es
    // imposible sin necesidad de explorar nada.
    private void failFastIfImpossible(List<Long> participantIds, Set<Pair> excludedPairs) {
        for (Long giver : participantIds) {
            long candidatesDisponibles = participantIds.stream()
                    .filter(receiver -> !receiver.equals(giver))
                    .filter(receiver -> !excludedPairs.contains(new Pair(giver, receiver)))
                    .count();
            if (candidatesDisponibles == 0) {
                throw new SorteoImposibleException(
                        "Un participante no tiene a quien regalarle con las exclusiones actuales");
            }
        }
    }

    private boolean backtrack(int idx, List<Long> givers, List<Long> allIds,
                               Set<Long> used, Map<Long, Long> assignment,
                               Set<Pair> excludedPairs) {
        if (idx == givers.size()) {
            return true;
        }

        Long giver = givers.get(idx);
        List<Long> candidates = new ArrayList<>(allIds);
        Collections.shuffle(candidates);

        for (Long receiver : candidates) {
            if (used.contains(receiver)) continue;
            if (receiver.equals(giver)) continue;
            if (excludedPairs.contains(new Pair(giver, receiver))) continue;

            used.add(receiver);
            assignment.put(giver, receiver);

            if (backtrack(idx + 1, givers, allIds, used, assignment, excludedPairs)) {
                return true;
            }

            used.remove(receiver);
            assignment.remove(giver);
        }
        return false;
    }

    private record Pair(Long giverId, Long receiverId) {}
}
