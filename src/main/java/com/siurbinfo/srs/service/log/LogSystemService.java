package com.siurbinfo.srs.service.log;

import com.siurbinfo.srs.dto.log.LogResponseDTO;
import com.siurbinfo.srs.entity.log.LogEntity;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.repository.log.LogProjection;
import com.siurbinfo.srs.repository.log.LogRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LogSystemService {

    private static final Logger logger = LoggerFactory.getLogger(LogSystemService.class);

    private final LogRepository repository;
    private final UserRepository userRepository;
    private final CognitoUserService cognitoUserService;
    private final HttpServletRequest request;

    public void log(String modification, String localModification, ReserveType reservationType, Long idData, LogsType logsType, Jwt jwt){
        try {
            String email = cognitoUserService.getEmail(jwt.getTokenValue());

            // grupos vem do claim do proprio access token, sem chamada extra ao Cognito
            List<String> groups = jwt.getClaimAsStringList("cognito:groups");

            String ip = resolveIp();

            Long id = userRepository.getIdByEmail(email);

            LogEntity entity = new LogEntity();
            entity.setModification(modification);
            entity.setLocalModification(localModification);
            entity.setReservationType(reservationType);
            entity.setIdData(idData);
            entity.setLogsType(logsType);
            entity.setUser(id);
            entity.setIp(ip);
            entity.setGroupUser(groups == null ? List.of() : groups);
            repository.save(entity);
        } catch (Exception e) {
            // falha no log de auditoria nao pode derrubar a operacao principal
            logger.error("Falha ao gravar log de auditoria ({} - {}): {}", localModification, logsType, e.getMessage(), e);
        }
    }

    private String resolveIp() {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) {
            // sem ALB (dev local) o IP da conexao direta ja eh a origem
            String remote = request.getRemoteAddr();
            return remote == null || remote.isBlank() ? "0.0.0.0" : remote;
        }
        String[] parts = xff.split(",");
        return parts[Math.max(0, parts.length - 2)].trim();
    }

    public List<LogResponseDTO> getLog(Long offSet){
        Long pag = offSet>0 ? (offSet-1) * 10 : 0;

        List<LogProjection> logsList = repository.getLogs(pag);

        return logsList.stream()
                .map(a -> new LogResponseDTO(a.getId(),
                        a.getReservationType(),
                        a.getModification(),
                        a.getLocalModification(),
                        a.getGivenName(),
                        a.getFamilyName(),
                        a.getEmail(),
                        a.getIp(),
                        a.getGroupUser(),
                        a.getTypeLog(),
                        a.getModificationDate(),
                        a.getIdData()))
                .toList();
    }

}
