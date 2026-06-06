package br.com.ibmec.researchstars.common.exception;

/** Lançada quando um recurso único já existe (HTTP 409). */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
