# Sprint 6 - Execution Plan

## Estado

Definicion funcional aprobada por el Product Owner el 2026-07-22.

## Objetivo

Implementar acceso operativo y administracion base del tenant para que KODA PLATFORM pueda ser usada por el cliente piloto sin depender de bootstrap, datos seed o intervencion tecnica diaria.

## Principio rector

Operar sin pedirle permiso al tecnico.

La plataforma ya tiene seguridad, licencias, observabilidad y personalizacion. Sprint 6 debe convertir esa base en una experiencia administrable: entrar, mantener sesion, ver quien soy, administrar usuarios, roles, sucursales y depositos.

## Base funcional aprobada

La base funcional de Sprint 6 fue aprobada por el Product Owner el 2026-07-22 y queda documentada en `docs/sprints/SPRINT_6_FUNCTIONAL_BASELINE.md`.

Con esta aprobacion, Sprint 6 puede iniciar desarrollo por hitos sin modificar las reglas funcionales aprobadas.

## Alcance del sprint

- Login frontend real.
- `AuthProvider` y estado de sesion.
- Refresh y logout frontend.
- Proteccion de rutas privadas.
- Manejo de `TENANT_SELECTION_REQUIRED`.
- Administracion tenant-scoped de usuarios.
- Consulta y asignacion controlada de roles tenant.
- CRUD tenant-scoped de sucursales.
- CRUD tenant-scoped de depositos.
- Migracion de permisos `warehouses:*` y matriz rol-permiso aprobada.
- Auditoria de operaciones administrativas.
- Tests backend/frontend.
- Primeras pruebas smoke o E2E autenticadas.
- Documentacion y cierre.

## Hitos internos aprobados

| Hito | Estado | Resultado esperado |
| --- | --- | --- |
| 1. Base funcional Sprint 6 | Completado | Product Owner aprobo alcance, reglas, matriz de permisos y fuera de alcance. |
| 2. Fundacion frontend de autenticacion | Pendiente | Login UI, AuthProvider, almacenamiento controlado de sesion y rutas privadas. |
| 3. Contrato de sesion y refresh controlado | Pendiente | Clientes API usan fuente central de token, refresh ante 401 y logout robusto. |
| 4. Administracion backend de usuarios tenant | Pendiente | API tenant-scoped para listar, crear, actualizar y deshabilitar usuarios/membresias. |
| 5. Roles tenant y matriz de permisos | Pendiente | Consulta de roles, asignacion controlada, migracion de permisos y bloqueo de escalamiento. |
| 6. Sucursales y depositos backend | Pendiente | CRUD tenant-scoped con validaciones, version optimista, auditoria y permisos. |
| 7. UI administrativa base | Pendiente | Pantallas frontend para usuarios, roles, sucursales y depositos dentro del shell SaaS. |
| 8. Hardening Sprint 6 | Pendiente | Validacion completa backend/frontend, smoke autenticado, documentacion final y cierre tecnico. |

## Hito 1 completado

El Hito 1 cierra la definicion funcional:

- Documento `docs/sprints/SPRINT_6_FUNCTIONAL_BASELINE.md`.
- Foco aprobado: acceso operativo y administracion base del tenant.
- Confirmacion de que Sprint 6 no incluye MFA, SSO, recuperacion de password, invitaciones por email, roles custom, custom domains ni login publico tenant-aware completo.
- Matriz de permisos aprobada para usuarios, roles, sucursales y depositos.
- Criterios de aceptacion funcional.

Decision funcional: avanzar con Sprint 6 manteniendo fuera de alcance cualquier capacidad de identidad avanzada o administracion custom que pueda inflar el sprint y abrir grietas de seguridad.

## Hito 2 - Fundacion frontend de autenticacion

Construir el acceso visual real:

- Pantalla de login.
- Formularios y validaciones basicas.
- Estados de carga/error.
- `AuthProvider`.
- Estado `anonymous`, `authenticating`, `authenticated`, `refreshing` y `expired`.
- Redireccion de rutas privadas a login.
- Shell privado solo para usuarios autenticados.
- Limpieza de compatibilidad con lecturas directas de token desde `localStorage`.

Decision tecnica esperada: access token en memoria, refresh token en `sessionStorage` durante Sprint 6. No implementar "recordarme".

## Hito 3 - Contrato de sesion y refresh controlado

Endurecer la sesion operativa:

- Cliente `login`.
- Cliente `refresh`.
- Cliente `logout`.
- Fuente central de token para `platformHttp`.
- Reintento unico ante `401` cuando exista refresh token.
- Logout local aunque la revocacion remota falle.
- Manejo controlado de `TENANT_SELECTION_REQUIRED`.
- Tests frontend de login, expiracion, refresh y logout.

