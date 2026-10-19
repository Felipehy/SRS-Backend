package com.siurbinfo.srs.service.cognito.user;

import com.siurbinfo.srs.dto.cognito.group.GroupResponse;
import com.siurbinfo.srs.dto.cognito.user.DeleteUserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponse;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.dto.department.DepartmentResponseDTO;
import com.siurbinfo.srs.dto.reservation.ReservationsDTO;
import com.siurbinfo.srs.dto.user.ColorsRequestDTO;
import com.siurbinfo.srs.dto.user.ListReservationsUserDTO;
import com.siurbinfo.srs.dto.user.UserResponseDTO;
import com.siurbinfo.srs.entity.user.UserEntity;
import com.siurbinfo.srs.enums.ReserveType;
import com.siurbinfo.srs.exception.DepartmentIdNotFoundedException;
import com.siurbinfo.srs.exception.UserIdNotFoundedException;
import com.siurbinfo.srs.mapper.department.DepartmentMapper;
import com.siurbinfo.srs.repository.user.ReservationsProjection;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.group.CognitoGroupService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Versao do CognitoUserService para o perfil "dev": devolve o usuario fake
 * definido no application-dev.yaml sem chamar a AWS. As operacoes que
 * administram usuarios no Cognito nao sao suportadas fora de producao.
 */
@Service
@Profile("dev")
public class DevCognitoUserService implements CognitoUserService {

    private final UserRepository userRepository;
    private final CognitoGroupService cognitoGroupService;
    private final DepartmentMapper departmentMapper;
    private final String email;
    private final String givenName;
    private final String familyName;

    public DevCognitoUserService(UserRepository userRepository,
                                 CognitoGroupService cognitoGroupService,
                                 DepartmentMapper departmentMapper,
                                 @Value("${app.fake-user.email}") String email,
                                 @Value("${app.fake-user.given-name}") String givenName,
                                 @Value("${app.fake-user.family-name}") String familyName) {
        this.userRepository = userRepository;
        this.cognitoGroupService = cognitoGroupService;
        this.departmentMapper = departmentMapper;
        this.email = email;
        this.givenName = givenName;
        this.familyName = familyName;
    }

    @Override
    public String getName(String accessToken) {
        return givenName + " " + familyName;
    }

    @Override
    public String getEmail(String acessToken) {
        return email;
    }

    @Override
    public String getAttribute(String accessToken, String attributeName) {
        return switch (attributeName) {
            case "email" -> email;
            case "given_name" -> givenName;
            case "family_name" -> familyName;
            default -> null;
        };
    }

    @Override
    public UserResponseDTO getUser(Jwt jwt) {
        if (jwt == null) {
            throw new SecurityException("A chave de acesso está vazia");
        }
        List<GroupResponse> groups = cognitoGroupService.listGroupsForUser(
                email);
        UserEntity entity = userRepository.getUserByEmail(email);
        // usuario fake ainda sem linha em tbl_user no banco de dev
        if (entity == null) {
            return new UserResponseDTO(null, email, null, null, toMapColors(null), null, groups, null, null);
        }
        Long idDepartment = entity.getDepartment() != null ? entity.getDepartment().getId() : null;
        return new UserResponseDTO(entity.getId(),
                entity.getEmail(),entity.getGivenName(), entity.getFamilyName(), toMapColors(entity), entity.getIsHorizontalNavbar(), groups,
                idDepartment,entity.getIsFirstLogin());
    }

    private Map<String, String> toMapColors(UserEntity entity){
        Map<String, String> map = new HashMap<>();
        map.put("colorAuditorium", entity != null ? entity.getColorAuditorium() : null);
        map.put("colorFleetVehicle", entity != null ? entity.getColorFleetVehicles() : null);
        map.put("colorMeetingRoom", entity != null ? entity.getColorMeetingRoom() : null);
        return map;
    }


