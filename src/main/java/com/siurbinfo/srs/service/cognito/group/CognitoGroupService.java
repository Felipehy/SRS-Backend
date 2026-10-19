package com.siurbinfo.srs.service.cognito.group;

import com.siurbinfo.srs.dto.cognito.group.*;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;

import java.util.List;

public interface CognitoGroupService {
    List<GroupResponse> listGroup();

    List<GroupResponse> listGroupsForUser(String email);

    ListUsersInGroupResponseDTO listUsersInGroup(GroupRequest dto);

    GroupResponse createGroup(GroupRequest dto);

    GroupResponseMessage deleteGroup(GroupRequest dto);

    GroupResponseMessage updateGroup(GroupRequest dto);

    UserResponseMessage addUserToGroups(UserGroupsRequest dto);

    UserResponseMessage removeUserFromGroups(UserGroupsRequest dto);
}
