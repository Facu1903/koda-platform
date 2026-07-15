# KODA PLATFORM - Coding Standards

## 1. Principio rector

El codigo debe ser claro, mantenible, testeable y preparado para evolucion. Si una solucion parece "rapida pero despues vemos", ya perdio el juicio tecnico.

## 2. Idioma

- Codigo, nombres de clases, metodos, variables, commits y ramas: ingles.
- Documentacion funcional y de proyecto: espanol, salvo terminos tecnicos.
- Mensajes al usuario final: espanol inicialmente, preparados para i18n.

## 3. Backend Java

### Version

- Java 21.
- Spring Boot 3.
- Maven.

### Paquetes

Base package:

```text
com.koda.platform
```

Convencion por modulo:

```text
com.koda.platform.<product-or-area>.<module>.<layer>
```

Ejemplos:

```text
com.koda.platform.platform.tenants.domain
com.koda.platform.platform.security.application
com.koda.platform.erp.products.api
com.koda.platform.erp.stock.infrastructure
```

### Nombres

- Entidades de dominio: sustantivo singular (`Product`, `Tenant`, `StockMovement`).
- Value objects: nombre semantico (`Money`, `EmailAddress`, `TenantId`).
- Casos de uso/servicios de aplicacion: accion clara (`CreateProductUseCase`, `AssignRoleUseCase`).
- DTO request: `CreateProductRequest`.
- DTO response: `ProductResponse`.
- Repositorio de dominio: `ProductRepository`.
- Implementacion infraestructura: `JpaProductRepository`.
- Mapper: `ProductMapper`.

## 4. Clean Architecture

Reglas:

- `domain` no depende de frameworks.
- `application` orquesta casos de uso.
- `infrastructure` implementa detalles tecnicos.
- `api` adapta HTTP al modelo de aplicacion.
- No se inyectan repositorios JPA directamente en controladores.
- No se coloca logica de negocio en controladores.
- No se devuelven entidades JPA desde endpoints.

## 5. Validaciones

Usar Bean Validation para validaciones de entrada:

- `@NotBlank`
- `@NotNull`
- `@Size`
- `@Email`
- Validadores custom cuando la regla lo amerite.

Las reglas de negocio que dependen del estado del sistema deben vivir en application/domain, no solo en anotaciones.

## 6. Manejo de errores

Debe existir un manejador global de excepciones.

Categorias minimas:

- Validation error.
- Authentication error.
- Authorization error.
- Resource not found.
- Conflict.
- Business rule violation.
- Unexpected internal error.

Los errores deben incluir codigo estable para frontend y soporte.

## 7. Persistencia

### JPA/Hibernate

- Las entidades JPA pertenecen a infraestructura o a un subpaquete claramente aislado si se decide un modelo persistente separado.
- Evitar relaciones bidireccionales salvo necesidad real.
- Evitar `FetchType.EAGER` por defecto.
- Paginacion obligatoria para listados.
- No usar consultas sin filtro tenant en datos de negocio.

### Flyway

Convencion:

```text
VYYYYMMDDHHMM__description.sql
```

Ejemplo:

```text
V202607141000__create_tenants_table.sql
```

Cada migracion debe ser pequena, legible y revisable.

## 8. Base de datos

Convenciones:

- Tablas: `snake_case`, plural o concepto estable (`tenants`, `stock_movements`).
- Columnas: `snake_case`.
- PK: `id`.
- FK: `<entity>_id`.
- Tenant: `tenant_id`.
- Timestamps: `created_at`, `updated_at`, `deleted_at`.
- Usuarios auditoria: `created_by`, `updated_by`.

Unicidad tenant-scoped:

```text
unique (tenant_id, code)
```

## 9. Seguridad en codigo

- Nunca confiar en datos sensibles enviados desde frontend.
- Nunca loguear passwords, tokens, hashes ni datos secretos.
- Nunca aceptar `tenant_id` libre para operar sobre datos de negocio.
- Validar autorizacion en casos de uso criticos.
- Usar IDs opacos.
- Evitar mensajes de error que revelen existencia de recursos de otro tenant.

## 10. Frontend TypeScript

### Version y tooling

- React.
- TypeScript estricto.
- Vite.
- Material UI.

### Estructura

```text
src/
  app/
  shared/
    components/
    hooks/
    api/
    utils/
    types/
  platform/
  erp/
  theme/
  routes/
  test/
```

### Convenciones

- Componentes: `PascalCase`.
- Hooks: `useSomething`.
- Utilidades: funciones puras cuando sea posible.
- Tipos: evitar `any`.
- Formularios: validacion del lado cliente y respaldo obligatorio en backend.
- Estado remoto: centralizar cliente HTTP y cache si se adopta libreria.

## 11. UI

- Tema oscuro por defecto.
- Material UI como base.
- Color primario inicial: `#F6862B`.
- No duplicar componentes visuales.
- Cada pantalla debe contemplar loading, empty, error y forbidden.
- Componentes orientados a trabajo intensivo, no composiciones decorativas.

## 12. Testing

### Backend

Minimos:

- Unit tests para reglas de dominio.
- Tests de application services/use cases.
- Tests de API para endpoints criticos.
- Tests de persistencia para consultas tenant-scoped.
- Tests de seguridad para aislamiento y permisos.

Herramientas esperadas:

- JUnit 5.
- Mockito cuando corresponda.
- Spring Boot Test para integracion.
- Testcontainers recomendado para PostgreSQL real.

### Frontend

Minimos:

- Tests de componentes criticos.
- Tests de flujos de autenticacion.
- Tests de permisos y estados de UI.
- Tests de utilidades y cliente API.

Herramientas esperadas:

- Vitest.
- React Testing Library.

## 13. Calidad

No se considera terminado si:

- No compila.
- Rompe tests existentes.
- Introduce duplicacion obvia.
- Agrega logica de negocio en el lugar incorrecto.
- Omite tenant en consultas de negocio.
- Expone entidades directamente.
- No actualiza documentacion afectada.

## 14. Comentarios

Comentar solo cuando el codigo no sea autoexplicativo. Un comentario debe explicar intencion, restriccion o decision, no repetir lo que hace una linea.

## 15. Commits

Formato recomendado:

```text
type(scope): summary
```

Tipos:

- `feat`
- `fix`
- `docs`
- `test`
- `refactor`
- `chore`
- `build`
- `ci`

Ejemplos:

```text
docs(project): add sprint 0 constitution
feat(auth): add jwt login flow
test(stock): cover cross-tenant isolation
```

## 16. Definition of Done tecnica

Una tarea esta terminada cuando:

- Cumple criterio funcional aprobado.
- Respeta arquitectura.
- Tiene tests proporcionales al riesgo.
- Compila.
- Ejecuta localmente.
- Actualiza documentacion.
- Actualiza changelog.
- No deja deuda no documentada.

