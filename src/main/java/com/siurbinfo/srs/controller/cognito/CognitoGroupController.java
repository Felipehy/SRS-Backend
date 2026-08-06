package com.siurbinfo.srs.controller.cognito;

import com.siurbinfo.srs.dto.cognito.group.*;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.service.cognito.CognitoGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class CognitoGroupController {

    private final CognitoGroupService serviceGroup;

    @GetMapping("/list-group")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<List<GroupResponse>> listGroup(){
        return ResponseEntity.ok().body(serviceGroup.listGroup());
    }

    @PostMapping("/user/groups")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<List<GroupResponse>> listGroupsForUser(@RequestBody UserRequest dto){
        return ResponseEntity.ok().body(serviceGroup.listGroupsForUser(dto));
    }

    @PostMapping("/groups/users")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<ListUsersInGroupResponseDTO> listUsersInGroup(@RequestBody GroupRequest dto){
        return ResponseEntity.ok().body(serviceGroup.listUsersInGroup(dto));
    }

    @PostMapping("/group")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<GroupResponse> createGroup(@Valid @RequestBody GroupRequest dto){
        return ResponseEntity.ok().body(serviceGroup.createGroup(dto));
    }

    @PostMapping("/user/add-group")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<UserResponseMessage> putUserInGroups(@RequestBody UserGroupsRequest dto){
        return ResponseEntity.ok().body(serviceGroup.addUserToGroups(dto));
    }

    @PostMapping("/user/remove-group")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<UserResponseMessage> removeUserFromGroups(@RequestBody UserGroupsRequest dto){
        return ResponseEntity.ok().body(serviceGroup.removeUserFromGroups(dto));
    }

    @DeleteMapping("/group")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<GroupResponseMessage> deleteGroup(@RequestBody GroupRequest dto) {
        return ResponseEntity.ok().body(serviceGroup.deleteGroup(dto));
    }

    @PatchMapping("/group")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<GroupResponseMessage> updateGroup(@RequestBody UpdateGroupRequestDTO dto){
        return ResponseEntity.ok().body(serviceGroup.updateGroup(dto));
    }
}
