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
import com.siurbinfo.srs.exception.AttributeIsEmptyException;
import com.siurbinfo.srs.exception.CognitoQueryException;
import com.siurbinfo.srs.exception.CognitoUnauthorizedException;
import com.siurbinfo.srs.exception.DepartmentIdNotFoundedException;
import com.siurbinfo.srs.exception.UserIdNotFoundedException;
import com.siurbinfo.srs.mapper.department.DepartmentMapper;
import com.siurbinfo.srs.mapper.user.UserMapper;
import com.siurbinfo.srs.repository.user.ReservationsProjection;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.group.CognitoGroupServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Profile("prod")
public class CognitoUserServiceImpl implements CognitoUserService {

    private static final Logger log = LoggerFactory.getLogger(CognitoUserServiceImpl.class);
    private final CognitoIdentityProviderClient client;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final CognitoGroupServiceImpl cognitoGroupService;
    private final DepartmentMapper departmentMapper;
    String temporaryPassword =  "";
    String userPoolId = "";


    public CognitoUserServiceImpl(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
                                  @Value("${spring.temporary-password}") String temporaryPassword,
                                  UserMapper userMapper,
                                  UserRepository repository,
                                  CognitoGroupServiceImpl cognitoGroupService,
                                  DepartmentMapper departmentMapper) {
        URI issuer = URI.create(issuerUri);
        this.userMapper = userMapper;
        this.userRepository = repository;
        this.temporaryPassword = temporaryPassword;
        this.departmentMapper = departmentMapper;
        this.cognitoGroupService = cognitoGroupService;
        userPoolId = issuer.getPath().replaceFirst("^/", "");
        String region = userPoolId.substring(0, userPoolId.indexOf('_'));
        this.client = CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public String getName(String accessToken) {
        GetUserResponse response = fetchUser(accessToken);
        String fullName = Stream.of(extract(response, "given_name"), extract(response, "family_name"))
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return fullName.isBlank() ? null : fullName;
    }

    @Override
    public UserResponseDTO getUser(Jwt jwt) {

        if (jwt == null) {
            throw new SecurityException("A chave de acesso está vazia");
        }
        String email = getEmail(jwt.getTokenValue());
        UserEntity entity = userRepository.getUserByEmail(email);
        List<GroupResponse> groups = cognitoGroupService.listGroupsForUser(email);
        // usuario existe no Cognito mas nao tem linha em tbl_user
        if (entity == null) {
            return new UserResponseDTO(null, email, null, null, toMapColors(null), null, groups, null, null);
        }
        // id_department e nullable (V20); getId() no proxy lazy nao dispara carregamento
        Long idDepartment = entity.getDepartment() != null ? entity.getDepartment().getId() : null;
        return new UserResponseDTO(entity.getId(),
                entity.getEmail(),entity.getGivenName(), entity.getFamilyName(), toMapColors(entity), entity.getIsHorizontalNavbar(), groups,
                idDepartment,entity.getIsFirstLogin());

    }

    // Usuario sem linha em tbl_user ou sem cores definidas -> valores null (Map.of nao aceita null).
    private Map<String, String> toMapColors(UserEntity entity){
        Map<String, String> map = new HashMap<>();
        map.put("colorAuditorium", entity != null ? entity.getColorAuditorium() : null);
        map.put("colorFleetVehicle", entity != null ? entity.getColorFleetVehicles() : null);
        map.put("colorMeetingRoom", entity != null ? entity.getColorMeetingRoom() : null);
        return map;
    }


    @Override
    public String getEmail(String acessToken) {
        GetUserResponse response = fetchUser(acessToken);
        return response.userAttributes().stream()
                .filter(a -> a.name().equals("email"))
                .map(AttributeType::value)
                .findFirst()
                .orElse(null);
    }

    @Override
    public String getAttribute(String accessToken, String attributeName) {
        return extract(fetchUser(accessToken), attributeName);
    }

    private GetUserResponse fetchUser(String accessToken) {
        try {
            return client.getUser(request -> request.accessToken(accessToken));
        } catch (NotAuthorizedException e) {
            // token expirado/invalido ou sem o escopo aws.cognito.signin.user.admin
            log.warn("GetUser negado pelo Cognito - token invalido ou sem o escopo "
                    + "'aws.cognito.signin.user.admin': {}", awsMessage(e));
            throw new CognitoUnauthorizedException(
                    "Token invalido ou sem o escopo 'aws.cognito.signin.user.admin' para consultar o Cognito.");
        } catch (SdkException e) {
            // cobre erros de servico (CognitoIdentityProviderException) e de cliente/rede
            log.error("Falha ao consultar dados do usuario no Cognito via GetUser: {}", e.getMessage(), e);
            throw new CognitoQueryException("Falha ao consultar dados do usuario no Cognito.");
        }
    }

    private static String extract(GetUserResponse response, String attributeName) {
        return response.userAttributes().stream()
                .filter(attribute -> attributeName.equals(attribute.name()))
                .map(AttributeType::value)
                .findFirst()
                .orElse(null);
    }

    private static String awsMessage(NotAuthorizedException e) {
        return e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage();
    }

    @Override
    public UserResponseMessage createUser(UserRequest dto){

        UserEntity user = userMapper.toEntity(dto);
        user.setGivenName(dto.givenName().strip());
        user.setFamilyName(dto.familyName().strip());
        user.setDepartment(userMapper.resolveDepartment(dto.idDepartment()));
        String email = dto.email().strip();

        List<AttributeType> attributes = new ArrayList<>();
        attributes.add(attr("given_name", dto.givenName().strip()));
        attributes.add(attr("family_name", dto.familyName().strip()));
        attributes.add(attr("email", email));
        attributes.add(attr("email_verified", "true"));

        String phoneNumber = normalizePhoneToE164(dto.phoneNumber());
        if (phoneNumber != null) {
            attributes.add(attr("phone_number", phoneNumber));
        }

        AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                .userPoolId(this.userPoolId)
                .username(email)
                .userAttributes(attributes)
                .temporaryPassword(temporaryPassword)
                .build();

        this.client.adminCreateUser(createUserRequest);
        this.userRepository.save(user);
        return new UserResponseMessage(userRepository.getIdByEmail(dto.email()), "Conta criada com sucesso",dto.givenName());
    }

    private static AttributeType attr(String name, String value){
        return AttributeType.builder()
                .name(name)
                .value(value)
                .build();
    }

    private static String normalizePhoneToE164(String raw){
        if (raw == null) {
            return null;
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.isEmpty()) {
            return null;
        }
        // Já inclui o código do país (55): DDD + número + 55 -> 12 ou 13 dígitos.
        if (digits.length() == 12 || digits.length() == 13) {
            return "+" + digits;
        }
        // Número nacional: DDD + número (10 ou 11 dígitos).
        return "+55" + digits;
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
    public List<UserResponse> listUsers(){

        String paginationToken = null;
        List<UserResponse> allUsers = new ArrayList<>();

        do{
            ListUsersRequest usersRequest = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .limit(60)
                    .paginationToken(paginationToken)
                    .build();

            ListUsersResponse usersResponse = this.client.listUsers(usersRequest);

            List<UserResponse> listUsersDTO = usersResponse.users().stream()
                    .map(u -> new UserResponse(
                            u.username(),
                            att(u,"given_name"),
                            att(u,"family_name"),
                            att(u,"email"),
                            att(u,"phone_number"),
                            u.userStatusAsString(),
                            userRepository.getIdDepartmentByEmail(att(u,"email"))
                    ))
                    .toList();

            allUsers.addAll(listUsersDTO);
            paginationToken = usersResponse.paginationToken();

        } while (paginationToken != null);

        return allUsers;

    }

    private String att(UserType u, String value){
        return u.attributes().stream()
                .filter(f -> f.name().equals(value))
                .map(AttributeType::value)
                .findFirst().orElse(null);
    }

    @Override
    public UserResponseMessage deleteUser(DeleteUserRequest dto){
        AdminDeleteUserRequest userRequest = AdminDeleteUserRequest.builder()
                .userPoolId(userPoolId)
                .username(dto.email())
                .build();

        Instant dateTime = Instant.now();
        // le o id antes: depois do deleteByEmail o getIdByEmail ja nao enxerga o usuario
        // e o log de auditoria da exclusao ficaria sem saber quem saiu
        Long id = userRepository.getIdByEmail(dto.email());

        this.client.adminDeleteUser(userRequest);
        this.userRepository.deleteByEmail(dto.email(),dateTime);
        return new UserResponseMessage(id, "Pessoa deletada com sucesso!",null);
    }

    @Override
    public UserResponseMessage updateUserAtt(UserRequest dto){

        List<AttributeType> attributeTypes = new ArrayList<>();

        AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                .userPoolId(userPoolId)
                .username(dto.email())
                .build();

        AdminGetUserResponse user = this.client.adminGetUser(getUserRequest);

        UserEntity entity = userRepository.getUserByEmail(dto.email());

        if (!dto.givenName().isBlank() && !isEqualAtt(user,"given_name",dto.givenName())){
            attributeTypes.add(attr("given_name", dto.givenName()));
        }

        if (!dto.familyName().isBlank() && !isEqualAtt(user,"family_name", dto.familyName())){
            attributeTypes.add(attr("family_name", dto.familyName()));
        }

        if (attributeTypes.isEmpty()){throw new AttributeIsEmptyException("Campos vazios ou informações parecidas");}

        AdminUpdateUserAttributesRequest updateUserAttributesRequest = AdminUpdateUserAttributesRequest.builder()
                .userPoolId(userPoolId)
                .username(dto.email())
                .userAttributes(attributeTypes)
                .build();

        userMapper.updateEntity(dto,entity);
        this.client.adminUpdateUserAttributes(updateUserAttributesRequest);
        userRepository.save(entity);

        return new UserResponseMessage(userRepository.getIdByEmail(dto.email()),"Usuario atualizado com sucesso", dto.givenName());
    }

    private Boolean isEqualAtt(AdminGetUserResponse user, String name , String value){
        return (user.userAttributes().stream()
                .filter(a -> a.name().equals(name))
                .findFirst()
                .map(v -> v.value().equals(value))
                .orElse(false));
    }

    public UserResponseMessage resetPassword(String email, Jwt jwt){
        AdminSetUserPasswordRequest adminResetUserPasswordRequest = AdminSetUserPasswordRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .password(temporaryPassword)
                .permanent(false)
                .build();

        Long id = userRepository.getIdByEmail(email);

        this.client.adminSetUserPassword(adminResetUserPasswordRequest);
        return new UserResponseMessage(id, "Reset de senha", email);
    }

    @Override
    public void resetMFA(String email) {
        this.client.adminDeleteSoftwareToken(AdminDeleteSoftwareTokenRequest.builder()
                        .userPoolId(userPoolId)
                        .username(email)
                .build());
    }

    @Transactional
    @Override
    public void setColors(Jwt jwt, ColorsRequestDTO dto){
        if (jwt == null){
            throw new SecurityException("A chave de acesso está vazia");
        }
        String email = getEmail(jwt.getTokenValue());
        userRepository.setColors(email, dto.colorAuditorium(), dto.colorFleetVehicles(), dto.colorMeetingRoom());
    }

    @Transactional
    @Override
    public void finishTutorial(Jwt jwt) {
        if (jwt == null) {
            throw new SecurityException("A chave de acesso está vazia");
        }
        userRepository.setFirstLogin(false,getEmail(jwt.getTokenValue()));
    }


    @Transactional
    @Override
    public void setNavbar(Boolean navbar, Jwt jwt) {
        if (jwt == null) {
            throw new SecurityException("A chave de acesso está vazia");
        }

        String email = getEmail(jwt.getTokenValue());

        userRepository.setNavbar(navbar,email);
    }


    // readOnly = true mantem a sessao aberta: o mapper le room.floor (associacao LAZY)
    @Transactional(readOnly = true)
    @Override
    public DepartmentResponseDTO getDepartmentFromUser(Long id, Jwt jwt){
        if (jwt == null) {throw new SecurityException("Is necessary a Json web token");}
        if (!userRepository.getIdByEmail(getEmail(jwt.getTokenValue())).equals(id)){throw new SecurityException("É necessario estar logado na conta do usuário para pegar seu departmento");}
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new UserIdNotFoundedException("Usuário não encontrado: " + id));
        // id_department e nullable (V20): usuario sem vinculo nao tem departamento pra devolver
        if (userEntity.getDepartment() == null) {
            throw new DepartmentIdNotFoundedException("Usuário não possui departamento vinculado");
        }
        return departmentMapper.toResponseDTO(userEntity.getDepartment());
    }

    @Transactional
    @Override
    public void setDepartment(Long id, String email){
        userRepository.setDepartment(id, email);
    }

}
