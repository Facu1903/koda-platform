# ERP Catalogs

## Objetivo

El Hito 6 implementa los catalogos base de KODA ERP con aislamiento multiempresa: marcas, categorias, unidades de medida, presentaciones y productos.

Todos los endpoints son tenant-scoped. El tenant efectivo se resuelve desde `TenantContext`; el cliente no envia ni decide `tenant_id`.

## Base funcional aprobada

Aprobado por Product Owner el 2026-07-17:

- Marca opcional en productos.
- Categorias planas para Sprint 1.
- Un producto tiene una presentacion principal.
- SKU unico por tenant.
- Se permite desactivar productos con stock.
- La eliminacion es soft delete.
- `TENANT_OWNER` y `TENANT_ADMIN`: CRUD completo de catalogos.
- `MANAGER`: lectura y actualizacion de catalogos.
- `READ_ONLY`: solo lectura de catalogos.

## Endpoints

Base path: `/api/v1/catalog`.

### Brands

- `GET /brands`
- `GET /brands/{id}`
- `POST /brands`
- `PUT /brands/{id}`
- `DELETE /brands/{id}?version={version}`

### Categories

- `GET /categories`
- `GET /categories/{id}`
- `POST /categories`
- `PUT /categories/{id}`
- `DELETE /categories/{id}?version={version}`

Categorias son planas en Sprint 1. La tabla soporta `parent_id`, pero la API no lo expone todavia.

### Units

- `GET /units`
- `GET /units/{id}`
- `POST /units`
- `PUT /units/{id}`
- `DELETE /units/{id}?version={version}`

### Presentations

- `GET /presentations`
- `GET /presentations/{id}`
- `POST /presentations`
- `PUT /presentations/{id}`
- `DELETE /presentations/{id}?version={version}`

Una presentacion referencia una unidad de medida activa del mismo tenant.

### Products

- `GET /products`
- `GET /products/{id}`
- `POST /products`
- `PUT /products/{id}`
- `DELETE /products/{id}?version={version}`

Producto requiere unidad base y presentacion principal activas. Marca y categoria son opcionales.

## Permisos

| Rol | Permisos de catalogos |
| --- | --- |
| `TENANT_OWNER` | `read`, `create`, `update`, `delete` para productos, marcas, categorias, unidades y presentaciones |
| `TENANT_ADMIN` | `read`, `create`, `update`, `delete` para productos, marcas, categorias, unidades y presentaciones |
| `MANAGER` | `read`, `update` para productos, marcas, categorias, unidades y presentaciones |
| `READ_ONLY` | `read` para productos, marcas, categorias, unidades y presentaciones |

La matriz se aplica al tenant piloto KODA mediante Flyway en `V202607171530__assign_catalog_role_permissions.sql`.

## Reglas tecnicas

- Codigos y SKU se normalizan a mayusculas.
- `version` es obligatoria para actualizaciones y eliminaciones.
- `DELETE` realiza soft delete.
- Productos aceptan estado `ACTIVE` o `INACTIVE` desde la API.
- `allowNegativeStock` no se expone y queda siempre en `false`.
- Las referencias deben existir, estar activas y pertenecer al mismo tenant.
- Las operaciones sensibles registran auditoria en `audit_events`.

## Ejemplo de producto

Request:

```json
{
  "sku": "ALIMENTO-001",
  "name": "Alimento balanceado adulto",
  "description": "Bolsa 15 kg",
  "barcode": "7790000000010",
  "brandId": null,
  "categoryId": null,
  "baseUnitId": "00000000-0000-4000-8000-000000000010",
  "defaultPresentationId": "00000000-0000-4000-8000-000000000011",
  "productType": "GOOD",
  "status": "ACTIVE",
  "stockTrackingEnabled": true
}
```

## Errores

- `401`: falta autenticacion o token valido.
- `403 PERMISSION_DENIED`: el JWT no contiene el permiso requerido.
- `404 CATALOG_ITEM_NOT_FOUND`: el item no existe para el tenant autenticado.
- `400 CATALOG_REFERENCE_NOT_FOUND`: una referencia no existe, no esta activa o no pertenece al tenant.
- `409 CATALOG_VERSION_CONFLICT`: la version enviada no coincide con la version actual.
- `400 VALIDATION_ERROR`: request invalido por Bean Validation.
- `400 INVALID_REQUEST`: regla de aplicacion invalida.

## No incluido en Hito 6

- Busqueda avanzada, paginacion o filtros complejos.
- Jerarquia de categorias.
- Multiples presentaciones por producto con reglas comerciales avanzadas.
- Precios.
- Impuestos.
- Imagenes de producto.
- Validacion de stock para eliminar/desactivar.
- UI de catalogos.