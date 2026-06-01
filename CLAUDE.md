# Rotula IA — Contexto del proyecto

## Stack

- **Backend**: Spring Boot 3.3.0, Java 21, PostgreSQL, Flyway (V1–V9), JPA/Hibernate, Spring Security + JWT, Maven
- **Frontend**: React 18 + TypeScript, Vite 5, Tailwind CSS, React Hook Form v7, Zod, TanStack React Query v5, Axios
- **Multitenancy**: RLS con `app.current_tenant` (PostgreSQL session var), `TenantAwareDataSource`, `JwtAuthFilter`
- **Sin interfaces** en servicios (decisión deliberada — Spring 4+ no lo requiere, una sola implementación por servicio)

## Cómo correr

```bash
# Backend (desde /backend)
mvn spring-boot:run   # requiere JDK 21 en JAVA_HOME

# Frontend (desde /frontend)
npm install
npm run dev           # http://localhost:5173
```

## Estructura de carpetas relevante

```
backend/src/main/java/ar/com/rotula/
  controller/          # REST controllers
  domain/              # Entidades JPA
  dto/                 # Records de request/response
  repository/          # JPA repositories
  service/             # Servicios (sin interfaces)
  service/nutrition/   # NutritionCalculatorService, rounder, seals
  service/allergen/    # AllergenDeclarationService
  security/            # JWT, AppUserDetails, JwtAuthFilter
  config/              # SecurityConfig, TenantDataSourceWrapper

backend/src/main/resources/db/migration/
  V1__init.sql                           # Schema base
  V2__rls_users_and_force.sql
  V3__weight_unit_add_cc.sql
  V4__ingredients_weight_grams.sql
  V5__regulatory_rules.sql               # Reglas normativas CAA + seed
  V6__ingredients_nutrition_products_serving.sql
  V7__regulatory_names_label_data.sql    # Denominaciones legales CAA + seed
  V8__food_groups_product_nutrition.sql  # TABLA I + reestructura products
  V9__drop_products_category.sql         # Fix: elimina category (NOT NULL)

frontend/src/
  components/products/ProductForm.tsx    # Formulario v2 (ver abajo)
  components/ingredients/IngredientForm.tsx
  lib/foodReferenceApi.ts
  lib/productsApi.ts / ingredientsApi.ts
  types/product.ts / ingredient.ts
```

## Modelo de datos — estado actual (post V9)

### products
| Columna | Tipo | Notas |
|---|---|---|
| id | UUID | PK |
| tenant_id | UUID | RLS |
| name | VARCHAR(300) | Nombre interno (no va al rótulo) |
| denomination | VARCHAR(300) | Denominación del alimento (va al rótulo, CAA) |
| food_group_id | UUID | FK → food_groups (TABLA I) |
| food_item_id | UUID | FK → food_items (TABLA I) |
| serving_size_g | NUMERIC(8,2) | Auto-completado desde food_item, read-only en UI |
| net_weight | NUMERIC(10,3) | |
| weight_unit | VARCHAR(10) | g/kg/ml/l/cc/u |
| rne_number | VARCHAR(20) | Opcional (futuro: por planta) |
| rnpa_number | VARCHAR(20) | **Obligatorio** |
| cross_contamination | TEXT | AllergenGroup names separados por coma |
| show_ingredient_percentages | BOOLEAN | DEFAULT false |
| energy_kcal_per100g … sodium_mg_per100g | NUMERIC(8,2) | 8 campos nutricionales por 100g |
| status | VARCHAR(20) | draft/active |
| created_by / created_at / updated_at | | |

### ingredients (simplificado en V8)
| Columna | Notas |
|---|---|
| id, product_id, tenant_id | |
| name | VARCHAR(300) |
| weight_grams | NUMERIC(10,3) — el % se calcula on-the-fly |
| is_allergen | BOOLEAN — auto-detectado o manual |
| created_at | |
| ~~campos nutricionales~~ | Eliminados en V8 — la nutrición vive en products |

