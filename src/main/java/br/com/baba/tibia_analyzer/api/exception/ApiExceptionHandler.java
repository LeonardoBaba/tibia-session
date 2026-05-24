package br.com.baba.tibia_analyzer.api.exception;

import br.com.baba.tibia_analyzer.core.exception.ConverterException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice(basePackages = "br.com.baba.tibia_analyzer.api")
public class ApiExceptionHandler {

    @ExceptionHandler({ConverterException.class, NumberFormatException.class, IllegalArgumentException.class})
    public ResponseEntity<Map<String, Object>> handleBadInput(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(HttpStatus.BAD_REQUEST, ex.getMessage()));
    }

    private Map<String, Object> error(HttpStatus status, String message) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message == null ? "" : message
        );
    }
}
