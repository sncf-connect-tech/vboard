package com.vsct.vboard.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Created by lucas_cimon on 11/01/2018.
 */
@ControllerAdvice
public class GlobalControllerExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(Exception.class)  // Catch any exception. ie : NPE
    public ResponseEntity handleException(Exception exception) {
        logger.error("Error 500", exception);
        return new ResponseEntity<>(exception.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
