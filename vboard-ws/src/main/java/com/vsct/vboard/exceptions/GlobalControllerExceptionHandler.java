/*
 * This file is part of the vboard distribution.
 * (https://github.com/voyages-sncf-technologies/vboard)
 * Copyright (c) 2017 VSCT.
 *
 * vboard is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, version 3.
 *
 * vboard is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.vsct.vboard.exceptions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import static org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST;
import static org.springframework.web.servlet.HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE;

/**
 * Created by lucas_cimon on 11/01/2018.
 */
@RestControllerAdvice
@SuppressFBWarnings("ISB_TOSTRING_APPENDING")
public class GlobalControllerExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity handleNotFound(Exception exception, WebRequest request) {
        // Next line is workaround for SpringBoot 1.5 from: https://github.com/spring-projects/spring-framework/issues/20865#issuecomment-453465378
        request.removeAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, SCOPE_REQUEST);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .headers(headers)
                .body(exception.getMessage());
    }

    @ExceptionHandler({DuplicateContentException.class})
    public ResponseEntity handleDuplicateContent(Exception exception, WebRequest request) {
        // Next line is workaround for SpringBoot 1.5 from: https://github.com/spring-projects/spring-framework/issues/20865#issuecomment-453465378
        request.removeAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, SCOPE_REQUEST);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .headers(headers)
                .body(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)  // Catch any exception. ie : NPE
    public ResponseEntity handleException(Exception exception, WebRequest request) {
        logger.error("Error 500", exception);
        // Next line is workaround for SpringBoot 1.5 from: https://github.com/spring-projects/spring-framework/issues/20865#issuecomment-453465378
        request.removeAttribute(PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE, SCOPE_REQUEST);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        String msg = exception.toString();
        if (exception.getCause() != null) {
            msg += "\nCaused by: " + exception.getCause().toString();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .headers(headers)
                .body(msg);
    }
}
