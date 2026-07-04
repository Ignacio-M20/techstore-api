# TechStore Chile — Microservicio de Gestión de Productos

Microservicio RESTful desarrollado con **Java 17 + Spring Boot 3.2**, que administra el catálogo de productos de TechStore Chile. Incluye autenticación JWT, persistencia en PostgreSQL, auditoría asíncrona de inventario mediante **Amazon SQS + AWS Lambda**, y despliegue productivo en **AWS Academy (ECS Fargate + API Gateway)** con pipeline de CI/CD en GitHub Actions.

---

## Arquitectura de despliegue

```
Cliente (Postman/JWT)
      │  HTTPS
      ▼
Amazon API Gateway  ──────────────►  Application Load Balancer (ALB)
                                            │  :8080
                                            ▼
                                   Amazon ECS Fargate (techstore-api)
                                            │  JDBC
                                            ▼
                                   RDS / PostgreSQL
                                            │
                                            │  (evento async en cada escritura)
                                            ▼
                                   Amazon SQS (techstore-audit-queue)
                                            │  trigger
                                            ▼
                                   AWS Lambda (techstore-audit-logger)
                                            │
                                            ▼
                                   Amazon CloudWatch Logs
```

- **Flujo síncrono:** el cliente consume la API a través de API Gateway, que enruta al ALB, el cual balancea hacia las tareas de ECS Fargate.
- **Flujo asíncrono:** cada `POST`, `PUT` o `DELETE` lógico publica un evento JSON de auditoría en SQS, que dispara la Lambda de logging hacia CloudWatch.

---

## Estructura del proyecto (arquitectura en capas)

```
src/main/java/cl/techstore/api/
│
├── config/
│   └── SqsConfig.java              ← Cliente SqsClient (DefaultCredentialsProvider, us-east-1)
│
├── controller/
│   ├── AuthController.java         ← POST /auth/login
│   └── ProductoController.java     ← GET, POST, PUT, DELETE /api/productos
│
├── service/
│   ├── ProductoService.java        ← Lógica de negocio del CRUD
│   └── AuditoriaService.java       ← Publica eventos de auditoría en SQS (@Async)
│
├── repository/
│   └── ProductoRepository.java     ← Extiende JpaRepository
│
├── model/
│   └── Producto.java               ← Entidad JPA (@Entity)
│
├── security/
│   ├── JwtUtil.java                ← Generación y validación de tokens
│   ├── JwtFilter.java              ← Filtro que intercepta cada petición
│   └── SecurityConfig.java         ← Configuración de Spring Security
│
└── dto/
    ├── LoginRequest.java            ← { username, password }
    ├── LoginResponse.java           ← { token, tipo, expiracion }
    └── ProductoDTO.java             ← Objeto de transferencia de datos

.github/workflows/
└── deploy.yml                      ← Pipeline CI/CD: build → push ECR → deploy ECS Fargate

Dockerfile                          ← Multi-stage (Maven → eclipse-temurin:17-jre-alpine, user no-root)
docker-compose.yml                  ← Levanta Postgres + microservicio en entorno local
```

---

## Requisitos previos

| Herramienta | Versión mínima |
|---|---|
| Java (JDK) | 17 |
| Maven | 3.9+ |
| Docker Desktop | Cualquier versión actual |
| Postman | Cualquier versión |
| AWS CLI | v2 (configurado con credenciales de AWS Academy) |
| Cuenta AWS Academy Learner Lab | Activa, con rol `LabRole` disponible |

---

## Clonar el repositorio

```bash
git clone https://github.com/<tu-usuario>/techstore-api.git
cd techstore-api
```

---

## Ejecución local con Docker Compose

Levanta PostgreSQL y el microservicio con un solo comando:

```bash
docker compose up --build
```

La API queda disponible en: `http://localhost:8080`

Para detener el entorno:
```bash
docker compose down -v
```

> **Nota sobre SQS en local:** el servicio `AuditoriaService` intenta publicar en la cola real de AWS (`techstore-audit-queue`) usando `DefaultCredentialsProvider`. Para que funcione en local, monta tus credenciales temporales de AWS Academy como volumen (ver `docker-compose.yml`, servicio `microservicio` → `volumes: ~/.aws:/home/nobody/.aws:ro`) y define la variable `SQS_QUEUE_URL`. Si no configuras credenciales, el resto de la API funciona igual: el envío a SQS falla de forma controlada (try/catch) y solo queda un log de advertencia.

---

## Despliegue en AWS Academy Learner Lab

### 1. Restricción de seguridad — `LabRole`

El laboratorio no permite crear roles IAM propios. Todo componente (ECS Task Definition, Lambda) debe usar el rol preconfigurado:
```
arn:aws:iam::<ACCOUNT_ID>:role/LabRole
```

