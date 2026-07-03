package cl.techstore.api.repository;

import cl.techstore.api.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Producto.
 * Extiende JpaRepository para heredar operaciones CRUD básicas.
 */
@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /**
     * Retorna solo los productos activos (activo = true).
     */
    List<Producto> findByActivoTrue();

    /**
     * Busca productos por categoría (solo activos).
     */
    List<Producto> findByCategoriaAndActivoTrue(String categoria);
}
