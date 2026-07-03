package cl.techstore.api.controller;

import cl.techstore.api.dto.ProductoDTO;
import cl.techstore.api.model.Producto;
import cl.techstore.api.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador RESTful para la gestión de Productos.
 * Todos los endpoints requieren autenticación JWT (Bearer token).
 *
 * Endpoints disponibles:
 *   GET    /api/productos       → Lista todos los productos activos
 *   POST   /api/productos       → Crea un nuevo producto
 *   PUT    /api/productos/{id}  → Modifica un producto existente
 *   DELETE /api/productos/{id}  → Borrado lógico (activo = false)
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    /**
     * Lista todos los productos activos del catálogo.
     *
     * GET /api/productos
     * Respuesta: 200 OK + lista de productos
     */
    @GetMapping
    public ResponseEntity<List<Producto>> listar() {
        List<Producto> productos = productoService.listarTodos();
        return ResponseEntity.ok(productos);
    }

    /**
     * Crea un nuevo producto en el catálogo.
     *
     * POST /api/productos
     * Body: ProductoDTO (nombre, descripcion, precio, stock, categoria, activo)
     * Respuesta: 201 Created + producto creado
     */
    @PostMapping
    public ResponseEntity<Producto> crear(@RequestBody ProductoDTO dto) {
        Producto nuevo = productoService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    /**
     * Modifica un producto existente por su ID.
     *
     * PUT /api/productos/{id}
     * Body: ProductoDTO con los nuevos datos
     * Respuesta: 200 OK + producto actualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> modificar(@PathVariable Long id,
                                       @RequestBody ProductoDTO dto) {
        try {
            Producto actualizado = productoService.modificar(id, dto);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Realiza el borrado lógico de un producto (cambia activo a false).
     * El registro permanece en la base de datos.
     *
     * DELETE /api/productos/{id}
     * Respuesta: 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            productoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
