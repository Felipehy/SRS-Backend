package com.siurbinfo.srs.service.cognito;

import com.siurbinfo.srs.dto.cognito.user.DeleteUserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponse;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.entity.UserEntity;
import com.siurbinfo.srs.exception.AttributeIsEmptyException;
import com.siurbinfo.srs.exception.CognitoQueryException;
import com.siurbinfo.srs.exception.CognitoUnauthorizedException;
import com.siurbinfo.srs.mapper.user.UserMapper;
import com.siurbinfo.srs.repository.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Consulta atributos do proprio usuario autenticado no Cognito via GetUser.
 *
 * GetUser e autorizado pelo proprio access token do usuario (nao pelo IAM): o
 * token precisa conter o escopo "aws.cognito.signin.user.admin" e cada usuario
 * so consegue ler os proprios atributos. A aplicacao NAO precisa de nenhuma
 * permissao IAM sobre o Cognito.
 */
@Service
public class CognitoUserService {

    private static final Logger log = LoggerFactory.getLogger(CognitoUserService.class);
    private final CognitoIdentityProviderClient client;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    String temporaryPassword =  "";
    String userPoolId = "";


    public CognitoUserService(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
                              @Value("${spring.temporary-password}") String temporaryPassword,
                              UserMapper userMapper,
                              UserRepository repository) {
        URI issuer = URI.create(issuerUri);
        this.userMapper = userMapper;
        this.userRepository = repository;
        this.temporaryPassword = temporaryPassword;
        userPoolId = issuer.getPath().replaceFirst("^/", "");
        String region = userPoolId.substring(0, userPoolId.indexOf('_'));
        this.client = CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build();
    }

    /**
     * Monta o nome completo do dono do access token juntando os atributos padrao
     * given_name (nome) e family_name (sobrenome). Faz uma unica chamada GetUser.
     * Devolve null se nenhum dos dois atributos existir.
     */
    public String getName(String accessToken) {
        GetUserResponse response = fetchUser(accessToken);
        // junta "nome sobrenome" ignorando as partes ausentes/vazias
        String fullName = Stream.of(extract(response, "given_name"), extract(response, "family_name"))
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
        return fullName.isBlank() ? null : fullName;
    }

    public String getEmail(String acessToken) {
        GetUserResponse response = fetchUser(acessToken);
        return response.userAttributes().stream()
                .filter(a -> a.name().equals("email"))
                .map(AttributeType::value)
                .findFirst()
                .orElse(null);
    }

    /** Devolve o valor de um atributo qualquer do dono do access token (ou null). */
    public String getAttribute(String accessToken, String attributeName) {
        return extract(fetchUser(accessToken), attributeName);
    }

    /** Chama o GetUser tratando os erros do Cognito e devolve a resposta. */
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

    /** Extrai o valor de um atributo da resposta do GetUser (null se nao existir). */
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

    public UserResponseMessage createUser(UserRequest dto){

        UserEntity user = userMapper.toEntity(dto);
        String email = dto.email().strip();

        List<AttributeType> attributes = new ArrayList<>();
        attributes.add(attr("given_name", dto.givenName().strip()));
        attributes.add(attr("family_name", dto.familyName().strip()));
        attributes.add(attr("email", email));
        attributes.add(attr("email_verified", "true"));

        // phone_number é opcional; só é enviado ao Cognito quando informado.
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
        return new UserResponseMessage("Conta criada com sucesso",dto.givenName());
    }

    private static AttributeType attr(String name, String value){
        return AttributeType.builder()
                .name(name)
                .value(value)
                .build();
    }

    /**
     * Normaliza um telefone brasileiro para o formato E.164 exigido pelo Cognito
     * (ex.: +5511999998888). Retorna null quando o telefone não foi informado.
     */
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

    public List<UserResponse> listUsers(){

        String paginationToken = null;
        List<String> att = List.of("given_name","email","family_name");
        List<UserResponse> allUsers = new ArrayList<>();

        do{
            ListUsersRequest usersRequest = ListUsersRequest.builder()
                    .userPoolId(userPoolId)
                    .attributesToGet(att)
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
                            u.userStatusAsString()
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

    public void deleteUser(DeleteUserRequest dto){
        AdminDeleteUserRequest userRequest = AdminDeleteUserRequest.builder()
                .userPoolId(userPoolId)
                .username(dto.email())
                .build();

        this.client.adminDeleteUser(userRequest);
        this.userRepository.deleteByEmail(dto.email());
    }

    public UserResponseMessage updateUserAtt(UserRequest dto){

        List<AttributeType> attributeTypes = new ArrayList<>();

        AdminGetUserRequest getUserRequest = AdminGetUserRequest.builder()
                .userPoolId(userPoolId)
                .username(dto.email())
                .build();

        AdminGetUserResponse user = this.client.adminGetUser(getUserRequest);

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

        this.client.adminUpdateUserAttributes(updateUserAttributesRequest);

        return new UserResponseMessage("Usuario atualizado com sucesso", dto.givenName());
    }

    private Boolean isEqualAtt(AdminGetUserResponse user, String name , String value){
        return (user.userAttributes().stream()
                .filter(a -> a.name().equals(name))
                .findFirst()
                .map(v -> v.value().equals(value))
                .orElse(false));
    }
    
}
