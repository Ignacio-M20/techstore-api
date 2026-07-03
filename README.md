# TechStore Chile — Microservicio de Gestión de Productos

Microservicio RESTful desarrollado con **Java 17 + Spring Boot 3.2**, que permite administrar el catálogo de productos de TechStore Chile. Incluye autenticación JWT, persistencia en PostgreSQL y empaquetado como `.JAR` ejecutable con Maven.

---

## Estructura del proyecto (arquitectura en capas)

```
src/main/java/cl/techstore/api/
│
├── controller/
│   ├── AuthController.java        ← POST /auth/login
│   └── ProductoController.java    ← GET, POST, PUT, DELETE /api/productos
│
├── service/
│   └── ProductoService.java       ← Lógica de negocio del CRUD
│
├── repository/
│   └── ProductoRepository.java    ← Extiende JpaRepository
│
├── model/
│   └── Producto.java              ← Entidad JPA (@Entity)
│
├── security/
│   ├── JwtUtil.java               ← Generación y validación de tokens
│   ├── JwtFilter.java             ← Filtro que intercepta cada petición
│   └── SecurityConfig.java        ← Configuración de Spring Security
│
└── dto/
    ├── LoginRequest.java           ← { username, password }
    ├── LoginResponse.java          ← { token, tipo, expiracion }
    └── ProductoDTO.java            ← Objeto de transferencia de datos
```

---

## Requisitos previos

| Herramienta | Versión mínima |
|---|---|
| Java (JDK) | 17 |
| Maven | 3.9+ |
| Docker Desktop | Cualquier versión actual |
| Postman | Cualquier versión |

---

## Clonar el repositorio

```bash
git clone https://github.com/<tu-usuario>/techstore-api.git
cd techstore-api
```

---

## Opción A — Ejecutar con Docker Compose (recomendado)

Levanta PostgreSQL y el microservicio con un solo comando:

```bash
# 1. Generar el .JAR
mvn clean package -DskipTests

# 2. Levantar todo el entorno
docker compose up --build
```

La API quedará disponible en: `http://localhost:8080`

Para detener el entorno:
```bash
docker compose down
```

---

## Opción B — Ejecutar localmente (sin Docker Compose)

### 1. Levantar PostgreSQL con Docker

```bash
docker run --name techstore_db \
  -e POSTGRES_DB=techstore \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin123 \
  -p 5432:5432 \
  -d postgres:15
```

### 2. Comandos Maven

```bash
# Verificar que compila sin errores
mvn compile

# Ejecutar tests
mvn test

# Generar el .JAR en target/
mvn clean package -DskipTests

# Ejecutar el .JAR
java -jar target/techstore-api-1.0.0.jar
```

---

## Uso de la API con Postman

### 1. Obtener token JWT (login)

```
POST http://localhost:8080/auth/login
Content-Type: application/json

{
  "username": "admin@techstore.cl",
  "password": "Admin1234"
}
```

**Respuesta exitosa (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tipo": "Bearer",
  "expiracion": "3600"
}
```

> Copia el token. En todos los endpoints siguientes agrega el header:
> `Authorization: Bearer <tu_token_aqui>`

---

### 2. Listar productos

```
GET http://localhost:8080/api/productos
Authorization: Bearer <token>
```

**Respuesta (200 OK):**
```json
[
  {
    "id": 1,
    "nombre": "Laptop Lenovo IdeaPad",
    "descripcion": "Notebook 15.6 pulgadas, 8GB RAM, 512GB SSD",
    "precio": 499990.0,
    "stock": 15,
    "categoria": "Computación",
    "activo": true
  }
]
```

---

### 3. Crear producto

```
POST http://localhost:8080/api/productos
Authorization: Bearer <token>
Content-Type: application/json

{
  "nombre": "Laptop Lenovo IdeaPad",
  "descripcion": "Notebook 15.6 pulgadas, 8GB RAM, 512GB SSD",
  "precio": 499990,
  "stock": 15,
  "categoria": "Computación",
  "activo": true
}
```

**Respuesta (201 Created)**

---

### 4. Modificar producto

```
PUT http://localhost:8080/api/productos/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "nombre": "Laptop Lenovo IdeaPad Pro",
  "descripcion": "Actualizada",
  "precio": 549990,
  "stock": 10,
  "categoria": "Computación",
  "activo": true
}
```

**Respuesta (200 OK)**

---

### 5. Eliminar producto (borrado lógico)

```
DELETE http://localhost:8080/api/productos/{id}
Authorization: Bearer <token>
```

**Respuesta (204 No Content)**

> El producto no se borra de la base de datos: su campo `activo` cambia a `false`.

---

## Credenciales de acceso

Las credenciales están definidas en `application.properties` y **no se almacenan en la base de datos**:

| Campo | Valor |
|---|---|
| Usuario | `admin@techstore.cl` |
| Contraseña | `Admin1234` |

---

## Estructura de ramas Git

```
main     ← código estable y probado
develop  ← integración continua
feature/ ← ramas de funcionalidades (feature/crud-productos, feature/jwt, etc.)
```

**Flujo de trabajo:**
```bash
git checkout -b feature/nombre-funcionalidad
# ... desarrollo ...
git add .
git commit -m "feat: descripción clara del cambio"
git checkout develop
git merge feature/nombre-funcionalidad
```

---

## Variables de entorno (Docker Compose)

| Variable | Valor por defecto |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/techstore` |
| `SPRING_DATASOURCE_USERNAME` | `admin` |
| `SPRING_DATASOURCE_PASSWORD` | `admin123` |

---

## Tecnologías utilizadas

- Java 17
- Spring Boot 3.2
- Spring Security + JWT (jjwt 0.11.5)
- Spring Data JPA + Hibernate
- PostgreSQL 15
- Maven 3.9
- Docker + Docker Compose
- Lombok
