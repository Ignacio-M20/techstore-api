package cl.techstore.api.service;

import cl.techstore.api.dto.ProductoDTO;
import cl.techstore.api.model.Producto;
import cl.techstore.api.repository.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private AuditoriaService auditoriaService;

    public List<Producto> listarTodos() {
        return productoRepository.findByActivoTrue();
    }

    public Producto crear(ProductoDTO dto) {
        Producto producto = new Producto();
        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setPrecio(dto.getPrecio());
        producto.setStock(dto.getStock());
        producto.setCategoria(dto.getCategoria());
        producto.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        
        Producto guardado = productoRepository.save(producto);
        
        // Disparar auditoría asíncrona SQS
        auditoriaService.publicarEvento("CREAR", guardado.getId(), guardado.getNombre());
        
        return guardado;
    }

    public Producto modificar(Long id, ProductoDTO dto) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));

        producto.setNombre(dto.getNombre());
        producto.setDescripcion(dto.getDescripcion());
        producto.setPrecio(dto.getPrecio());
        producto.setStock(dto.getStock());
        producto.setCategoria(dto.getCategoria());
        if (dto.getActivo() != null) {
            producto.setActivo(dto.getActivo());
        }

        Producto actualizado = productoRepository.save(producto);
        
        // Disparar auditoría asíncrona SQS
        auditoriaService.publicarEvento("MODIFICAR", actualizado.getId(), actualizado.getNombre());
        
        return actualizado;
    }

    public void eliminar(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID: " + id));
        producto.setActivo(false);
        productoRepository.save(producto);
        
        // Disparar auditoría asíncrona SQS (Borrado Lógico)
        auditoriaService.publicarEvento("ELIMINAR", producto.getId(), producto.getNombre());
    }
}