package com.siurbinfo.srs.controller.cognito;

import com.siurbinfo.srs.dto.cognito.user.DeleteUserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponse;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.dto.reservation.ReservationsDTO;
import com.siurbinfo.srs.dto.user.ColorsRequestDTO;
import com.siurbinfo.srs.dto.user.ListReservationsUserDTO;
import com.siurbinfo.srs.dto.user.UserResponseDTO;
import com.siurbinfo.srs.enums.LogsType;
import com.siurbinfo.srs.service.cognito.user.CognitoUserService;
import com.siurbinfo.srs.service.log.LogSystemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CognitoUserController {

    private final CognitoUserService userService;
    private final LogSystemService logSystem;

    @GetMapping("/user")
    public ResponseEntity<UserResponseDTO> me(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getUser(jwt));
    }

    @PostMapping("/user/color")
    public ResponseEntity<Void> setColors(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ColorsRequestDTO dto) {
        userService.setColors(jwt,dto);
        logSystem.log("Mudança de cores das reservas", "Configurações", null, null, LogsType.UPDATE, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user")
    public ResponseEntity<Void> setNavbar(@AuthenticationPrincipal Jwt jwt, @RequestParam("navbar") Boolean navbar){
        userService.setNavbar(navbar,jwt);
        logSystem.log("Mudança da barra de navegação", "Configurações", null, null, LogsType.UPDATE, jwt);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/reservations")
    public ResponseEntity<ReservationsDTO> getReservations(@AuthenticationPrincipal Jwt jwt,
                                                           @RequestParam(value = "pagination", required = true) @PositiveOrZero(message = "A página não pode ser negativa") Long pag,
                                                           @RequestParam(value = "date", required = false) LocalDate date,
                                                           @RequestParam(value = "type", required = false) List<String> types){
        return ResponseEntity.ok(userService.getReservationsByUser(jwt,date,pag,types));
    }

    @PatchMapping("/user/tutorial")
    public ResponseEntity<Void> finishTutorial(@AuthenticationPrincipal Jwt jwt){
        userService.finishTutorial(jwt);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/list-users")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<List<UserResponse>> listUsersCognito(){
        return ResponseEntity.ok().body(userService.listUsers());
    }

    @PostMapping("/admin/user")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<UserResponseMessage> createUser(@Valid @RequestBody UserRequest dto, @AuthenticationPrincipal Jwt jwt){
        UserResponseMessage response = userService.createUser(dto);
        logSystem.log("Criação de usuario", "Admin", null, response.id(), LogsType.CREATE, jwt);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/admin/reset-password")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<Void> resetPassword(@RequestParam("email") String email, @AuthenticationPrincipal Jwt jwt){
        UserResponseMessage response = userService.resetPassword(email, jwt);
        logSystem.log("Reset de senha do usuario " + email, "Admin", null, response.id(), LogsType.UPDATE, jwt);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/user")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<Void> deleteUser(@Valid @RequestBody DeleteUserRequest dto, @AuthenticationPrincipal Jwt jwt){
        UserResponseMessage response = userService.deleteUser(dto);
        logSystem.log("Usuario deletado, email: " + dto.email(), "Admin", null, response.id(), LogsType.DELETE, jwt);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/user")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<UserResponseMessage> updateUserAtt(@Valid @RequestBody UserRequest dto, @AuthenticationPrincipal Jwt jwt){
        UserResponseMessage response = userService.updateUserAtt(dto);
        logSystem.log("Alteração no usuario", "Admin", null, response.id(), LogsType.UPDATE, jwt);
        return ResponseEntity.ok().body(response);
    }

    @PostMapping("/admin/department")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<Void> setDepartment(@AuthenticationPrincipal Jwt jwt, @RequestParam("id") Long id, @RequestParam("email") String email){
        if (jwt == null) {
            throw new SecurityException("A chave de acesso está vazia");
        }
        userService.setDepartment(id,email);
        logSystem.log("Alteração no departamento", "Admin", null, null, LogsType.UPDATE, jwt);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/admin/reset-mfa")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<Void> resetMFA(@AuthenticationPrincipal Jwt jwt, @RequestParam("email") String email){
        userService.resetMFA(email);
        logSystem.log("Reset do MFA do " + email, "Admin", null, null, LogsType.UPDATE, jwt);
        return ResponseEntity.ok().build();
    }

}
