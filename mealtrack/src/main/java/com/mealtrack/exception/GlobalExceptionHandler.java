
package com.mealtrack.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ApiError> criarResposta(
            HttpStatus status,
            String mensagem,
            HttpServletRequest request,
            Map<String, String> erros
    ) {
        ApiError resposta = new ApiError(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI(),
                erros
        );

        return ResponseEntity.status(status).body(resposta);
    }

    // 400 - Requisição inválida
    @ExceptionHandler({
        IllegalArgumentException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> erroRequisicao(
            Exception ex, HttpServletRequest request) {

        return criarResposta(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida: verifique os dados enviados.",
                request,
                Map.of()
        );
    }

    // 400 - Erros de validação
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> erroValidacao(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> erros = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        erro -> erro.getField(),
                        erro -> erro.getDefaultMessage(),
                        (mensagem1, mensagem2) -> mensagem1
                ));

        return criarResposta(
                HttpStatus.BAD_REQUEST,
                "Existem campos inválidos na requisição.",
                request,
                erros
        );
    }

    // 404 - Registro não encontrado
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiError> naoEncontrado(
            EntityNotFoundException ex,
            HttpServletRequest request) {

        return criarResposta(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request,
                Map.of()
        );
    }

    // 404 - Rota não encontrada
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> rotaNaoEncontrada(
            NoHandlerFoundException ex,
            HttpServletRequest request) {

        return criarResposta(
                HttpStatus.NOT_FOUND,
                "Endpoint não encontrado.",
                request,
                Map.of()
        );
    }

    // 405 - Método HTTP não permitido
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> metodoNaoPermitido(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        return criarResposta(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Método HTTP não permitido neste endpoint.",
                request,
                Map.of()
        );
    }

    // 409 - Conflito de dados
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> conflitoDados(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        return criarResposta(
                HttpStatus.CONFLICT,
                "Não foi possível realizar a operação. Verifique se os dados já existem ou violam uma restrição.",
                request,
                Map.of()
        );
    }

    // 500 - Erro inesperado
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> erroInterno(
            Exception ex,
            HttpServletRequest request) {

        // Registre o detalhe no log do servidor, não na resposta pública.
        ex.printStackTrace();

        return criarResposta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno no servidor.",
                request,
                Map.of()
        );
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
public ResponseEntity<ApiError> recursoNaoEncontrado(
        RecursoNaoEncontradoException ex,
        HttpServletRequest request) {

    return criarResposta(
            HttpStatus.NOT_FOUND,
            ex.getMessage(),
            request,
            Map.of()
    );
}
}
