package cl.techstore.api.dto;

import lombok.Data;

/**
 * DTO para crear o modificar un Producto.
 * Separa la capa de transporte de la entidad JPA.
 */
@Data
public class ProductoDTO {
    private String nombre;
    private String descripcion;
    private Double precio;
    private Integer stock;
    private String categoria;
    private Boolean activo;
}
