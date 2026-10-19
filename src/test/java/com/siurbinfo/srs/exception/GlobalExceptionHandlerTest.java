package com.siurbinfo.srs.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static final String MSG = "mensagem de teste";

    static Stream<Arguments> handlers() {
        GlobalExceptionHandler h = new GlobalExceptionHandler();
        return Stream.of(
                Arguments.of("ReservationIdIsNotFounded",
                        (Function<String, ProblemDetail>) m -> h.handleReservationIdIsNotFoundedException(new ReservationIdIsNotFoundedException(m)),
                        HttpStatus.NOT_FOUND, "Id da reserva não encontrada"),
                Arguments.of("RoomIdIsNotFounded",
                        (Function<String, ProblemDetail>) m -> h.handleRoomIdisNotFoundedException(new RoomIdIsNotFoundedException(m)),
                        HttpStatus.NOT_FOUND, "Id da sala não encontrada"),
                Arguments.of("GroupNotFounded",
                        (Function<String, ProblemDetail>) m -> h.handleGroupNotFounded(new GroupNotFoundedException(m)),
                        HttpStatus.NOT_FOUND, "Grupo não encontrado"),
                Arguments.of("FileNotExistInEfs",
                        (Function<String, ProblemDetail>) m -> h.handleFileNotExistInEfs(new FileNotExistInEfsException(m)),
                        HttpStatus.NOT_FOUND, "O arquivo não existe"),
                Arguments.of("PathNotExist",
                        (Function<String, ProblemDetail>) m -> h.handlePathNotExistException(new PathNotExistException(m)),
                        HttpStatus.NOT_FOUND, "Caminho não existe"),
                Arguments.of("ReservationConflict",
                        (Function<String, ProblemDetail>) m -> h.handleReservationConflictException(new ReservationConflictException(m)),
                        HttpStatus.CONFLICT, "Reserva inválida"),
                Arguments.of("CognitoUnauthorized",
                        (Function<String, ProblemDetail>) m -> h.handleCognitoUnauthorized(new CognitoUnauthorizedException(m)),
                        HttpStatus.FORBIDDEN, "Acesso negado ao Cognito"),
                Arguments.of("CognitoQuery",
                        (Function<String, ProblemDetail>) m -> h.handleCognitoQuery(new CognitoQueryException(m)),
                        HttpStatus.BAD_GATEWAY, "Falha ao consultar o Cognito"),
                Arguments.of("InvalidUrl",
                        (Function<String, ProblemDetail>) m -> h.handleInvalidUrlException(new InvalidUrlException(m)),
                        HttpStatus.INTERNAL_SERVER_ERROR, "A url da imagem foi mal formada"),
                Arguments.of("StartTimeIsNotBeforeEndTime",
                        (Function<String, ProblemDetail>) m -> h.handleStartTimeIsNotBeforeEndTimeException(new StartTimeIsNotBeforeEndTimeException(m)),
                        HttpStatus.BAD_REQUEST, "Tempo inválido"),
                Arguments.of("InvalidPeopleCount",
                        (Function<String, ProblemDetail>) m -> h.handleInvalidPeopleCountException(new InvalidPeopleCountException(m)),
                        HttpStatus.BAD_REQUEST, "Reserva inválida"),
                Arguments.of("NameRequesterIsBlank",
                        (Function<String, ProblemDetail>) m -> h.handleNameRequesterIsBlankException(new NameRequesterIsBlankException(m)),
                        HttpStatus.BAD_REQUEST, "Problema no nome do solicitante"),
                Arguments.of("SameDepartureAndDestination",
                        (Function<String, ProblemDetail>) m -> h.handleSameAddress(new SameDepartureAndDestinationException(m)),
                        HttpStatus.BAD_REQUEST, "Problema no endereço"),
                Arguments.of("UserIdNotFounded",
                        (Function<String, ProblemDetail>) m -> h.handleUserIdNotFounded(new UserIdNotFoundedException(m)),
                        HttpStatus.BAD_REQUEST, "Id do usuario não encontrado"),
                Arguments.of("EmptyRequest",
                        (Function<String, ProblemDetail>) m -> h.handleEmptyRequest(new EmptyRequestException(m)),
                        HttpStatus.BAD_REQUEST, "Campos vazios"),
                Arguments.of("InvalidTimeBetweenReservations",
                        (Function<String, ProblemDetail>) m -> h.handlerInvalidTimeBetweenReservations(new InvalidTimeBetweenReservations(m)),
                        HttpStatus.BAD_REQUEST, "Problema entre os horários"),
                Arguments.of("ReservationWasCancell",
                        (Function<String, ProblemDetail>) m -> h.handleReservationWasCancell(new ReservationWasCancell(m)),
                        HttpStatus.BAD_REQUEST, "Status da reserva"),
                Arguments.of("ReservationWasApprove",
                        (Function<String, ProblemDetail>) m -> h.handleReservationWasApprove(new ReservationWasApprove(m)),
                        HttpStatus.BAD_REQUEST, "Status da reserva"),
                Arguments.of("ReservationWasFinshed",
                        (Function<String, ProblemDetail>) m -> h.handleReservationWasFinshed(new ReservationWasFinshed(m)),
                        HttpStatus.BAD_REQUEST, "Status da reserva"),
                Arguments.of("ReservationWasReject",
                        (Function<String, ProblemDetail>) m -> h.handleReservationWasReject(new ReservationWasReject(m)),
                        HttpStatus.BAD_REQUEST, "Status da reserva"),
                Arguments.of("AttributeIsEmpty",
                        (Function<String, ProblemDetail>) m -> h.handleAttributeIsEmptyException(new AttributeIsEmptyException(m)),
                        HttpStatus.BAD_REQUEST, "Erro nos atributos"),
                Arguments.of("Security",
                        (Function<String, ProblemDetail>) m -> h.handleSecurity(new SecurityException(m)),
                        HttpStatus.FORBIDDEN, "Requisição inválida"),
                Arguments.of("FailedToSaveInEfs",
                        (Function<String, ProblemDetail>) m -> h.handleFailedToSaveInEfs(new FailedToSaveInEfs(m)),
                        HttpStatus.BAD_REQUEST, "Erro no EFS"),
                Arguments.of("InvalidFile",
                        (Function<String, ProblemDetail>) m -> h.handleInvalidFileException(new InvalidFileException(m)),
                        HttpStatus.BAD_REQUEST, "Erro no arquivo"),
                Arguments.of("InvalidTime",
                        (Function<String, ProblemDetail>) m -> h.handleInvalidTimeException(new InvalidTimeException(m)),
                        HttpStatus.BAD_REQUEST, "Hours is invalid"),
                Arguments.of("InvalidReason",
                        (Function<String, ProblemDetail>) m -> h.handleInvalidReasonException(new InvalidReasonException(m)),
                        HttpStatus.BAD_REQUEST, "Error in the Reason"),
                Arguments.of("InvalidReservation",
                        (Function<String, ProblemDetail>) m -> h.handleInvalidReservationException(new InvalidReservationException(m)),
                        HttpStatus.BAD_REQUEST, "Error in the Reservation")
        );
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("handlers")
    void handler_devolveStatusTitleEDetailEsperados(String nome,
                                                    Function<String, ProblemDetail> chamada,
                                                    HttpStatus statusEsperado,
                                                    String titleEsperado) {
        ProblemDetail pd = chamada.apply(MSG);

        assertEquals(statusEsperado.value(), pd.getStatus(), "status errado no handler de " + nome);
        assertEquals(titleEsperado, pd.getTitle(), "title errado no handler de " + nome);
        assertEquals(MSG, pd.getDetail(), "detail nao repassou a mensagem da excecao em " + nome);
    }

    @Test
    void handleEmailNotFoundedException_returnsBadRequestWithTitleAndDetail() {
        ProblemDetail pd = handler.handleEmailNotFoundedException(
                new EmailNotFoundedException("Não foi encontrado o email!"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), pd.getStatus());
        assertEquals("Problema com o e-mail", pd.getTitle());
        assertEquals("Não foi encontrado o email!", pd.getDetail());
    }

    @Test
    void handleErrorDeleteFileException_returnsBadRequestWithTitleAndDetail() {
        ProblemDetail pd = handler.handleErrorDeleteFileException(
                new ErrorDeleteFileException("Erro ao deletar o arquivo!"));

        assertEquals(HttpStatus.BAD_REQUEST.value(), pd.getStatus());
        assertEquals("Erro no arquivo", pd.getTitle());
        assertEquals("Erro ao deletar o arquivo!", pd.getDetail());
    }
}
