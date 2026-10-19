package com.siurbinfo.srs.service.cognito.group;

import com.siurbinfo.srs.dto.cognito.group.*;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.entity.group.GroupEntity;
import com.siurbinfo.srs.exception.EmptyRequestException;
import com.siurbinfo.srs.exception.GroupNotFoundedException;
import com.siurbinfo.srs.mapper.group.GroupMapper;
import com.siurbinfo.srs.repository.group.GroupRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Versao do CognitoGroupService para o perfil "dev".
 *
 * Os grupos vem de tbl_groups, exatamente como no CognitoGroupServiceImpl — o
 * banco do dev e o mesmo em estrutura, entao nao ha motivo para devolver grupo
 * fake. O que nao existe fora de producao e o Cognito: a associacao
 * usuario<->grupo so mora la (nao ha tabela de vinculo no banco), entao ela e
 * simulada pelo usuario fake do application-dev.yaml e as operacoes que
 * alterariam essa associacao continuam indisponiveis.
 */
@Service
@Profile("dev")
public class DevCognitoGroupService implements CognitoGroupService {

    private final GroupRepository groupRepository;
    private final GroupMapper groupMapper;
    private final List<String> groups;
    private final String groupName;
    private final String colorGroup;
    private final String description;
    private final String givenName;
    private final String familyName;

    public DevCognitoGroupService(GroupRepository groupRepository,
                                  GroupMapper groupMapper,
                                  @Value("${app.fake-user.groupCognito}") List<String> groupCognito,
                                  @Value("${app.fake-user.groupName}") String groupName,
                                  @Value("${app.fake-user.colorGroup}") String colorGroup,
                                  @Value("${app.fake-user.description}") String description,
                                  @Value("${app.fake-user.given-name}") String givenName,
                                  @Value("${app.fake-user.family-name}") String familyName) {
        this.groupRepository = groupRepository;
        this.groupMapper = groupMapper;
        this.groups = groupCognito;
        this.groupName = groupName;
        this.colorGroup = colorGroup;
        this.description = description;
        this.givenName = givenName;
        this.familyName = familyName;
    }

    @Override
    public List<GroupResponse> listGroup() {
        List<Map<String, String>> rows = groupRepository.getGroups();
        return rows.stream()
                .map(a -> new GroupResponse(a.get("groupName"), a.get("groupCognito"), a.get("colorGroup"), a.get("description")))
                .toList();
    }

    @Override
    public List<GroupResponse> listGroupsForUser(String email) {
        // sem Cognito nao da para saber a que grupos o usuario pertence; no dev a
        // associacao vem do app.fake-user.groupCognito e os dados de exibicao do banco
        return groups.stream()
                .map(group -> {
                    GroupEntity data = groupRepository.getGroupByGroupCognito(group);
                    return data == null
                            ? new GroupResponse(groupName, group, colorGroup, description)
                            : new GroupResponse(data.getGroupName(), data.getGroupCognito(), data.getColorGroup(), data.getDescription());
                })
                .toList();
    }

    @Override
    public ListUsersInGroupResponseDTO listUsersInGroup(GroupRequest dto) {
        // idem: o unico membro possivel no dev e o proprio usuario fake
        List<Map<String, String>> users = groups.contains(dto.groupCognito())
                ? List.of(Map.of("name", givenName, "familyName", familyName))
                : List.of();
        return new ListUsersInGroupResponseDTO(users, dto.groupName());
    }

    @Override
    public GroupResponse createGroup(GroupRequest dto) {

        GroupEntity entity = groupMapper.toEntity(dto);

        groupRepository.save(entity);

        return new GroupResponse(entity.getGroupName(), entity.getGroupCognito(), entity.getColorGroup(), entity.getDescription());
    }

    @Override
    public GroupResponseMessage deleteGroup(GroupRequest dto) {

        if (dto.groupCognito() == null || dto.groupCognito().isBlank()) {
            throw new EmptyRequestException("è necessario o nome do grupo do cognito");
        }

        groupRepository.deleteByGroupCognito(dto.groupCognito());

        return new GroupResponseMessage(dto.groupCognito(), "Grupo deletado com sucesso!");
    }

    @Override
    public GroupResponseMessage updateGroup(GroupRequest dto) {

        if (dto.groupCognito() == null || dto.groupCognito().isBlank()) {
            throw new EmptyRequestException("É necessário o nome do grupo do cognito");
        }

        // no prod o grupo tambem existe no Cognito e é de la que vem a descricao
        // antiga quando o dto nao traz uma nova; aqui a referencia é a linha do banco
        GroupEntity entity = groupRepository.getGroupByGroupCognito(dto.groupCognito());

        if (entity == null) {
            throw new GroupNotFoundedException("O grupo " + dto.groupCognito() + " não existe no banco");
        }

        groupMapper.updateGroup(dto, entity);
        groupRepository.save(entity);

        return new GroupResponseMessage(dto.groupName(), "O grupo foi atualizado");
    }

    @Override
    public UserResponseMessage addUserToGroups(UserGroupsRequest dto) {
        // o vinculo usuario<->grupo so existe no Cognito, nao ha onde persistir isso no banco
        throw new UnsupportedOperationException("Gestao de grupos do Cognito indisponivel no perfil dev");
    }

    @Override
    public UserResponseMessage removeUserFromGroups(UserGroupsRequest dto) {
        throw new UnsupportedOperationException("Gestao de grupos do Cognito indisponivel no perfil dev");
    }
}
