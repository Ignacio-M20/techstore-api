package cl.techstore.api.controller;

import cl.techstore.api.dto.LoginRequest;
import cl.techstore.api.dto.LoginResponse;
import cl.techstore.api.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación.
 * Expone el endpoint POST /auth/login para obtener un token JWT.
 *
 * Las credenciales válidas están definidas en application.properties
 * (app.auth.username y app.auth.password), NO en la base de datos.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    // Credenciales leídas desde application.properties (no desde BD)
    @Value("${app.auth.username}")
    private String validUsername;

    @Value("${app.auth.password}")
    private String validPassword;

    @Value("${app.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Endpoint de login: valida las credenciales y devuelve un token JWT.
     *
     * POST /auth/login
     * Body: { "username": "admin@techstore.cl", "password": "Admin1234" }
     *
     * @param loginRequest credenciales del usuario
     * @return token JWT si las credenciales son correctas, 401 si no lo son
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (validUsername.equals(loginRequest.getUsername())
                && validPassword.equals(loginRequest.getPassword())) {

            String token = jwtUtil.generateToken(loginRequest.getUsername());
            long expirationSeconds = jwtExpiration / 1000;

            return ResponseEntity.ok(
                    new LoginResponse(token, "Bearer", String.valueOf(expirationSeconds))
            );
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Credenciales inválidas. Verifique su usuario y contraseña.");
    }
}
