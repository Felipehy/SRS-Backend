package com.siurbinfo.srs.dto.cognito.group;

import java.util.List;
import java.util.Map;

public record ListUsersInGroupResponseDTO(
        List<Map<String,String>> username,
        String group
) {}
