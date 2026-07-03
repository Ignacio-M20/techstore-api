package cl.techstore.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO para la respuesta del login exitoso.
 * Devuelve el token JWT generado.
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String tipo;
    private String expiracion;
}
