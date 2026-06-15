package org.example.adminservice.domain.exception;

public class IncorrectUpdateRoleException extends RuntimeException{
    public IncorrectUpdateRoleException(String message) {
        super(message);
    }
}
