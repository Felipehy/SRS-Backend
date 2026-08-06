package com.siurbinfo.srs.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Handler global de excecoes da aplicacao.
 * Intercepta as excecoes lancadas pelos controllers/services e as converte
 * em respostas padronizadas no formato ProblemDetail.
 */
@RestControllerAdvice
@Log4j2
public class GlobalExceptionHandler{

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleParamValidation(HandlerMethodValidationException ex){
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Parâmetros inválidos");
        Map<String, String> errors = ex.getParameterValidationResults().stream()
                        .collect(Collectors.toMap(
                            result -> result.getMethodParameter().getParameterName(),
                            result -> result.getResolvableErrors().stream()
                                    .map(MessageSourceResolvable::getDefaultMessage)
                                    .collect(Collectors.joining("; ")),
                                (a,b) -> a + "; " + b
                        ));
        pd.setProperty("errors",errors);
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex){
        ProblemDetail pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Erro de validação");
        pd.setDetail("Um ou mais campos estão invalidos");
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> Optional.ofNullable(fe.getDefaultMessage()).orElse("Valor invalido"),
                        (a,b) -> a + "; " + b
                ));
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(ReservationIdIsNotFoundedException.class)
    public ProblemDetail handleReservationIdIsNotFoundedException(ReservationIdIsNotFoundedException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Id da reserva não encontrada");
        return pd;
    }

    @ExceptionHandler(RoomIdIsNotFoundedException.class)
    public ProblemDetail handleRoomIdisNotFoundedException(RoomIdIsNotFoundedException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Id do sala não encontrada");
        return pd;
    }

    @ExceptionHandler(StartTimeIsNotBeforeEndTimeException.class)
    public ProblemDetail handleStartTimeIsNotBeforeEndTimeException(StartTimeIsNotBeforeEndTimeException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Tempo inválido");
        return pd;
    }

    @ExceptionHandler(ReservationConflictException.class)
    public ProblemDetail handleReservationConflictException(ReservationConflictException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Reserva inválida");
        return pd;
    }

    @ExceptionHandler(InvalidPeopleCountException.class)
    public ProblemDetail handleInvalidPeopleCountException(InvalidPeopleCountException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Reserva inválida");
        return pd;
    }

    @ExceptionHandler(NameRequesterIsBlankException.class)
    public ProblemDetail handleNameRequesterIsBlankException(NameRequesterIsBlankException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Problema no nome do solicitante");
        return pd;
    }

    @ExceptionHandler(SameDepartureAndDestinationException.class)
    public ProblemDetail handleSameAddress(SameDepartureAndDestinationException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Problema no endereço");
        return pd;
    }

    @ExceptionHandler(UserIdNotFoundedException.class)
    public ProblemDetail handleUserIdNotFounded(UserIdNotFoundedException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,ex.getMessage());
        pd.setTitle("Id do usuario não encontrado");

        return pd;
    }

    @ExceptionHandler(EmptyRequestException.class)
    public ProblemDetail handleEmptyRequest(EmptyRequestException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Campos vazios");
        return pd;
    }

    @ExceptionHandler(InvalidTimeBetweenReservations.class)
    public ProblemDetail handlerInvalidTimeBetweenReservations(InvalidTimeBetweenReservations ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Problemo entre os horarios");
        return pd;
    }

    @ExceptionHandler(ReservationWasCancell.class)
    public ProblemDetail handleReservationWasCancell(ReservationWasCancell ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Status da reserva");
        return pd;
    }

    @ExceptionHandler(ReservationWasApprove.class)
    public ProblemDetail handleReservationWasApprove(ReservationWasApprove ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Status da reserva");
        return pd;
    }

    @ExceptionHandler(ReservationWasFinshed.class)
    public ProblemDetail handleReservationWasFinshed(ReservationWasFinshed ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Status da reserva");
        return pd;
    }

    @ExceptionHandler(ReservationWasReject.class)
    public ProblemDetail handleReservationWasReject(ReservationWasReject ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Status da reserva");
        return pd;
    }

    // Token do usuario invalido ou sem o escopo exigido -> problema do cliente -> 403.
    @ExceptionHandler(CognitoUnauthorizedException.class)
    public ProblemDetail handleCognitoUnauthorized(CognitoUnauthorizedException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        pd.setTitle("Acesso negado ao Cognito");
        return pd;
    }

    // Falha do Cognito como dependencia externa (indisponibilidade/rede) -> 502.
    @ExceptionHandler(CognitoQueryException.class)
    public ProblemDetail handleCognitoQuery(CognitoQueryException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
        pd.setTitle("Falha ao consultar o Cognito");
        return pd;
    }

    @ExceptionHandler(software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException.class)
    public ProblemDetail handleNotAuthorized(NotAuthorizedException ex){
        log.warn("Cognito NotAuthorized: {}", ex.awsErrorDetails().errorMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        pd.setTitle("Não autorizado no Cognito");
        return pd;
    }

    @ExceptionHandler(CognitoIdentityProviderException.class)
    public ProblemDetail handleCognitoIdentity(CognitoIdentityProviderException ex){
        // loga sempre, com o código de erro real da AWS
        String awsCode = ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorCode() : "unknown";
        log.error("Erro do Cognito [{}]: {}", awsCode, ex.getMessage(), ex);

        // AccessDenied = problema de permissão IAM do servidor, não do cliente -> 500/403
        HttpStatus status = "AccessDeniedException".equals(awsCode)
                ? HttpStatus.INTERNAL_SERVER_ERROR
                : HttpStatus.BAD_REQUEST;

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setTitle("Erro na operação com o Cognito");
        return pd;
    }

    @ExceptionHandler(AttributeIsEmptyException.class)
    public ProblemDetail handleAttributeIsEmptyException(AttributeIsEmptyException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Erro nos atributos");
        return pd;
    }

    // Registro existe no banco mas o arquivo não está no EFS -> 404.
    @ExceptionHandler(FileNotExistInEfsException.class)
    public ProblemDetail handleFileNotExistInEfs(FileNotExistInEfsException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("O arquivo não existe");
        return pd;
    }

    // Falha interna ao montar a URL do recurso -> 500 (log com stack trace).
    @ExceptionHandler(InvalidUrlException.class)
    public ProblemDetail handleInvalidUrlException(InvalidUrlException ex){
        log.error("URL malformada ao servir imagem: {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        pd.setTitle("A url da imagem foi mal formada");
        return pd;
    }

    // Usuário não possui imagem registrada no banco -> 404.
    @ExceptionHandler(PathNotExistException.class)
    public ProblemDetail handlePathNotExistException(PathNotExistException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Caminho não existe");
        return pd;
    }

    // Tentativa de escapar do diretório base (path traversal) -> 400.
    @ExceptionHandler(SecurityException.class)
    public ProblemDetail handleSecurity(SecurityException ex){
        log.warn("Path inválido detectado: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Requisição inválida");
        return pd;
    }

    @ExceptionHandler(FailedToSaveInEfs.class)
    public ProblemDetail handleFailedToSaveInEfs(FailedToSaveInEfs ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Error no efs");
        return pd;
    }

    @ExceptionHandler(InvalidFileException.class)
    public ProblemDetail handleInvalidFileException(InvalidFileException ex){
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Erro no arquivo");
        return pd;
    }

}
