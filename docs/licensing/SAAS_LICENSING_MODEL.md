# SaaS Licensing Model

## Estado

Modelo persistente implementado en Sprint 3 Hito 2. Calculo backend y API de capabilities implementados en Sprint 3 Hito 3.

## Objetivo

Definir la base persistente para que KODA PLATFORM pueda controlar productos, planes, modulos, suscripciones, limites, feature flags y entitlements por tenant.

Esta capa no reemplaza RBAC. La complementa.

Regla central: un usuario necesita permiso y el tenant necesita derecho comercial de uso. Una sola de esas condiciones no alcanza.

## Migracion

Modelo creado en:

- `backend/src/main/resources/db/migration/V202607201500__create_saas_licensing_model.sql`

La migracion se apoya sobre tablas ya existentes:

- `platform_products`
- `platform_modules`
- `tenant_product_entitlements`
- `tenant_module_entitlements`

## Tablas incorporadas

### `product_plans`

Define planes por producto. En Sprint 3 se crea el plan tecnico `KODA_PILOT` para `KODA_ERP`.

Campos clave:

- `product_id`
- `code`
- `name`
- `status`
- `is_system`
- `version`

### `product_plan_modules`

Relaciona planes con modulos incluidos.

Permite calcular que modulos deberia tener disponible un tenant segun su plan.

### `product_plan_limits`

Define limites por plan.

Limites iniciales modelados:

- `MAX_USERS`
- `MAX_BRANCHES`
- `MAX_WAREHOUSES`
- `MAX_PRODUCTS`
- `AUDIT_RETENTION_DAYS`

Para `KODA_PILOT` se registran como ilimitados. Esto evita inventar restricciones comerciales antes de tiempo y conserva al tenant KODA como piloto completo.

### `tenant_product_subscriptions`

Registra la suscripcion vigente de un tenant a un producto y plan.

Estados soportados:

- `ACTIVE`
- `SUSPENDED`
- `EXPIRED`
- `CANCELLED`

La migracion crea una suscripcion activa para KODA sobre `KODA_ERP` con plan `KODA_PILOT`.

### `tenant_limit_overrides`

Permite sobrescribir limites de plan para un tenant especifico.

No se cargan overrides iniciales.

### `tenant_feature_flags`

Permite activar o desactivar flags tecnicos por tenant, producto y opcionalmente modulo.

No reemplaza licencias ni permisos. Una feature flag habilitada no debe abrir un modulo sin entitlement activo.

## Ajustes sobre tablas existentes

### `platform_modules`

Se agregan:

- `core_module`
- `commercially_toggleable`

Los modulos `SECURITY`, `CONFIGURATION` y `AUDIT` quedan marcados como core y no toggleables comercialmente.

## Seed inicial

Se crea:

- Plan `KODA_PILOT` para `KODA_ERP`.
- Asociacion de `KODA_PILOT` con los 10 modulos actuales.
- 5 limites ilimitados para `KODA_PILOT`.
- Suscripcion activa de tenant KODA a `KODA_ERP` con `KODA_PILOT`.
- Insercion defensiva de entitlements de producto y modulos para KODA si faltaran.

## Indices

Se agregan indices para preparar consultas eficientes de capabilities:

- `idx_product_plans_product_status`
- `idx_product_plan_modules_product`
- `idx_product_plan_limits_plan`
- `uq_tenant_product_subscriptions_current`
- `idx_tenant_product_subscriptions_tenant_status`
- `idx_tenant_product_subscriptions_plan`
- `idx_tenant_limit_overrides_tenant_product`
- `uq_tenant_feature_flags_scope_code`
- `idx_tenant_feature_flags_tenant_product_enabled`
- `idx_tenant_product_entitlements_tenant_status`
- `idx_tenant_module_entitlements_tenant_status`

## Capabilities backend

Implementado en Hito 3 mediante:

- Servicio `TenantCapabilitiesService`.
- Puerto `TenantCapabilitiesRepository`.
- Repositorio `JdbcTenantCapabilitiesRepository`.
- Endpoint `GET /api/v1/capabilities`.

La respuesta expone:

- tenant activo,
- productos habilitados,
- modulos habilitados por producto,
- feature flags efectivas,
- limites efectivos,
- timestamp de calculo.

Reglas de calculo actuales:

- filtra siempre por tenant,
- exige suscripcion activa y vigente,
- exige plan activo,
- exige producto y modulo activos,
- exige entitlement activo de producto y modulo,
- respeta `valid_from` y `valid_until`,
- resuelve overrides de limites por tenant sobre los limites del plan.

La API requiere usuario autenticado y tenant context. No exige permiso RBAC especifico porque el frontend necesita leer capabilities para construir navegacion y rutas. El bloqueo operativo real queda en los guards backend del Hito 4.

## Validacion

Validado con:

- `mvn -B test`: 92 tests unitarios, 0 fallos.
- `mvn -B verify`: 92 tests unitarios y 8 tests de integracion, 0 fallos.
- Flyway/Testcontainers/PostgreSQL 17.10: 20 migraciones hasta `v202607201500`.

## Fuera de alcance actual

- Guards backend por modulo.
- Administracion interna de licencias.
- UI de capabilities.
- Billing real.
- Precios comerciales finales.

## Siguiente paso

Hito 4 debe implementar guards backend reutilizables por producto/modulo para que cada operacion valide permiso RBAC y derecho comercial del tenant.