### 2. Build y push de la imagen a Amazon ECR

```bash
mvn clean package -DskipTests

aws ecr get-login-password --region us-east-1 \
  | docker login --username AWS --password-stdin <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com

docker build -t techstore-api .
docker tag techstore-api:latest <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/techstore-api:latest
docker push <ACCOUNT_ID>.dkr.ecr.us-east-1.amazonaws.com/techstore-api:latest
```

### 3. ECS Fargate

- Cluster: `techstore-cluster`
- Servicio: `techstore-service`
- Task Definition: 0.25 vCPU / 0.5 GB RAM, Task Role y Execution Role = `LabRole`
- Expuesto públicamente a través de un **Application Load Balancer (ALB)**

### 4. Amazon SQS

Cola estándar: `techstore-audit-queue`. El microservicio publica en ella con la siguiente estructura de evento:

```json
{
  "accion": "CREAR / MODIFICAR / ELIMINAR",
  "productoId": 12,
  "nombre": "Laptop Lenovo IdeaPad",
  "usuario": "admin@techstore.cl",
  "fecha": "2026-07-04T14:43:00Z"
}
```

### 5. AWS Lambda

Función `techstore-audit-logger` (rol `LabRole`), con **trigger de SQS** sobre `techstore-audit-queue`. Procesa cada mensaje y registra la auditoría en **Amazon CloudWatch Logs**.

### 6. Amazon API Gateway

Ruta `ANY /{proxy+}` integrada vía HTTP hacia el DNS del ALB (`http://<alb-dns>/{proxy}`). Los Security Groups del ALB y de ECS solo permiten tráfico entrante desde el Gateway/ALB, bloqueando accesos directos.

### 7. CI/CD — GitHub Actions

El archivo `.github/workflows/deploy.yml` se dispara en cada `push` a `main` y automatiza: compilación Maven → build de imagen Docker → push a ECR → actualización del servicio ECS Fargate.

**Secretos requeridos en el repositorio** (Settings → Secrets and variables → Actions):

| Secreto | Descripción |
|---|---|
| `AWS_ACCESS_KEY_ID` | Credencial temporal de AWS Academy |
| `AWS_SECRET_ACCESS_KEY` | Credencial temporal de AWS Academy |
| `AWS_SESSION_TOKEN` | Token de sesión (obligatorio, expira en pocas horas) |

> Las credenciales de AWS Academy son temporales: debes actualizar estos tres secretos cada vez que reinicies el Learner Lab.

---

## Uso de la API con Postman

### 1. Obtener token JWT (login)

```
POST /auth/login
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

Usa como base:
- **Local:** `http://localhost:8080`
- **AWS (vía API Gateway):** `https://<api-id>.execute-api.us-east-1.amazonaws.com`

---

### 2. Listar productos

```
GET /api/productos
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
POST /api/productos
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

**Respuesta (201 Created)** — dispara evento de auditoría `CREAR` hacia SQS.

---

### 4. Modificar producto

```
PUT /api/productos/{id}
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

**Respuesta (200 OK)** — dispara evento de auditoría `MODIFICAR` hacia SQS.

---

### 5. Eliminar producto (borrado lógico)

```
DELETE /api/productos/{id}
Authorization: Bearer <token>
```

**Respuesta (204 No Content)** — dispara evento de auditoría `ELIMINAR` hacia SQS.

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
main     ← código estable, dispara el pipeline de despliegue a AWS
develop  ← integración continua
feature/ ← ramas de funcionalidades (feature/crud-productos, feature/jwt, feature/sqs-auditoria, etc.)
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

## Variables de entorno

| Variable | Valor por defecto / ejemplo | Uso |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://postgres:5432/techstore` | Conexión a base de datos |
| `SPRING_DATASOURCE_USERNAME` | `admin` | Usuario de base de datos |
| `SPRING_DATASOURCE_PASSWORD` | `admin123` | Contraseña de base de datos |
| `SQS_QUEUE_URL` | `https://sqs.us-east-1.amazonaws.com/<ACCOUNT_ID>/techstore-audit-queue` | URL de la cola de auditoría |

---

## Tecnologías utilizadas

- Java 17
- Spring Boot 3.2
- Spring Security + JWT (jjwt 0.11.5)
- Spring Data JPA + Hibernate
- PostgreSQL 15
- Maven 3.9
- Docker (multi-stage, `eclipse-temurin:17-jre-alpine`, usuario no-root) + Docker Compose
- AWS SDK v2 para Java (SQS)
- **AWS:** ECR, ECS Fargate, Application Load Balancer, API Gateway, SQS, Lambda, CloudWatch, IAM (`LabRole`)
- GitHub Actions (CI/CD)