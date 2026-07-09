package cl.techstore.api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de Spring Security.
 * Define qué endpoints son públicos y cuáles requieren autenticación JWT.
 * La sesión es stateless: no se crean sesiones HTTP en el servidor.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Desactivar CSRF (no necesario con JWT stateless)
            .csrf(AbstractHttpConfigurer::disable)

            // Configurar política de sesión: STATELESS (sin sesiones)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Reglas de autorización
           .authorizeHttpRequests(auth -> auth
                // Login es público: aquí se genera el token, no puede exigir uno
                .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                // Catálogo de solo lectura es público (consulta de productos)
                .requestMatchers(HttpMethod.GET, "/api/productos/**").permitAll()
                // Escritura sobre productos exige JWT válido (así se sabe quién audita)
                .requestMatchers(HttpMethod.POST, "/api/productos/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/productos/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/productos/**").authenticated()
                .anyRequest().authenticated()
            )

            // Agregar el filtro JWT antes del filtro de autenticación estándar
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
