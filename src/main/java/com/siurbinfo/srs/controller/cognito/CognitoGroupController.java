package com.siurbinfo.srs.controller.cognito;

import com.siurbinfo.srs.dto.cognito.group.*;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.service.cognito.group.CognitoGroupService;
import com.siurbinfo.srs.service.log.LogSystemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class CognitoGroupController {

    private final CognitoGroupService serviceGroup;
    private final LogSystemService log;

    @GetMapping("/list-group")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<List<GroupResponse>> listGroup(){
        return ResponseEntity.ok().body(serviceGroup.listGroup());
    }

    @PostMapping("/user/groups")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<List<GroupResponse>> listGroupsForUser(@RequestBody UserRequest dto){
        return ResponseEntity.ok().body(serviceGroup.listGroupsForUser(dto.email()));
    }

    @PostMapping("/groups/users")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<ListUsersInGroupResponseDTO> listUsersInGroup(@RequestBody GroupRequest dto){
        return ResponseEntity.ok().body(serviceGroup.listUsersInGroup(dto));
    }

    @PostMapping("/group")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupRequest dto, @AuthenticationPrincipal Jwt jwt){
        GroupResponse response = serviceGroup.createGroup(dto);
        log.log("Criação de grupo", "Admin", null, null, LogsType.CREATE, jwt);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/user/add-group")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<UserResponseMessage> putUserInGroups(@RequestBody UserGroupsRequest dto, @AuthenticationPrincipal Jwt jwt){
        UserResponseMessage response = serviceGroup.addUserToGroups(dto);
        log.log("Adição de usuario em grupos", "Admin", null, response.id(), LogsType.UPDATE, jwt);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/user/remove-group")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<UserResponseMessage> removeUserFromGroups(@RequestBody UserGroupsRequest dto, @AuthenticationPrincipal Jwt jwt){
        UserResponseMessage response = serviceGroup.removeUserFromGroups(dto);
        log.log("Remoção de usuario de grupos", "Admin", null, response.id(), LogsType.UPDATE, jwt);
        return ResponseEntity.ok().body(response);
    }

    @DeleteMapping("/group")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<GroupResponseMessage> deleteGroup(@RequestBody GroupRequest dto, @AuthenticationPrincipal Jwt jwt) {
        GroupResponseMessage response = serviceGroup.deleteGroup(dto);
        log.log("Exclusão de grupo", "Admin", null, null, LogsType.DELETE, jwt);
        return ResponseEntity.ok().body(response);
    }

    @PatchMapping("/group")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<GroupResponseMessage> updateGroup(@RequestBody GroupRequest dto, @AuthenticationPrincipal Jwt jwt){
        GroupResponseMessage response = serviceGroup.updateGroup(dto);
        log.log("Alteração no grupo", "Admin", null, null, LogsType.UPDATE, jwt);
        return ResponseEntity.ok().body(response);
    }
}
