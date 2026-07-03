package cl.techstore.api.dto;

import lombok.Data;

/**
 * DTO para la petición de login.
 * Recibe las credenciales del usuario.
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
}
