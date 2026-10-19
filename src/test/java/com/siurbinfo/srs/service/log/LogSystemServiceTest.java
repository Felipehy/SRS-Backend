package com.siurbinfo.srs.service.log;

import com.siurbinfo.srs.dto.log.LogResponseDTO;
import com.siurbinfo.srs.entity.log.LogEntity;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.repository.log.LogProjection;
import com.siurbinfo.srs.repository.log.LogRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class LogSystemServiceTest {

    private static final String EMAIL = "fulano@prefeitura.sp.gov.br";
    private static final String XFF = "200.1.2.3, 10.0.0.5";

    private LogRepository repository;
    private UserRepository userRepository;
    private CognitoUserService cognitoUserService;
    private HttpServletRequest request;
    private LogSystemService service;

    private static Jwt jwtComGrupos(List<String> grupos) {
        Jwt.Builder builder = Jwt.withTokenValue("token-fake")
                .header("alg", "none")
                .claim("sub", EMAIL);
        if (grupos != null) {
            builder.claim("cognito:groups", grupos);
        }
        return builder.build();
    }

    @BeforeEach
    void setUp() {
        repository = mock(LogRepository.class);
        userRepository = mock(UserRepository.class);
        cognitoUserService = mock(CognitoUserService.class);
        request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(XFF);
        service = new LogSystemService(repository, userRepository, cognitoUserService, request);
    }

    @Test
    void log_gravaEntidadeComTodosOsCampos() {
        when(cognitoUserService.getEmail("token-fake")).thenReturn(EMAIL);
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);

        service.log("criou reserva", "auditorio", ReserveType.AUDITORIUM, 42L, LogsType.CREATE,
                jwtComGrupos(List.of("siurb-administrador")));

        ArgumentCaptor<LogEntity> captor = ArgumentCaptor.forClass(LogEntity.class);
        verify(repository).save(captor.capture());

        LogEntity salvo = captor.getValue();
        assertEquals("criou reserva", salvo.getModification());
        assertEquals("auditorio", salvo.getLocalModification());
        assertEquals(ReserveType.AUDITORIUM, salvo.getReservationType());
        assertEquals(42L, salvo.getIdData());
        assertEquals(LogsType.CREATE, salvo.getLogsType());
        assertEquals(7L, salvo.getUser());
        assertEquals("200.1.2.3", salvo.getIp());
        assertEquals(List.of("siurb-administrador"), salvo.getGroupUser());
    }

    @Test
    void log_semClaimDeGrupos_gravaListaVazia() {
        when(cognitoUserService.getEmail("token-fake")).thenReturn(EMAIL);
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);

        service.log("alterou", "sala", ReserveType.MEETING_ROOM, 1L, LogsType.UPDATE, jwtComGrupos(null));

        ArgumentCaptor<LogEntity> captor = ArgumentCaptor.forClass(LogEntity.class);
        verify(repository).save(captor.capture());

        assertNotNull(captor.getValue().getGroupUser());
        assertTrue(captor.getValue().getGroupUser().isEmpty());
    }

    @Test
    void log_usuarioNaoEncontrado_gravaLogComUserNulo() {
        when(cognitoUserService.getEmail("token-fake")).thenReturn(EMAIL);
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(null);

        service.log("removeu", "frota", ReserveType.FLEET_VEHICLE, 3L, LogsType.DELETE,
                jwtComGrupos(List.of("siurb-divisao-frotas")));

        ArgumentCaptor<LogEntity> captor = ArgumentCaptor.forClass(LogEntity.class);
        verify(repository).save(captor.capture());
        assertNull(captor.getValue().getUser());
    }

    @Test
    void log_semHeaderDeIp_caiParaOIpDaConexaoDireta() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(cognitoUserService.getEmail("token-fake")).thenReturn(EMAIL);
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);

        service.log("criou", "auditorio", ReserveType.AUDITORIUM, 1L, LogsType.CREATE,
                jwtComGrupos(List.of("siurb-administrador")));

        ArgumentCaptor<LogEntity> captor = ArgumentCaptor.forClass(LogEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("127.0.0.1", captor.getValue().getIp());
    }

    @Test
    void log_headerDeIpVazio_caiParaOIpDaConexaoDireta() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(request.getRemoteAddr()).thenReturn("192.168.0.10");
        when(cognitoUserService.getEmail("token-fake")).thenReturn(EMAIL);
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);

        service.log("criou", "auditorio", ReserveType.AUDITORIUM, 1L, LogsType.CREATE,
                jwtComGrupos(List.of("siurb-administrador")));

        ArgumentCaptor<LogEntity> captor = ArgumentCaptor.forClass(LogEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("192.168.0.10", captor.getValue().getIp());
    }

    @Test
    void log_semHeaderESemIpDeConexao_gravaIpNeutroSemPerderOLog() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);
        when(cognitoUserService.getEmail("token-fake")).thenReturn(EMAIL);
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);

        service.log("criou", "auditorio", ReserveType.AUDITORIUM, 1L, LogsType.CREATE,
                jwtComGrupos(List.of("siurb-administrador")));

        ArgumentCaptor<LogEntity> captor = ArgumentCaptor.forClass(LogEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("0.0.0.0", captor.getValue().getIp());
    }

    @Test
    void log_headerComUmUnicoIp_usaEsseIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("200.1.2.3");
        when(cognitoUserService.getEmail("token-fake")).thenReturn(EMAIL);
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);

        service.log("criou", "auditorio", ReserveType.AUDITORIUM, 1L, LogsType.CREATE,
                jwtComGrupos(List.of("siurb-administrador")));

        ArgumentCaptor<LogEntity> captor = ArgumentCaptor.forClass(LogEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("200.1.2.3", captor.getValue().getIp());
    }

    @Test
    void log_headerComVariosProxies_usaOPenultimo() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("200.1.2.3, 172.16.0.9, 10.0.0.5");
        when(cognitoUserService.getEmail("token-fake")).thenReturn(EMAIL);
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);

        service.log("criou", "auditorio", ReserveType.AUDITORIUM, 1L, LogsType.CREATE,
                jwtComGrupos(List.of("siurb-administrador")));

        ArgumentCaptor<LogEntity> captor = ArgumentCaptor.forClass(LogEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("172.16.0.9", captor.getValue().getIp());
    }

    @Test
    void log_falhaNoCognito_naoPropagaExcecao() {
        when(cognitoUserService.getEmail("token-fake")).thenThrow(new RuntimeException("cognito fora do ar"));

        assertDoesNotThrow(() -> service.log("criou", "auditorio", ReserveType.AUDITORIUM, 1L, LogsType.CREATE,
                jwtComGrupos(List.of("siurb-administrador"))));

        verifyNoInteractions(repository);
    }

    @Test
    void log_falhaAoSalvar_naoPropagaExcecao() {
        when(cognitoUserService.getEmail("token-fake")).thenReturn(EMAIL);
        when(userRepository.getIdByEmail(EMAIL)).thenReturn(7L);
        when(repository.save(any(LogEntity.class))).thenThrow(new RuntimeException("banco fora do ar"));

        assertDoesNotThrow(() -> service.log("criou", "auditorio", ReserveType.AUDITORIUM, 1L, LogsType.CREATE,
                jwtComGrupos(List.of("siurb-administrador"))));
    }

    @Test
    void log_jwtNulo_naoPropagaExcecao() {
        assertDoesNotThrow(() -> service.log("criou", "auditorio", ReserveType.AUDITORIUM, 1L, LogsType.CREATE, null));

        verifyNoInteractions(repository);
    }

    @Test
    void getLog_primeiraPagina_usaOffsetZero() {
        when(repository.getLogs(anyLong())).thenReturn(List.of());

        service.getLog(1L);

        verify(repository).getLogs(0L);
    }

    @Test
    void getLog_offsetZero_usaOffsetZero() {
        when(repository.getLogs(anyLong())).thenReturn(List.of());

        service.getLog(0L);

        verify(repository).getLogs(0L);
    }

    @Test
    void getLog_paginaSeguinte_pulaDeDezEmDez() {
        when(repository.getLogs(anyLong())).thenReturn(List.of());

        service.getLog(3L);

        verify(repository).getLogs(20L);
    }

    @Test
    void getLog_mapeiaProjecaoParaDto() {
        Instant quando = Instant.now();
        LogProjection projection = mock(LogProjection.class);
        when(projection.getId()).thenReturn(1L);
        when(projection.getReservationType()).thenReturn("AUDITORIUM");
        when(projection.getModification()).thenReturn("criou reserva");
        when(projection.getLocalModification()).thenReturn("auditorio");
        when(projection.getGivenName()).thenReturn("Fulano");
        when(projection.getFamilyName()).thenReturn("de Tal");
        when(projection.getEmail()).thenReturn(EMAIL);
        when(projection.getIp()).thenReturn("200.1.2.3");
        when(projection.getGroupUser()).thenReturn("{siurb-administrador}");
        when(projection.getTypeLog()).thenReturn("CREATE");
        when(projection.getModificationDate()).thenReturn(quando);
        when(projection.getIdData()).thenReturn(42L);

        when(repository.getLogs(0L)).thenReturn(List.of(projection));

        List<LogResponseDTO> logs = service.getLog(1L);

        assertEquals(1, logs.size());
        LogResponseDTO dto = logs.getFirst();
        assertEquals(1L, dto.id());
        assertEquals("AUDITORIUM", dto.reservationType());
        assertEquals("criou reserva", dto.modification());
        assertEquals("auditorio", dto.localModification());
        assertEquals("Fulano", dto.givenName());
        assertEquals("de Tal", dto.familyName());
        assertEquals(EMAIL, dto.email());
        assertEquals("200.1.2.3", dto.ip());
        assertEquals("{siurb-administrador}", dto.groups());
        assertEquals("CREATE", dto.typeLog());
        assertEquals(quando, dto.modificationDate());
        assertEquals(42L, dto.idData());
    }

    @Test
    void getLog_semRegistros_devolveListaVazia() {
        when(repository.getLogs(0L)).thenReturn(List.of());

        assertTrue(service.getLog(1L).isEmpty());
    }
}
