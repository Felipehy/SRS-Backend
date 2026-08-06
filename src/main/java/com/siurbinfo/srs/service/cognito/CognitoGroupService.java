package com.siurbinfo.srs.service.cognito;

import com.siurbinfo.srs.dto.cognito.group.*;
import com.siurbinfo.srs.dto.cognito.group.ListUsersInGroupResponseDTO;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.exception.EmptyRequestException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
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
public class CognitoGroupService {

    CognitoIdentityProviderClient client = null;
    String userPoolId = null;

    public CognitoGroupService (@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri){
        URI issuer = URI.create(issuerUri);
        userPoolId = issuer.getPath().replaceFirst("^/", "");
        String region = userPoolId.substring(0, userPoolId.indexOf('_'));
        this.client = CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build();
    }

    public List<GroupResponse> listGroup(){

        String paginationToken = null;
        List<GroupResponse> allGroups = new ArrayList<>();

        do {

            ListGroupsRequest groupsRequest = ListGroupsRequest.builder()
                    .userPoolId(userPoolId)
                    .limit(10)
                    .nextToken(paginationToken)
                    .build();

            ListGroupsResponse groupsResponse = this.client.listGroups(groupsRequest);

            allGroups.addAll(groupsResponse.groups().stream()
                    .map(g -> new GroupResponse(
                            g.groupName(),
                            g.description()
                    ))
                    .toList());

            paginationToken = groupsResponse.nextToken();

        } while (paginationToken != null);
        return allGroups;
    }

    public List<GroupResponse> listGroupsForUser(UserRequest dto){

        List<GroupResponse> allGroups;

        AdminListGroupsForUserRequest adminListGroupsForUserRequest = AdminListGroupsForUserRequest.builder()
                .userPoolId(userPoolId)
                .username(dto.email())
                .limit(20)
                .build();

        AdminListGroupsForUserResponse adminListGroupsForUserResponse = this.client.adminListGroupsForUser(adminListGroupsForUserRequest);

        allGroups = adminListGroupsForUserResponse.groups().stream()
                .map(g -> new GroupResponse(
                        g.groupName(),
                        g.description()
                ))
                .toList();

        return allGroups;

    }

    public ListUsersInGroupResponseDTO listUsersInGroup(GroupRequest dto){

        String paginationToken = null;
        List<Map<String,String>> allUsers;

        do {
            ListUsersInGroupRequest listUsersInGroupRequest = ListUsersInGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .groupName(dto.groupName())
                    .limit(60)
                    .nextToken(paginationToken)
                    .build();

            ListUsersInGroupResponse listUsersInGroupResponse = this.client.listUsersInGroup(listUsersInGroupRequest);

            allUsers = getUserFromGroup(listUsersInGroupResponse);

            paginationToken = listUsersInGroupResponse.nextToken();
        } while (paginationToken != null);
        return new ListUsersInGroupResponseDTO(allUsers, dto.groupName());
    }

    private List<Map<String,String>> getUserFromGroup(ListUsersInGroupResponse listUsers){

        List<Map<String,String>> allUsers = new ArrayList<>();

        for (UserType user : listUsers.users()){

            Map<String,String> map = new HashMap<>();

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

    public GroupResponse createGroup(GroupRequest dto) {

        CreateGroupRequest groupRequest = CreateGroupRequest.builder()
                .groupName(dto.groupName())
                .description(dto.description())
                .userPoolId(userPoolId)
                .build();

        CreateGroupResponse groupResponse = this.client.createGroup(groupRequest);

        return new GroupResponse(groupResponse.group().groupName(),groupResponse.group().description());

    }

    public GroupResponseMessage deleteGroup(GroupRequest dto){

        if (dto.groupName().isBlank()){throw new EmptyRequestException("è necessario o nome do grupo");}

        DeleteGroupRequest deleteGroupRequest = DeleteGroupRequest.builder()
                .userPoolId(userPoolId)
                .groupName(dto.groupName())
                .build();

        this.client.deleteGroup(deleteGroupRequest);

        return new GroupResponseMessage(deleteGroupRequest.groupName(), "Grupo deletado com sucesso!");
    }

    public GroupResponseMessage updateGroup(UpdateGroupRequestDTO dto) {

        Map<String,String> newGroup = new HashMap<>();

        newGroup.put("groupName", dto.groupName());

        if (dto.groupName().isBlank()){throw new EmptyRequestException("É necessário o nome do grupo");}

       GetGroupRequest getGroupRequest = GetGroupRequest.builder()
               .userPoolId(userPoolId)
               .groupName(dto.groupName())
               .build();

       GetGroupResponse getGroupResponse = this.client.getGroup(getGroupRequest);

       if (getGroupResponse.group().description().equals(dto.description())){
           newGroup.put("description",getGroupResponse.group().description());
       } else {
           newGroup.put("description", dto.description());
       }

       UpdateGroupRequest updateGroupRequest = UpdateGroupRequest.builder()
               .userPoolId(userPoolId)
               .groupName(newGroup.get("groupName"))
               .description(newGroup.get("description"))
               .build();

       this.client.updateGroup(updateGroupRequest);

       return new GroupResponseMessage(dto.groupName(), "O grupo foi atualizado");
    }

    public UserResponseMessage addUserToGroups(UserGroupsRequest dto){

        AdminAddUserToGroupRequest adminAddUserToGroupRequest = AdminAddUserToGroupRequest.builder()
                .userPoolId(userPoolId)
                .username(dto.username())
                .groupName(dto.group())
                .build();

        this.client.adminAddUserToGroup(adminAddUserToGroupRequest);

        return new UserResponseMessage("O usuario foi adicionado ao grupo!", dto.username());

    }

    public UserResponseMessage removeUserFromGroups(UserGroupsRequest dto){

        AdminRemoveUserFromGroupRequest adminRemoveUserFromGroupRequest = AdminRemoveUserFromGroupRequest.builder()
                .userPoolId(userPoolId)
                .groupName(dto.group())
                .username(dto.username())
                .build();

        this.client.adminRemoveUserFromGroup(adminRemoveUserFromGroupRequest);

        return new UserResponseMessage("O usuario foi removido do grupo", dto.username());
    }

}