### food_groups / food_items (nuevos en V8)
- `food_groups`: 8 grupos de la TABLA I (Res. Conjunta 21/2023)
- `food_items`: ~55 alimentos con `portion_grams` y `unit` (g o ml)
- Endpoint: `GET /food-reference/groups` y `/groups/{id}/items`

## Endpoints principales

| Método | Path | Descripción |
|---|---|---|
| POST | /auth/register, /auth/login | Auth JWT |
| GET/POST | /products | Lista paginada / crear producto |
| GET/PUT/DELETE | /products/{id} | CRUD producto |
| GET | /products/{id}/ingredients | Lista ingredientes |
| POST | /products/{id}/ingredients | Crear ingrediente |
| PUT/DELETE | /ingredients/{id} | Editar / eliminar ingrediente |
| GET | /food-reference/groups | Grupos TABLA I |
| GET | /food-reference/groups/{id}/items | Alimentos del grupo |
| GET | /food-reference/items/{id} | Alimento por ID |
| POST | /labels/generate/{productId} | Genera rótulo |
| GET | /labels/{productId}/versions | Historial de versiones |
| GET | /analysis/{productId} | Análisis completo (nutrición + sellos + alérgenos) |
| GET | /legal-name/{productId} | Sugerencia de denominación legal |

## Lógica de negocio clave

### Nutrición
- Los valores se cargan **por 100g en el producto** (no por ingrediente)
- `NutritionCalculatorService.calculate(Product)` escala a por-porción: `valor * serving_size_g / 100`
- La porción viene siempre de `food_items.portion_grams` (no editable por el usuario)
- `SealEvaluatorService` evalúa sellos Ley 27.642 sobre los valores por 100g

### Alérgenos
- Detección automática en ingredientes: `AllergenDetector.isAllergen(name)` según Res. 109/2023
- 14 grupos en enum `AllergenGroup`
- Contaminación cruzada: multi-select en el formulario de producto, guardado como CSV en `cross_contamination`
- `AllergenDeclarationService` genera el texto de declaración

### % de ingredientes
- `showIngredientPercentages` por producto (toggle en el formulario)
- El % se calcula on-the-fly: `weightGrams / sum(weightGrams) * 100`
- Los ingredientes se listan en orden decreciente de peso (norma CAA)

### Denominación legal
- `LegalNameService` busca en tabla `regulatory_name` usando `product.denomination` como clave
- Genera claims nutricionales (Bajo en sodio, Sin grasas, etc.) según CAA Art. 1385
- Detecta enriquecimiento (hierro, calcio, etc.) en nombres de ingredientes

## Normativa implementada

| Resolución / Ley | Qué cubre |
|---|---|
| Res. Conjunta 1/2023 (Res. 109/2023) | 14 alérgenos de declaración obligatoria |
| Ley 27.642 / Decreto 151/2022 | Sellos octogonales (exceso sodio/grasas/azúcares/calorías) |
| Res. Conjunta 21/2023 | TABLA I — porciones de referencia por grupo |
| CAA Art. 1354 | Tabla nutricional obligatoria |
| CAA Art. 1385 | Claims nutricionales (bajo en, sin, enriquecido con) |
| V5 seed | 25+ reglas normativas por categoría (bebidas, lácteos, panificados, etc.) |
| V7 seed | 35 denominaciones legales CAA |

## Convenciones de código

- **DTOs**: Java records (`record ProductRequest(...)`)
- **Validaciones**: Bean Validation en records + Zod en frontend
- **Seguridad**: JWT stateless, `@WithMockUser` en tests de controller
- **Tests**: `@ExtendWith(MockitoExtension.class)` para unit, `@WebMvcTest` para controller IT
- **Sin interfaces** en servicios (ver arriba)
- **forwardRef** en componentes React que wrappean `<input>` y usan `register()` de RHF (crítico para que `_f.mount` se setee)

## Pendientes / próximos pasos conocidos

- [ ] RNE por planta (alta de establecimientos, luego asociar al producto)
- [ ] Generación y exportación del rótulo en PDF/imagen
- [ ] Vista previa del rótulo en tiempo real
- [ ] Integración con IA para sugerencia de denominación legal
- [ ] Panel de administración de tenants
