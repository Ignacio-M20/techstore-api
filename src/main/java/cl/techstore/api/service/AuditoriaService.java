package cl.techstore.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuditoriaService {

    @Autowired
    private SqsClient sqsClient;

    @Value("${app.sqs.audit-queue-url}")
    private String queueUrl;

    private final ObjectMapper mapper = new ObjectMapper();

    @Async
    public void publicarEvento(String accion, Long productoId, String nombre) {
        try {
            String usuario = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getName()
                    : "desconocido";

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("accion", accion);
            payload.put("productoId", productoId);
            payload.put("nombre", nombre);
            payload.put("usuario", usuario);
            payload.put("fecha", Instant.now().toString());

            String json = mapper.writeValueAsString(payload);

            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(json)
                    .build());
        } catch (Exception e) {
            // No debe romper el flujo principal si falla la auditoría
            System.err.println("Error publicando evento de auditoría: " + e.getMessage());
        }
    }
}