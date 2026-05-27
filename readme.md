# RotulaIA

Plataforma SaaS para generación y validación de rótulos alimenticios según normativa argentina (CAA, ANMAT, Ley 27642).

## Stack

- **Backend:** Java 21 + Spring Boot 3 + PostgreSQL 16
- **Frontend:** React 18 + TypeScript + Vite + Tailwind CSS
- **Infra:** Docker Compose (desarrollo local)

## Levantar el entorno local

### Requisitos
- Docker Desktop
- Java 21 (para desarrollo sin Docker)
- Node.js 20 (para desarrollo sin Docker)

### Con Docker (recomendado)

```bash
docker-compose up --build
```

Servicios disponibles:
- Frontend: http://localhost:5173
- Backend API: http://localhost:8080
- PostgreSQL: localhost:5432

### Sin Docker

**Backend:**
```bash
cd backend
./mvnw spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

## Variables de entorno

Copiá `.env.example` a `.env` y ajustá los valores:

```bash
cp .env.example .env
```

## Estructura del proyecto

```
rotula-ia/
├── backend/          # Spring Boot — API REST
│   ├── src/main/java/ar/com/rotula/
│   │   ├── config/       # Configuración Spring, Security
│   │   ├── controller/   # REST controllers
│   │   ├── domain/       # Entidades JPA
│   │   ├── dto/          # Request / Response DTOs
│   │   ├── exception/    # Excepciones y handlers
│   │   ├── repository/   # Spring Data repositories
│   │   ├── security/     # JWT, filtros, contexto tenant
│   │   └── service/      # Lógica de negocio
│   └── src/main/resources/
│       ├── db/migration/ # Flyway migrations
│       └── application.yml
├── frontend/         # React + TypeScript
├── docs/             # Normativa, decisiones de arquitectura
└── docker-compose.yml
```

## Normativa aplicada

- Código Alimentario Argentino (CAA)
- Ley 27642 — Etiquetado frontal (sellos octagonales)
- Resolución ANMAT 109/2023 — Alérgenos

## Contacto
- Acá van los contactos