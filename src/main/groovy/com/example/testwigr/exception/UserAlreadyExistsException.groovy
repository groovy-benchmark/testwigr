package com.example.testwigr.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.CONFLICT)
class UserAlreadyExistsException extends RuntimeException {
    UserAlreadyExistsException(String message) {
        super(message)
    }
}
