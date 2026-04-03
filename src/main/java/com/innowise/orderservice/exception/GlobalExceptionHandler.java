package com.innowise.orderservice.exception;


import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorDetails> handleFeignException(FeignException e) {

        String errorBody = e.contentUTF8();
        String message;

        try {
            ObjectMapper mapper = new ObjectMapper();

            Map<String, Object> map = mapper.readValue(errorBody, Map.class);

            if (map.containsKey("message")) {
                message = map.get("message").toString();
            } else {
                message = errorBody;
            }
        } catch (Exception parseException) {
            message = e.getMessage();
        }

        HttpStatus status = HttpStatus.resolve(e.status());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        ErrorDetails exception = ErrorDetails.builder()
                .message(message)
                .errorName(status.getReasonPhrase())
                .httpStatus(status.value())
                .timestamp(LocalDateTime.now())
                .build();


        return new ResponseEntity<>(exception, status);
    }

//    @ExceptionHandler(UserServiceException.class)
//    public ResponseEntity<ErrorDetails> handleNotFound(UserServiceException e) {
//        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
//        ErrorDetails exception = ErrorDetails.builder()
//                .message(e.getMessage())
//                .errorName(status.getReasonPhrase())
//                .httpStatus(status.value())
//                .timestamp(LocalDateTime.now())
//                .build();
//        return  new ResponseEntity<>(exception, status);
//    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleNotFound(EntityNotFoundException e) {
        HttpStatus notFound = HttpStatus.NOT_FOUND;
        ErrorDetails exception = ErrorDetails.builder()
                .message(e.getMessage())
                .errorName(notFound.getReasonPhrase())
                .httpStatus(notFound.value())
                .timestamp(LocalDateTime.now())
                .build();
        return  new ResponseEntity<>(exception, notFound);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorDetails> handleConflict(BusinessException e) {
        HttpStatus httpError = e.getStatus();
        ErrorDetails exception = ErrorDetails.builder()
                .message(e.getMessage())
                .errorName(httpError.getReasonPhrase())
                .httpStatus(httpError.value())
                .timestamp(LocalDateTime.now())
                .build();
        return  new ResponseEntity<>(exception, httpError);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleValidation(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorDetails error = ErrorDetails.builder()
                .message(errorMessage)
                .errorName("Validation Error")
                .httpStatus(status.value())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, status);
    }


}
