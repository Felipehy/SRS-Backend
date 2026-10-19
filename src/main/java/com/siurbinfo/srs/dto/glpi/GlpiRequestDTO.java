package com.siurbinfo.srs.dto.glpi;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GlpiRequestDTO(
        String        name,
        String        content,
        Long       type,
        Long       urgency,
        Long       impact,
        Long       priority,
        Long       status,

        @JsonProperty("entities_id")         Long       entitiesId,
        @JsonProperty("itilcategories_id")   Long       itilcategoriesId,
        @JsonProperty("locations_id")        Long       locationsId,
        @JsonProperty("requesttypes_id")     Long       requesttypesId,

        String        date,

        @JsonProperty("resolution_date")     String        resolutionDate,

        @JsonProperty("_users_id_requester")  Long       usersIdRequester,
        @JsonProperty("_users_id_assign")     Long       usersIdAssign,
        @JsonProperty("_groups_id_assign")     Long       groupsIdAssign,

        @JsonProperty("_users_id_requester_notif") UserNotifDTO usersIdRequesterNotif,
        @JsonProperty("_users_id_assign_notif")    UserNotifDTO usersIdAssignNotif
) {
}
