package com.siurbinfo.srs.service.cognito.group;

import com.siurbinfo.srs.dto.cognito.group.*;
import com.siurbinfo.srs.dto.cognito.group.ListUsersInGroupResponseDTO;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.entity.group.GroupEntity;
import com.siurbinfo.srs.exception.EmptyRequestException;
import com.siurbinfo.srs.mapper.group.GroupMapper;
import com.siurbinfo.srs.repository.group.GroupRepository;
import com.siurbinfo.srs.repository.user.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@Profile("prod")
public class CognitoGroupServiceImpl implements CognitoGroupService {

    CognitoIdentityProviderClient client = null;
    String userPoolId = null;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMapper groupMapper;

    public CognitoGroupServiceImpl(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
                                   UserRepository userRepository,
                                   GroupRepository groupRepository,
                                   GroupMapper groupMapper){
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.groupMapper = groupMapper;
        URI issuer = URI.create(issuerUri);
        userPoolId = issuer.getPath().replaceFirst("^/", "");
        String region = userPoolId.substring(0, userPoolId.indexOf('_'));
        this.client = CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public List<GroupResponse> listGroup(){
        List<Map<String,String>> groups = groupRepository.getGroups();
        return groups.stream()
                .map(a -> new GroupResponse(a.get("groupName"),a.get("groupCognito"),a.get("colorGroup"),a.get("description")))
                .toList();
    }

    @Override
    public List<GroupResponse> listGroupsForUser(String email){

        AdminListGroupsForUserRequest adminListGroupsForUserRequest = AdminListGroupsForUserRequest.builder()
                .userPoolId(userPoolId)
                .username(email)
                .limit(20)
                .build();

        AdminListGroupsForUserResponse adminListGroupsForUserResponse = this.client.adminListGroupsForUser(adminListGroupsForUserRequest);

        return adminListGroupsForUserResponse.groups().stream()
                .map(groupType -> {
                    GroupEntity data = groupRepository.getGroupByGroupCognito(groupType.groupName());
                    return new GroupResponse(data.getGroupName(), data.getGroupCognito(), data.getColorGroup(), data.getDescription());
                })
                .toList();
    }

    @Override
    public ListUsersInGroupResponseDTO listUsersInGroup(GroupRequest dto){

        String paginationToken = null;
        List<Map<String,String>> allUsers = new ArrayList<>();

        do {
            ListUsersInGroupRequest listUsersInGroupRequest = ListUsersInGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .groupName(dto.groupCognito())
                    .limit(60)
                    .nextToken(paginationToken)
                    .build();

            ListUsersInGroupResponse listUsersInGroupResponse = this.client.listUsersInGroup(listUsersInGroupRequest);

            allUsers.addAll(getUserFromGroup(listUsersInGroupResponse));

            paginationToken = listUsersInGroupResponse.nextToken();
        } while (paginationToken != null);
        return new ListUsersInGroupResponseDTO(allUsers, dto.groupName());
    }

    private List<Map<String,String>> getUserFromGroup(ListUsersInGroupResponse listUsers){

        List<Map<String,String>> allUsers = new ArrayList<>();

        for (UserType user : listUsers.users()){

            Map<String,String> map = new HashMap<>();

            user.attributes().stream()
                            .filter(a -> a.name().equals("email"))
                            .map(AttributeType::value)
                            .findFirst()
                            .ifPresent(u -> map.put("username",u));

            user.attributes().stream()
                    .filter(a -> a.name().equals("given_name"))
                    .map(AttributeType::value)
                    .findFirst()
                    .ifPresent(v -> map.put("name", v));

            user.attributes().stream()
                    .filter(a -> a.name().equals("family_name"))
                    .map(AttributeType::value)
                    .findFirst()
                    .ifPresent(v -> map.put("familyName", v));

            allUsers.add(map);

        }
        return allUsers;
    }

    @Override
    public GroupResponse createGroup(GroupRequest dto) {

        GroupEntity entity = groupMapper.toEntity(dto);

        CreateGroupRequest groupRequest = CreateGroupRequest.builder()
                .groupName(dto.groupCognito())
                .description(dto.description())
                .userPoolId(userPoolId)
                .build();

        this.client.createGroup(groupRequest);
        groupRepository.save(entity);

        return new GroupResponse(entity.getGroupName(), entity.getGroupCognito(), entity.getColorGroup(), entity.getDescription());
    }

    @Override
    public GroupResponseMessage deleteGroup(GroupRequest dto){

        if (dto.groupCognito() == null || dto.groupCognito().isBlank()){
            throw new EmptyRequestException("è necessario o nome do grupo do cognito");
        }

        DeleteGroupRequest deleteGroupRequest = DeleteGroupRequest.builder()
                .userPoolId(userPoolId)
                .groupName(dto.groupCognito())
                .build();

        this.client.deleteGroup(deleteGroupRequest);
        groupRepository.deleteByGroupCognito(dto.groupCognito());

        return new GroupResponseMessage(deleteGroupRequest.groupName(), "Grupo deletado com sucesso!");
    }

    @Override
    public GroupResponseMessage updateGroup(GroupRequest dto) {

        if (dto.groupCognito() == null || dto.groupCognito().isBlank()){
            throw new EmptyRequestException("É necessário o nome do grupo do cognito");
        }

       // no Cognito o grupo so existe pelo groupCognito; groupName e rotulo de exibicao
       GetGroupRequest getGroupRequest = GetGroupRequest.builder()
               .userPoolId(userPoolId)
               .groupName(dto.groupCognito())
               .build();

       GetGroupResponse getGroupResponse = this.client.getGroup(getGroupRequest);

       String description = (dto.description() == null || dto.description().isBlank())
               ? getGroupResponse.group().description()
               : dto.description();

       UpdateGroupRequest updateGroupRequest = UpdateGroupRequest.builder()
               .userPoolId(userPoolId)
               .groupName(dto.groupCognito())
               .description(description)
               .build();

       GroupEntity entity = groupRepository.getGroupByGroupCognito(dto.groupCognito());
       groupMapper.updateGroup(dto,entity);

       this.client.updateGroup(updateGroupRequest);
       groupRepository.save(entity);

       return new GroupResponseMessage(dto.groupName(), "O grupo foi atualizado");
    }

    @Override
    public UserResponseMessage addUserToGroups(UserGroupsRequest dto){

        AdminAddUserToGroupRequest adminAddUserToGroupRequest = AdminAddUserToGroupRequest.builder()
                .userPoolId(userPoolId)
                .username(dto.username())
                .groupName(dto.group())
                .build();

        this.client.adminAddUserToGroup(adminAddUserToGroupRequest);

        return new UserResponseMessage(userRepository.getIdByEmail(dto.username()), "O usuario foi adicionado ao grupo!", dto.username());

    }

    @Override
    public UserResponseMessage removeUserFromGroups(UserGroupsRequest dto){

        AdminRemoveUserFromGroupRequest adminRemoveUserFromGroupRequest = AdminRemoveUserFromGroupRequest.builder()
                .userPoolId(userPoolId)
                .groupName(dto.group())
                .username(dto.username())
                .build();

        this.client.adminRemoveUserFromGroup(adminRemoveUserFromGroupRequest);

        return new UserResponseMessage(userRepository.getIdByEmail(dto.username()), "O usuario foi removido do grupo", dto.username());
    }

}
