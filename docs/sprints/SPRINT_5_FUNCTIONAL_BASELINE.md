# Sprint 5 - Functional Baseline

## Estado

Aprobado por Product Owner el 2026-07-22.

## Decision aprobada

Sprint 5 se define como: **Personalizacion Avanzada por Tenant**.

## Objetivo

Convertir la configuracion visual y regional existente de cada empresa en una experiencia real, consistente y segura dentro de KODA PLATFORM.

Sprint 5 no debe ser un ejercicio cosmetico. La personalizacion por tenant tiene impacto directo en identidad de marca, usabilidad diaria, formato de datos, soporte, seguridad de assets y escalabilidad del frontend. Si se hace mal, cada cliente termina pidiendo su propia excepcion y la plataforma se convierte en una fabrica de deuda con lazo naranja.

La meta es clara: cada empresa debe poder tener identidad visual y configuracion regional propia sin modificar codigo, sin romper compatibilidad y sin comprometer seguridad.

## Problema a resolver

Sprint 1 ya creo una API tenant-scoped para configuracion de empresa con:

- logo,
- favicon,
- imagen de login,
- color primario,
- color secundario,
- modo de tema,
- locale,
- moneda,
- zona horaria,
- formatos de fecha, hora, numero y moneda.

Pero la aplicacion web todavia usa un tema KODA fijo y textos regionales hardcodeados. Eso alcanza para una base tecnica; no alcanza para un SaaS comercial donde cada empresa debe operar con su propia identidad.

## Principios de Sprint 5

- La personalizacion es por tenant, no por instalacion.
- La configuracion debe aplicarse sin deploy, build ni cambio de codigo.
- El tema visual debe ser controlado, no CSS libre.
- La experiencia debe mantener identidad KODA PLATFORM aunque cada empresa tenga marca propia.
- La configuracion regional debe afectar presentacion, no corromper datos persistidos.
- Monedas, fechas y numeros se guardan como datos normalizados; se muestran segun configuracion del tenant.
- Assets externos deben tener reglas de seguridad estrictas.
- Leer la apariencia runtime no debe requerir permiso administrativo.
- Editar configuracion si debe requerir permisos explicitos.
- La UI debe tener fallback seguro si la configuracion del tenant esta incompleta o temporalmente no disponible.

## Alcance aprobado

Sprint 5 incluye:

- Perfil runtime de empresa para que la UI obtenga branding y configuracion regional no sensible.
- Tema Material UI dinamico por tenant.
- Aplicacion de logo, color primario, color secundario y modo claro/oscuro/sistema.
- Favicon configurable por tenant cuando exista URL valida.
- Utilidades frontend para formato de fecha, hora, numero y moneda segun tenant.
- Pantalla administrativa de configuracion de empresa.
- Edicion controlada de branding y configuracion regional.
- Vista previa de cambios antes de guardar.
- Control optimista por version en la UI.
- Manejo de errores de validacion y conflicto de version.
- Auditoria de cambios de configuracion.
- Documentacion funcional y tecnica.
- Tests backend/frontend correspondientes.

## Reglas funcionales aprobadas

### Perfil runtime de empresa

La UI necesita una lectura liviana de configuracion visual/regional para todos los usuarios autenticados del tenant.

Regla aprobada:

- Crear o exponer un contrato runtime tenant-scoped con datos no sensibles de presentacion.
- Esta lectura debe requerir autenticacion y Tenant Context valido.
- No debe requerir `company_settings:read`, porque todos los usuarios necesitan renderizar la UI con identidad de su empresa.
- No debe devolver informacion fiscal sensible ni datos internos de administracion.

El endpoint administrativo `/api/v1/company/settings` mantiene permisos especificos para lectura administrativa y actualizacion.

### Tema visual

Reglas aprobadas:

- `themeMode` acepta `dark`, `light` y `system`.
- El modo por defecto sigue siendo `dark`.
- `primaryColor` es obligatorio y debe usar `#RRGGBB`.
- `secondaryColor` es opcional y debe usar `#RRGGBB` cuando exista.
- Si un color no permite contraste minimo razonable, la UI debe aplicar fallback seguro o mostrar validacion.
- La plataforma no permite CSS arbitrario por tenant.
- No se permite HTML custom por tenant.

