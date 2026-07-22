# Sprint 6 - Functional Baseline

## Estado

Aprobado por Product Owner el 2026-07-22.

## Decision aprobada

Sprint 6 se define como: **Acceso Operativo y Administracion Base del Tenant**.

## Objetivo

Convertir KODA PLATFORM en una aplicacion operable por el cliente piloto: login real, sesion frontend controlada, rutas protegidas y administracion basica de usuarios, roles, sucursales y depositos del tenant.

La verdad sin barniz: hasta Sprint 5 construimos una base SaaS fuerte, pero todavia se opera demasiado desde cimientos tecnicos. Sprint 6 debe cerrar esa brecha. Una plataforma empresarial no puede depender de bootstrap, datos seed o intervencion tecnica para administrar usuarios y estructura operativa.

## Problema a resolver

La plataforma ya tiene:

- autenticacion JWT y refresh tokens,
- Tenant Context seguro,
- roles y permisos,
- tablas de usuarios y membresias,
- tablas de sucursales y depositos,
- capabilities por tenant,
- shell frontend SaaS,
- personalizacion visual y regional por tenant.

Pero el frontend todavia no ofrece un flujo completo de acceso y administracion del tenant. Esto impide que KODA use el sistema como cliente piloto sin asistencia tecnica.

## Principios de Sprint 6

- El backend sigue siendo la fuente de verdad de autenticacion, tenant, permisos y licencias.
- El frontend no decide seguridad; solo mejora experiencia.
- La sesion debe estar centralizada en un `AuthProvider`, no distribuida en lecturas sueltas de storage.
- El access token debe vivir preferentemente en memoria; el refresh token puede persistirse en `sessionStorage` durante Sprint 6 para soportar recarga de pagina sin "recordarme".
- No se aprueba `localStorage` como almacenamiento nuevo para refresh tokens.
- Logout debe revocar refresh token y limpiar estado local.
- Refresh automatico debe intentar una sola vez por request fallida con `401`; no debe entrar en bucles.
- Administrar usuarios no significa administrar plataforma completa.
- No se debe permitir escalamiento de privilegios por UI ni por API.
- Sucursales y depositos son configuracion operativa del tenant, no datos globales.
- Todas las escrituras administrativas deben registrar auditoria.

## Alcance aprobado

Sprint 6 incluye:

- Pantalla real de login.
- Manejo frontend de sesion autenticada.
- Login, refresh y logout desde frontend.
- Estado de usuario autenticado y tenant actual.
- Proteccion de rutas frontend.
- Redireccion a login cuando no hay sesion valida.
- Manejo de expiracion de access token.
- Manejo controlado de `TENANT_SELECTION_REQUIRED`.
- Endpoint o contrato backend seguro para obtener estado de sesion actual si hace falta.
- Administracion tenant-scoped de usuarios.
- Alta de usuario para piloto con password inicial definido por administrador.
- Activacion, suspension/deshabilitacion y actualizacion basica de usuarios del tenant.
- Asignacion controlada de roles tenant.
- Consulta de roles tenant disponibles.
- CRUD tenant-scoped de sucursales.
- CRUD tenant-scoped de depositos.
- Permisos y matriz rol-permiso aplicada por migracion.
- Auditoria de cambios administrativos.
- Tests backend/frontend.
- Primeras pruebas smoke o E2E autenticadas cuando el stack lo permita sin sobredimensionar.
- Documentacion funcional y tecnica.

## Reglas funcionales aprobadas

### Login operativo

Reglas aprobadas:

- La pantalla de login debe usar el tema base KODA.
- No se aplica imagen de login tenant-specific antes de resolver tenant.
- No se aceptan custom domains, subdominios ni tenant discovery publico en Sprint 6.
- El login debe llamar a `/api/v1/auth/login`.
- Si el usuario pertenece a un solo tenant activo, el login entra directo.
- Si el backend responde `TENANT_SELECTION_REQUIRED`, el flujo debe pedir seleccion de empresa sin exponer tenants a usuarios no autenticados.
- Cualquier lista de tenants candidatos solo puede devolverse despues de validar credenciales.
- No se debe revelar si un email existe cuando las credenciales son invalidas.

### Sesion frontend

Reglas aprobadas:

- Crear un `AuthProvider` central.
- Centralizar access token, refresh token, usuario, tenant y expiracion.
- Reemplazar lecturas directas de `localStorage` en clientes API por una fuente de sesion controlada.
- Guardar access token en memoria.
- Guardar refresh token en `sessionStorage` solo para recuperar sesion tras recarga de pagina.
- No implementar "recordarme" en Sprint 6.
- Logout revoca el refresh token en backend cuando exista y limpia estado local.
- Si refresh falla, la sesion queda cerrada.

### Administracion de usuarios

Reglas aprobadas:

- La administracion es tenant-scoped.
- No se crean usuarios de plataforma desde rutas tenant.
- No se asignan roles de plataforma desde rutas tenant.
- Un administrador del tenant puede crear usuarios para su empresa piloto con password inicial.
- La API nunca devuelve passwords.
- La UI nunca debe mostrar ni loguear passwords luego del envio inicial.
- No hay invitacion por email en Sprint 6.
- No hay recuperacion de password en Sprint 6.
- No hay MFA en Sprint 6.
- No hay administracion avanzada de sesiones por dispositivo en Sprint 6.
- Borrar usuario significa deshabilitar o eliminar logicamente la membresia del tenant; no borrar identidad global historica.

