package com.siurbinfo.srs.dto.user;

import com.siurbinfo.srs.dto.cognito.group.GroupResponse;

import java.util.List;
import java.util.Map;

public record UserResponseDTO(

        Long id,
        String email,
        String givenName,
        String familyName,
        Map<String,String> colors,
        Boolean isHorizontalNavbar,
        List<GroupResponse> groups,
        Long idDepartment,
        Boolean isFirstLogin

) {
}