### Assets visuales

Reglas aprobadas:

- `logoUrl`, `faviconUrl` y `loginImageUrl` se manejan como URL externas validadas.
- En produccion deben ser `https://`.
- No se aceptan `javascript:`, `data:`, URLs vacias ni esquemas inseguros.
- Upload de archivos, storage propio, CDN, optimizacion de imagenes y antivirus quedan fuera de Sprint 5.
- Si un asset no carga, la UI debe usar fallback visual estable.

### Configuracion regional

Reglas aprobadas:

- `defaultLocale` define idioma/formato regional principal.
- `defaultCurrency` debe ser ISO 4217.
- `timeZone` debe ser un identificador valido de zona horaria.
- `dateFormat` y `timeFormat` se validan en backend.
- El frontend debe formatear fechas y monedas usando configuracion del tenant.
- Las operaciones comerciales siguen guardando moneda como codigo normalizado, no como texto formateado.

### Permisos

Matriz aprobada para Sprint 5:

| Capacidad | TENANT_OWNER | TENANT_ADMIN | MANAGER | SALES_USER | STOCK_USER | READ_ONLY |
| --- | --- | --- | --- | --- | --- | --- |
| Ver apariencia runtime | Si | Si | Si | Si | Si | Si |
| Leer configuracion administrativa | Si | Si | Si | No | No | No |
| Actualizar configuracion | Si | Si | No | No | No | No |

Esta matriz queda aprobada funcionalmente y debe aplicarse por migracion durante Sprint 5 antes de habilitar la UI administrativa de actualizacion.

### Auditoria

Cada actualizacion exitosa de configuracion debe registrar:

- tenant,
- usuario actor,
- accion `company_settings.update`,
- campos modificados,
- version anterior y nueva,
- resultado.

No se deben registrar tokens, secretos ni cuerpos completos de request.

### Login image

Sprint 5 puede editar y validar `loginImageUrl`, pero la aplicacion de imagen de login tenant-specific antes de autenticar queda limitada por una decision futura: como se resuelve el tenant antes del login.

Opciones futuras:

- subdominio por tenant,
- dominio custom,
- selector de empresa,
- codigo de empresa previo al login.

Hasta aprobar esa estrategia, la imagen de login puede quedar modelada, administrada y preparada, pero no debe forzar una arquitectura de login prematura.

## Fuera de alcance de Sprint 5

- Upload de archivos.
- Storage propio de assets.
- CDN.
- Antivirus o procesamiento de imagenes.
- Custom domains.
- Subdominios por tenant.
- Login publico tenant-aware completo.
- Preferencias visuales por usuario individual.
- Traduccion completa de textos de producto.
- Motor i18n avanzado.
- CSS/HTML arbitrario por cliente.
- White-label total de KODA PLATFORM.
- Billing o cambios de licencias.
- Nuevos modulos ERP.
- Aplicacion mobile.

## Criterios de aceptacion funcional

Sprint 5 se considerara aceptable si:

- La UI puede cargar configuracion runtime del tenant autenticado.
- El tema visual se genera dinamicamente desde la configuracion del tenant.
- Logo, colores y modo de tema se aplican con fallbacks seguros.
- Fechas, horas, numeros y monedas se formatean segun configuracion regional.
- Existe pantalla administrativa para consultar y actualizar configuracion de empresa.
- La actualizacion usa version optimista.
- Los errores de validacion y conflicto se muestran de forma controlada.
- Las reglas de permisos aprobadas quedan aplicadas.
- Los cambios quedan auditados.
- Backend y frontend mantienen aislamiento multiempresa.
- Tests relevantes pasan.
- README, roadmap, changelog y documentacion quedan actualizados.

## Decision

Se aprueba esta base funcional para iniciar Sprint 5.

Cualquier cambio que incorpore uploads, storage/CDN, custom domains, subdominios, login publico tenant-aware completo, preferencias visuales por usuario, CSS/HTML arbitrario o altere permisos aprobados requiere aprobacion explicita del Product Owner antes de implementarse.
