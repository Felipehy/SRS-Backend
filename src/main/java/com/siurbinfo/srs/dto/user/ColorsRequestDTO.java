package com.siurbinfo.srs.dto.user;

import org.hibernate.validator.constraints.Length;

public record ColorsRequestDTO (

        @Length(max = 7, message = "Apenas valores hexadecimais são permitidos")
        String colorAuditorium,
        @Length(max = 7, message = "Apenas valores hexadecimais são permitidos")
        String colorFleetVehicles,
        @Length(max = 7, message = "Apenas valores hexadecimais são permitidos")
        String colorMeetingRoom

){}
