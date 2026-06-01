# Rotula IA — Manual de usuario

## ¿Qué es Rotula IA?

Rotula IA es una plataforma para generar rótulos de alimentos que cumplen con la normativa argentina vigente (CAA, Ley 27.642, Res. 109/2023). Permite cargar productos, definir su composición e información nutricional, y obtener el rótulo completo listo para imprimir o exportar.

---

## Acceso al sistema

1. Ingresá a la URL de la plataforma.
2. Iniciá sesión con tu email y contraseña.
3. Si es tu primer ingreso, usá las credenciales que te proporcionó el administrador.

---

## Flujo de trabajo

```
Crear producto → Agregar ingredientes → Generar rótulo → Exportar
```

---

## 1. Crear un producto

Desde el menú principal, hacé clic en **Nuevo producto**.

### Campos del formulario

#### Identificación

| Campo | Descripción | Obligatorio |
|---|---|---|
| **Nombre interno** | Nombre de uso interno (no aparece en el rótulo). Ej.: "Galletitas avena lote A" | Sí |
| **Denominación del alimento** | Nombre que figura en el rótulo según el CAA. Ej.: "Galletitas dulces de avena con chips de chocolate" | Sí |

#### Clasificación (TABLA I)

Seleccioná el **grupo de alimentos** y luego el **alimento de referencia**. El sistema completa automáticamente la **porción de referencia** según la Resolución Conjunta 21/2023 — no se puede modificar.

> **¿Por qué no puedo cambiar la porción?**
> La porción de referencia está definida por la normativa argentina para cada tipo de alimento. Usarla garantiza que tu tabla nutricional sea comparable con otros productos del mismo grupo.

#### Presentación

- **Peso neto**: cantidad neta del producto (sin envase).
- **Unidad**: g, kg, ml, l, cc o u (unidades).

#### Registros

| Campo | Descripción |
|---|---|
| **RNPA** | Número de Registro Nacional de Producto Alimenticio. **Obligatorio.** |
| **RNE** | Número de Registro Nacional de Establecimiento. Opcional por ahora. |

#### Contaminación cruzada

Marcá los alérgenos **presentes en el ambiente de producción** (no en la receta). Esto genera la leyenda de contaminación cruzada en el rótulo según la Res. 109/2023.

Los 14 alérgenos disponibles son los de declaración obligatoria en Argentina:
Gluten, Crustáceos, Huevo, Pescado, Maní, Soja, Leche, Frutos de cáscara, Apio, Mostaza, Sésamo, Sulfitos, Altramuces, Moluscos.

#### Opciones de rótulo

- **Mostrar % de ingredientes**: activalo si querés que cada ingrediente muestre su porcentaje en la lista. Es optativo según la normativa.

#### Información nutricional por 100 g *(opcional)*

Completá los valores del producto final tal como salen del laboratorio o de la hoja técnica del fabricante. Ingresá los datos **por cada 100 g** — el sistema calcula automáticamente los valores por porción.

| Campo | Unidad |
|---|---|
| Energía | kcal |
| Proteínas | g |
| Carbohidratos | g |
| Azúcares | g |
| Grasas totales | g |
| Grasas saturadas | g |
| Grasas trans | g |
| Sodio | mg |

> Si no cargás información nutricional, el rótulo se genera sin tabla nutricional.

---

## 2. Agregar ingredientes

Una vez creado el producto, entrá al detalle y hacé clic en **Agregar ingrediente**.

| Campo | Descripción |
|---|---|
| **Nombre** | Nombre del ingrediente tal como debe aparecer en el rótulo. |
| **Peso (g)** | Peso del ingrediente en gramos en la formulación del producto. El porcentaje se calcula automáticamente. |
| **¿Es alérgeno?** | El sistema lo detecta automáticamente según el nombre. Podés corregirlo manualmente. |

### Orden de los ingredientes

Los ingredientes se listan automáticamente de mayor a menor peso (orden decreciente), como exige el CAA. No es necesario ordenarlos a mano.

### Detección automática de alérgenos

Mientras escribís el nombre del ingrediente, el sistema detecta si es un alérgeno según la Res. 109/2023. Aparece un aviso en amarillo si detecta algo. Podés sobreescribir la detección con el toggle manual.

---

## 3. Generar el rótulo

Desde el detalle del producto, hacé clic en **Generar rótulo**.

El sistema analiza automáticamente:

- **Tabla nutricional** (por porción y por 100 g)
- **Sellos de advertencia** (Ley 27.642): Exceso en sodio / grasas saturadas / azúcares / calorías
- **Declaración de alérgenos** (Res. 109/2023): alérgenos presentes en ingredientes + contaminación cruzada
- **Claims nutricionales** (CAA Art. 1385): "Sin grasas trans", "Bajo en sodio", etc., si corresponden
- **Denominación legal sugerida**: el sistema la compara con la denominación que cargaste y te avisa si hay diferencias

Cada generación crea una **nueva versión** del rótulo. Podés ver el historial de versiones desde el detalle del producto.

---

## 4. Sellos de advertencia (Ley 27.642)

El sistema aplica los sellos octogonales automáticamente según los umbrales vigentes (Fase 1 — desde agosto 2023):

| Sello | Umbral en sólidos | Umbral en líquidos |
|---|---|---|
| Exceso en sodio | ≥ 400 mg/100g | ≥ 100 mg/100mL |
| Exceso en grasas saturadas | ≥ 10 g/100g | ≥ 3 g/100mL |
| Exceso en azúcares | ≥ 10 g/100g | ≥ 5 g/100mL |
| Exceso en calorías | ≥ 275 kcal/100g | ≥ 70 kcal/100mL |

> **Fase 2 (agosto 2025):** los umbrales bajan. El sistema se actualizará automáticamente.

---

## 5. Preguntas frecuentes

**¿Por qué el sistema detecta un alérgeno en un ingrediente que no lo es?**
La detección es automática basada en palabras clave. Si el sistema se equivoca, usá el toggle manual para corregirlo.

**¿Puedo editar un rótulo ya generado?**
No directamente. Modificá el producto o los ingredientes y generá una nueva versión. El historial queda guardado.

**¿El grupo de alimentos aparece en el rótulo?**
No. Es un campo interno de clasificación que solo usa el sistema para determinar la porción de referencia.

**¿Qué pasa si no cargo la información nutricional?**
El rótulo se genera sin tabla nutricional. Si la normativa la exige para tu categoría de producto, el sistema te lo indicará al generar.

**¿Puedo tener varios usuarios en mi empresa?**
Sí. El administrador de tu cuenta puede crear usuarios adicionales con distintos roles.

---

## Glosario

| Término | Significado |
|---|---|
| **CAA** | Código Alimentario Argentino |
| **RNPA** | Registro Nacional de Producto Alimenticio |
| **RNE** | Registro Nacional de Establecimiento |
| **TABLA I** | Tabla de porciones de referencia por grupo de alimentos (Res. Conjunta 21/2023) |
| **Res. 109/2023** | Resolución que define los 14 alérgenos de declaración obligatoria en Argentina |
| **Ley 27.642** | Ley de etiquetado frontal de alimentos (sellos octogonales) |
| **Porción de referencia** | Cantidad estándar definida por normativa para comparar productos del mismo grupo |
