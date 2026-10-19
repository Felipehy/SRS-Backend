package com.siurbinfo.srs.service.glpi;

import com.siurbinfo.srs.dto.glpi.GlpiRequestDTO;
import com.siurbinfo.srs.dto.glpi.GlpiResponseDTO;
import com.siurbinfo.srs.dto.glpi.GlpiSessionTokenDTO;
import com.siurbinfo.srs.dto.glpi.UserNotifDTO;
import com.siurbinfo.srs.entity.reservation.AuditoriumEntity;
import com.siurbinfo.srs.repository.reservation.ReservationAuditoriumRepository;
import com.siurbinfo.srs.service.smtp.SmtpService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GlpiService {

    private static final DateTimeFormatter GLPI_DATE_HOUR = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter GLPI_DATE_DAY_AND_MONTH = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter GLPI_DATE_HOUR_MINUTES = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter GLPI_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");


    @Value("${app.glpi.full-session-url}")
    private String GET_FULL_SESSION;

    @Value("${app.glpi.authorization}")
    private String AUTHORIZATION;

    @Value("${app.glpi.app-token}")
    private String APP_TOKEN;

    @Value("${app.glpi.uri}")
    private String uri;

    private GlpiSessionTokenDTO sessionToken;

    private final ReservationAuditoriumRepository auditoriumRepository;
    private final SmtpService smtpService;

    @Transactional
    public void createTicket(Long id){
        AuditoriumEntity entity = auditoriumRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Auditório não encontrado: " + id));
        GlpiResponseDTO response = postTicket(entity);
        auditoriumRepository.putIdTicket(response.id(), id);
    }

    public GlpiResponseDTO postTicket(AuditoriumEntity entity){
        return restClient().post()
                .body(createBody(entity))
                .retrieve()
                .body(GlpiResponseDTO.class);
    }

    private @NonNull RestClient restClient(){
        return RestClient.builder()
                .baseUrl(URI.create(uri+"/Ticket"))
                .defaultHeader("App-Token",APP_TOKEN)
                .defaultHeader("Session-Token",getSessionToken())
                .defaultHeader("Authorization", AUTHORIZATION)
                .build();
    }

    Map<String, GlpiRequestDTO> createBody(AuditoriumEntity entity){
        String resolution = LocalDateTime.of(entity.getReservationDate(), entity.getTimeOpenAuditorium())
                .format(GLPI_DATE_HOUR);
        // ate as 8h em ponto quem abre o auditorio e uma pessoa (63); depois disso fica com a equipe (grupo 2)
        LocalTime abertura = entity.getTimeOpenAuditorium();
        Long idUser = abertura.isAfter(LocalTime.of(8,0,0)) ? null : 63L;
        Long idGroup = abertura.isAfter(LocalTime.of(8,0,0)) ? 2L : null;
        return Map.of("input",new GlpiRequestDTO(
                nameTicket(entity.getTimeOpenAuditorium(),entity.getReservationDate()),                 // name
                contentTicket(entity),
                2L,                                       // type: 1 = incidente, 2 = requisicao
                3L,                                       // urgency
                3L,                                       // impact
                3L,                                       // priority
                3L,                                       // status: 1 = novo

                0L,                                       // entities_id
                168L,                                       // itilcategories_id
                1L,                                       // locations_id
                1L,                                       // requesttypes_id

                resolution,                                     // date
                resolution,                                    // resolution_date

                394L,                                       // _users_id_requester
                idUser,                                       // _users_id_assign
                idGroup,                                    // _groups_id_assign

                new UserNotifDTO(0, null),
                new UserNotifDTO(0, null)
        ));
    }

    private String nameTicket(LocalTime time, LocalDate date){
        return "Acompanhamento no Auditório - " + date.format(GLPI_DATE_DAY_AND_MONTH) + " às " + time.format(GLPI_DATE_HOUR_MINUTES);
    }

    private String contentTicket(AuditoriumEntity entity){
        StringBuilder sb = new StringBuilder();
        sb.append("DATA DO EVENTO: ").append(entity.getReservationDate().format(GLPI_DATE));
        sb.append("\n\nHORÁRIO DA ABERTURA DO AUDITÓRIO: ").append(entity.getTimeOpenAuditorium().format(GLPI_DATE_HOUR_MINUTES));
        if (!entity.getTimeOpenAuditorium().equals(entity.getStartTime())){
            sb.append("\n\nMOTIVO ABERTURA ANTECIPADA: ").append(entity.getReasonOpenAuditorium());
        }
        sb.append("\n\nHORÁRIO DO INÍCIO DO EVENTO: ").append(entity.getStartTime().format(GLPI_DATE_HOUR_MINUTES));
        sb.append("\n\nHORÁRIO DO TÉRMINO: ").append(entity.getEndTime().format(GLPI_DATE_HOUR_MINUTES));
        sb.append("\n\nNUMERO ESTIMADO DE PESSOAS: ").append(entity.getNumPeople());
        sb.append("\n\nHAVERÁ PÚBLICO EXTERNO? ").append(entity.isExternalPublic() ? "Sim" : "Não");
        if (entity.isExternalPublic()){
            sb.append("\n\nQUANTAS PESSOAS? ").append(entity.getNumExternalPeople() == null ? "Não informado" : entity.getNumExternalPeople());
        }
        sb.append("\n\nASSUNTO DO EVENTO/RESERVA: ").append(entity.getReason());
        sb.append("\n\nRESPONSÁVEL PELO EVENTO/RESERVA: ").append(entity.getUserNameRequester());
        sb.append("\n\nTEL REPONSÁVEL: ").append(entity.getTelephone() == null ? "Não informado" : entity.getTelephone());
        sb.append("\n\nEMAIL RESPONSÁVEL: ").append(entity.getUser() == null ? "Não informado" : entity.getUser().getEmail());
        sb.append("\n\nNECESSÁRIO O USO DO TELÃO?: ").append(entity.isNecessaryScreen() ? "Sim" : "Não");
        sb.append("\n\nNECESSÁRIO USO DO SISTEMA DE SOM?: ").append(entity.isNecessarySoundSystem() ? "Sim" : "Não");
        sb.append("\n\nOBSERVAÇÕES: ").append(entity.getObservation() == null ? "Nenhuma" : entity.getObservation());
        sb.append("\n\nhttps://radiosaovivo.net/regiao/sao-paulo/#antena");
        return sb.toString();
    }

    public String getSessionToken(){

        if(sessionToken != null && Duration.between(sessionToken.createToken(), Instant.now()).toMinutes() <= sessionToken.expireToken()){
            return sessionToken.sessionToken();
        }

        RestClient restClient = RestClient.builder()
                .baseUrl(URI.create(uri+"/v1/initSession"))
                .defaultHeader("get_full_session", GET_FULL_SESSION)
                .defaultHeader("Authorization",AUTHORIZATION)
                .defaultHeader("App-Token",APP_TOKEN)
                .defaultHeader("Content-Type","application/json")
                .build();
        GlpiSessionTokenDTO response = restClient.get().retrieve().body(GlpiSessionTokenDTO.class);

        if(response == null){throw new NullPointerException("Não foi possivel criar uma nova chave api");}
        sessionToken = new GlpiSessionTokenDTO(response.sessionToken(),19,Instant.now());
        return sessionToken.sessionToken();
    }

}