    @Override
    public List<UserResponse> listUsers() {
        // devolve apenas o proprio usuario fake, para a tela de listagem nao vir vazia
        Long idDepartment = userRepository.getIdDepartmentByEmail(email);
        return List.of(new UserResponse(email, givenName, familyName, email, null, "CONFIRMED", idDepartment));
    }

    @Override
    public ReservationsDTO getReservationsByUser(Jwt jwt, LocalDate date, Long pag, List<String> types) {
        if (jwt == null){
            throw new SecurityException("Is necessary a json web token(jwt)");
        }
        Long id = userRepository.getIdByEmail(getEmail(jwt.getTokenValue()));
        // sem filtro de tipo traz os tres: "in :types" com null vira erro de sintaxe no Postgres
        List<String> filterTypes = (types == null || types.isEmpty())
                ? Arrays.stream(ReserveType.values()).map(Enum::name).toList()
                : types;
        List<ReservationsProjection> reservationsUserProjections = userRepository.getReservationsByIdUser(id,date,pag*10,filterTypes);
        Long numReservations = reservationsUserProjections.isEmpty() ? 0L : reservationsUserProjections.getFirst().totalReservations();
        List<ListReservationsUserDTO> reservationsUserDTOS = reservationsUserProjections.stream()
                .map(r -> new ListReservationsUserDTO(r.id(),
                        r.reservationType(), r.status(), r.reservationDate(), r.startTime(), r.endTime(), r.userNameRequester(), r.idUser()))
                .toList();
        return new ReservationsDTO(numReservations,reservationsUserDTOS);
    }

    @Override
    public UserResponseMessage createUser(UserRequest dto) {
        throw new UnsupportedOperationException("Gestao de usuarios do Cognito indisponivel no perfil dev");
    }

    @Override
    public UserResponseMessage deleteUser(DeleteUserRequest dto) {
        throw new UnsupportedOperationException("Gestao de usuarios do Cognito indisponivel no perfil dev");
    }

    @Override
    public UserResponseMessage updateUserAtt(UserRequest dto) {
        throw new UnsupportedOperationException("Gestao de usuarios do Cognito indisponivel no perfil dev");
    }

    @Override
    public UserResponseMessage resetPassword(String email, Jwt jwt){
        throw new UnsupportedOperationException("Gestao de usuarios do Cognito indisponivel no perfil dev");
    }

    @Override
    public void resetMFA(String email) {
        throw new UnsupportedOperationException("Gestao de usuarios do Cognito indisponivel no perfil dev");
    }

    @Override
    @Transactional
    public void setColors(Jwt jwt, ColorsRequestDTO dto) {
        if (jwt == null) {
            throw new SecurityException("A chave de acesso está vazia");
        }
        userRepository.setColors(email, dto.colorAuditorium(), dto.colorFleetVehicles(), dto.colorMeetingRoom());
    }

    @Transactional
    @Override
    public void setNavbar(Boolean navbar, Jwt jwt) {
        if (jwt == null) {
            throw new SecurityException("A chave de acesso está vazia");
        }
        userRepository.setNavbar(navbar,email);
    }

    @Transactional
    @Override
    public void finishTutorial(Jwt jwt) {
        if (jwt == null) {
            throw new SecurityException("A chave de acesso está vazia");
        }
        userRepository.setFirstLogin(false,email);
    }

    @Transactional
    @Override
    public void setDepartment(Long id, String email){
        userRepository.setDepartment(id,email);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentResponseDTO getDepartmentFromUser(Long id, Jwt jwt) {
        if (jwt == null) {
            throw new SecurityException("Is necessary a Json web token");
        }
        if (!userRepository.getIdByEmail(email).equals(id)) {
            throw new SecurityException("É necessario estar logado na conta do usuário para pegar seu departmento");
        }
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new UserIdNotFoundedException("Usuário não encontrado: " + id));
        if (userEntity.getDepartment() == null) {
            throw new DepartmentIdNotFoundedException("Usuário não possui departamento vinculado");
        }
        return departmentMapper.toResponseDTO(userEntity.getDepartment());
    }

}