Si se requiere backend adicional para tenant selection, debe devolver opciones solo despues de credenciales validas y sin filtrar existencia de email ante credenciales invalidas.

## Hito 4 - Administracion backend de usuarios tenant

Implementar API tenant-scoped:

- Listar usuarios del tenant.
- Crear usuario para tenant piloto con password inicial.
- Actualizar nombre, estado y datos basicos permitidos.
- Deshabilitar membresia.
- Mantener identidad global sin borrado fisico.
- Version optimista.
- Auditoria.
- Permisos `users:*`.
- Tests de permisos, tenant isolation, versionado y auditoria.

No incluye invitaciones por email, recuperacion de password ni MFA.

## Hito 5 - Roles tenant y matriz de permisos

Implementar control real de roles:

- Consulta de roles tenant asignables.
- Asignacion y remocion controlada de roles.
- Bloqueo para no asignar roles de plataforma.
- Bloqueo para que `TENANT_ADMIN` no asigne `TENANT_OWNER`.
- Bloqueo para no quitar el ultimo owner activo.
- Migracion de permisos `warehouses:*`.
- Migracion de asignacion rol-permiso aprobada para usuarios, roles, sucursales y depositos.
- Tests Flyway/PostgreSQL 17 de matriz final.

Decision tecnica: los roles se usan para sembrar permisos; runtime autoriza por permisos atomicos y reglas anti-escalamiento.

## Hito 6 - Sucursales y depositos backend

Implementar configuracion operativa del tenant:

- CRUD de sucursales.
- CRUD de depositos.
- Baja logica/desactivacion.
- Validacion de codigos unicos por tenant.
- Deposito siempre asociado a sucursal del mismo tenant.
- Version optimista.
- Auditoria.
- Permisos `branches:*` y `warehouses:*`.
- Tests de permisos, referencias, tenant isolation, versionado y auditoria.

No incluye reglas avanzadas de stock minimo, transferencias ni costos.

## Hito 7 - UI administrativa base

Construir pantallas operativas dentro del shell:

- Usuarios.
- Roles asignados.
- Sucursales.
- Depositos.
- Estados de carga/error.
- Formularios con validacion basica.
- Manejo de conflictos de version.
- Bloqueo visual por permisos.
- Navegacion condicionada por modulo `SECURITY` y `CONFIGURATION`.
- Tests frontend de flujos principales.

La UI debe ser densa, clara y orientada a trabajo, no una landing page disfrazada de ERP.

## Hito 8 - Hardening Sprint 6

Cerrar Sprint 6 con:

- `mvn -B verify`,
- `npm.cmd run test`,
- `npm.cmd run lint`,
- `npm.cmd run build`,
- smoke o E2E autenticado si queda estable en el entorno,
- reporte de hardening,
- reporte de cierre,
- actualizacion de README, ROADMAP y CHANGELOG.

## Criterios de calidad

- No exponer passwords, tokens ni refresh tokens en logs, UI persistente o auditoria.
- No usar `localStorage` como nuevo almacenamiento de refresh token.
- No permitir escalamiento de privilegios por rol.
- No aceptar `tenant_id` libre para operaciones tenant-scoped.
- Mantener Clean Architecture.
- Mantener DTOs y Bean Validation.
- Mantener auditoria para escrituras administrativas.
- Mantener aislamiento tenant.
- Tests de permisos y tenant isolation obligatorios para backend.
- Tests frontend de rutas privadas y sesion obligatorios.
- README, changelog y docs deben actualizarse en cada hito.

## Riesgos y decisiones a cuidar

- Token storage: sessionStorage es aceptable para piloto, pero antes de produccion debe reabrirse decision de cookies HttpOnly o arquitectura equivalente.
- Tenant selection: no convertirlo en endpoint publico de enumeracion de empresas.
- Usuarios: crear passwords iniciales sin email es una solucion de piloto, no un flujo comercial final.
- Roles: no permitir custom roles hasta tener administracion de permisos madura.
- Sucursales/depositos: no borrar fisicamente datos usados por operaciones historicas.
- Frontend: no confiar en ocultar botones como control de seguridad.

## Fuera de alcance

- Recuperacion de password.
- MFA.
- SSO/OAuth externo.
- "Recordarme".
- Cookies HttpOnly para refresh token.
- Invitaciones por email.
- Roles custom.
- Permisos por usuario individual.
- Custom domains/subdominios.
- Login publico tenant-aware completo.
- Upload/CDN/storage de assets.
- Nuevos modulos ERP.

## Siguiente paso recomendado

Avanzar al Hito 2: fundacion frontend de autenticacion.
