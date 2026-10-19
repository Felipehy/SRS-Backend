package com.siurbinfo.srs.service.cognito.user;

import com.siurbinfo.srs.dto.cognito.user.DeleteUserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponse;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.dto.department.DepartmentResponseDTO;
import com.siurbinfo.srs.dto.reservation.ReservationsDTO;
import com.siurbinfo.srs.dto.user.ColorsRequestDTO;
import com.siurbinfo.srs.dto.user.ListReservationsUserDTO;
import com.siurbinfo.srs.dto.user.UserResponseDTO;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface CognitoUserService {

    String getName(String accessToken);

    UserResponseDTO getUser(Jwt jwt);

    String getEmail(String acessToken);

    String getAttribute(String accessToken, String attributeName);

    UserResponseMessage createUser(UserRequest dto);

    List<UserResponse> listUsers();

    UserResponseMessage deleteUser(DeleteUserRequest dto);

    UserResponseMessage updateUserAtt(UserRequest dto);

    UserResponseMessage resetPassword(String email, Jwt jwt);

    ReservationsDTO getReservationsByUser(Jwt jwt, LocalDate date, Long pag, List<String> types);

    void resetMFA(String email);

    @Transactional
    void setColors(Jwt jwt, ColorsRequestDTO dto);

    @Transactional
    void setNavbar(Boolean navbar, Jwt jwt);

    @Transactional
    void setDepartment(Long id, String email);

    @Transactional
    void finishTutorial(Jwt jwt);

    @Transactional(readOnly = true)
    DepartmentResponseDTO getDepartmentFromUser(Long id, Jwt jwt);

}
