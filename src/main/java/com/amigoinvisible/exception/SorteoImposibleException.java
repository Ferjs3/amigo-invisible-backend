package com.amigoinvisible.exception;

// Se lanza cuando, dadas las exclusiones actuales, no existe ninguna
// asignacion valida de amigo invisible (ej. alguien excluyo a todos los demas).
public class SorteoImposibleException extends RuntimeException {
    public SorteoImposibleException(String message) {
        super(message);
    }
}
