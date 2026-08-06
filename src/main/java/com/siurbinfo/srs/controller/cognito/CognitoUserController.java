package com.siurbinfo.srs.controller.cognito;

import com.siurbinfo.srs.dto.cognito.user.DeleteUserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserRequest;
import com.siurbinfo.srs.dto.cognito.user.UserResponse;
import com.siurbinfo.srs.dto.cognito.user.UserResponseMessage;
import com.siurbinfo.srs.service.cognito.CognitoUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dados do usuario autenticado (o "eu"). Qualquer usuario autenticado acessa o
 * seu proprio /api/me — sem restricao de grupo.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CognitoUserController {

    // Servico que consulta os atributos do usuario no Cognito.
    private final CognitoUserService userService;

    /**
     * GET /api/me -> devolve { "name": "<nome do usuario logado>" }.
     * O Spring injeta o token JWT ja validado do usuario autenticado.
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, String>> me(@AuthenticationPrincipal Jwt jwt) {
        // Sem token valido nao ha usuario autenticado -> 401.
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        // GetUser e autorizado pelo proprio access token; getTokenValue() e o JWT bruto (string).
        String name = userService.getName(jwt.getTokenValue());
        // Se o Cognito nao tiver o atributo "name", devolve string vazia em vez de null.
        return ResponseEntity.ok(Map.of("name", name == null ? "" : name));
    }

    @GetMapping("/admin/list-users")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<List<UserResponse>> listUsersCognito(){
        return ResponseEntity.ok().body(userService.listUsers());
    }

    @PostMapping("/admin/user")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<UserResponseMessage> createUser(@Valid @RequestBody UserRequest dto){
        return ResponseEntity.ok().body(userService.createUser(dto));
    }

    @DeleteMapping("/admin/user")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<Void> deleteUser(@Valid @RequestBody DeleteUserRequest dto){
        userService.deleteUser(dto);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin/user")
    @PreAuthorize("hasAnyRole('siurb-administrador')")
    public ResponseEntity<UserResponseMessage> updateUserAtt(@Valid @RequestBody UserRequest dto){
        return ResponseEntity.ok().body(userService.updateUserAtt(dto));
    }

}