### Asignacion de roles

Reglas aprobadas:

- Sprint 6 usa roles tenant del sistema ya existentes.
- No se implementan roles custom editables por cliente en Sprint 6.
- `TENANT_OWNER` puede asignar `TENANT_ADMIN`, `MANAGER`, `SALES_USER`, `STOCK_USER` y `READ_ONLY`.
- `TENANT_ADMIN` puede asignar `MANAGER`, `SALES_USER`, `STOCK_USER` y `READ_ONLY`.
- `TENANT_ADMIN` no puede asignar ni quitar `TENANT_OWNER`.
- Nadie puede quitar el ultimo `TENANT_OWNER` activo del tenant.
- Un usuario no puede elevar sus propios permisos.
- El backend debe validar estas reglas aunque la UI oculte opciones.

### Sucursales y depositos

Reglas aprobadas:

- Las sucursales pertenecen a un tenant.
- Los depositos pertenecen a un tenant y a una sucursal.
- `code` debe ser unico por tenant para sucursales y depositos activos.
- Eliminar significa baja logica o desactivacion, no borrado fisico.
- No se puede desactivar una sucursal si tiene depositos activos, salvo regla futura aprobada.
- No se puede desactivar un deposito con referencias operativas criticas sin validacion de backend.
- Stock, ventas y compras deben seguir usando sucursal/deposito tenant-scoped.

## Matriz de permisos aprobada

| Capacidad | TENANT_OWNER | TENANT_ADMIN | MANAGER | SALES_USER | STOCK_USER | READ_ONLY |
| --- | --- | --- | --- | --- | --- | --- |
| Login y sesion propia | Si | Si | Si | Si | Si | Si |
| Leer usuarios del tenant | Si | Si | Si | No | No | No |
| Crear usuarios del tenant | Si | Si | No | No | No | No |
| Actualizar usuarios del tenant | Si | Si | No | No | No | No |
| Deshabilitar membresias | Si | Si | No | No | No | No |
| Leer roles tenant | Si | Si | Si | No | No | No |
| Asignar roles tenant | Si | Si, limitado | No | No | No | No |
| Leer sucursales | Si | Si | Si | Si | Si | Si |
| Crear sucursales | Si | Si | No | No | No | No |
| Actualizar sucursales | Si | Si | No | No | No | No |
| Desactivar sucursales | Si | Si | No | No | No | No |
| Leer depositos | Si | Si | Si | Si | Si | Si |
| Crear depositos | Si | Si | No | No | No | No |
| Actualizar depositos | Si | Si | No | No | No | No |
| Desactivar depositos | Si | Si | No | No | No | No |

Permisos atomicos esperados:

- `users:read`
- `users:create`
- `users:update`
- `users:delete`
- `roles:read`
- `roles:update`
- `branches:read`
- `branches:create`
- `branches:update`
- `branches:delete`
- `warehouses:read`
- `warehouses:create`
- `warehouses:update`
- `warehouses:delete`

`warehouses:*` debe agregarse por migracion porque todavia no existe como permiso atomico inicial.

## Auditoria

Cada operacion administrativa exitosa debe registrar:

- tenant,
- usuario actor,
- accion,
- recurso,
- identificador del recurso,
- resultado,
- campos modificados relevantes,
- metadata segura de request.

No se deben registrar tokens, refresh tokens, passwords, secretos ni cuerpos completos de request.

## Fuera de alcance de Sprint 6

- Recuperacion de password.
- Cambio de password self-service, salvo que se apruebe como mejora tecnica puntual.
- MFA.
- SSO.
- OAuth externo.
- "Recordarme" persistente.
- Cookies HttpOnly para refresh token.
- Administracion avanzada de sesiones por dispositivo.
- Roles custom editables por cliente.
- Permisos custom por usuario individual.
- Invitaciones por email.
- SMTP.
- Custom domains.
- Subdominios.
- Login publico tenant-aware completo.
- Upload/CDN/storage de assets.
- Row Level Security PostgreSQL.
- Nuevos modulos ERP.
- Mobile.

## Criterios de aceptacion funcional

Sprint 6 se considerara aceptable si:

- Un usuario real puede iniciar sesion desde frontend.
- La sesion se mantiene de forma controlada durante la navegacion.
- El usuario puede cerrar sesion.
- Las rutas privadas quedan protegidas.
- El shell muestra usuario y tenant actual desde sesion real.
- Un owner/admin puede administrar usuarios del tenant segun permisos.
- Un owner/admin puede asignar roles sin escalamiento indebido.
- Un owner/admin puede administrar sucursales y depositos.
- Usuarios operativos pueden leer sucursales y depositos para operar.
- Las escrituras administrativas quedan auditadas.
- Las reglas de permisos aprobadas quedan aplicadas.
- Backend y frontend mantienen aislamiento multiempresa.
- Tests relevantes pasan.
- README, roadmap, changelog y documentacion quedan actualizados.

## Decision

Se aprueba esta base funcional para iniciar Sprint 6.

Cualquier cambio que incorpore MFA, SSO, recuperacion de password, cookies HttpOnly, roles custom, permisos por usuario, invitaciones por email, custom domains, subdominios, tenant discovery publico o altere la matriz de permisos aprobada requiere aprobacion explicita del Product Owner antes de implementarse.
