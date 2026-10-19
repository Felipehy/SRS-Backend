package com.siurbinfo.srs.service.reservation;

import com.siurbinfo.srs.dto.reservation.ReservationsDTO;
import com.siurbinfo.srs.dto.user.ListReservationsUserDTO;
import com.siurbinfo.srs.exception.InvalidTimeException;
import com.siurbinfo.srs.repository.reservation.ReservationRepository;
import com.siurbinfo.srs.repository.user.ReservationsProjection;
import com.siurbinfo.srs.repository.user.UserRepository;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository repository;
    private final CognitoUserService userService;
    private final UserRepository userRepository;

    public ReservationsDTO getAllReservations(LocalDate startTime, LocalDate endTime, Jwt jwt){
        if (jwt == null){
            throw new SecurityException("Is necessary a json web token(jwt)");
        }
        if (startTime == null) {
            throw new InvalidTimeException("É necessário o tempo inicial");
        }
        Long id = userRepository.getIdByEmail(userService.getEmail(jwt.getTokenValue()));
        List<ReservationsProjection> reservations = repository.getAllReservations(id,startTime,endTime);
        Long numReservations = reservations.isEmpty() ? 0L: (long) reservations.getFirst().totalReservations();
        List<ListReservationsUserDTO> reservationsDTOs = reservations.stream()
                .map(r -> new ListReservationsUserDTO(r.id(),
                        r.reservationType(), r.status(), r.reservationDate(), r.startTime(), r.endTime(), r.userNameRequester(), r.idUser()))
                .toList();
        return new ReservationsDTO(numReservations,reservationsDTOs);
    }
}
