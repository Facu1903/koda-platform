# Visual Assets por Tenant

## Estado

Implementado en Sprint 5 Hito 6.

## Objetivo

Permitir assets visuales configurables por tenant sin abrir la puerta a URLs inseguras, HTML arbitrario, CSS arbitrario ni upload de archivos.

Assets cubiertos:

- logo,
- favicon,
- imagen de login preparada para pantalla futura.

## Regla de seguridad

KODA PLATFORM persiste solo URLs visuales externas seguras:

- `https://` obligatorio,
- host obligatorio,
- sin `data:`,
- sin `javascript:`,
- sin URLs relativas,
- sin usuario/password en la URL,
- sin fragmentos `#...`,
- maximo 2048 caracteres,
- string vacio se normaliza a `null`.

La regla se aplica en backend y frontend. El backend es la autoridad final.

## Backend

`CompanySettingsService` valida `logoUrl`, `faviconUrl` y `loginImageUrl` antes de persistir cambios de configuracion.

Si una URL visual no cumple la politica, la API responde error controlado de request invalido mediante el manejo global existente.

## Frontend

Componentes y utilidades:

- `frontend/src/platform/configuration/visualAssets.ts`: normalizacion, validacion y filtrado seguro.
- `frontend/src/app/App.tsx`: aplica logo del tenant en el shell y favicon runtime.
- `frontend/src/platform/configuration/CompanySettingsWorkspace.tsx`: valida URLs y muestra preview controlada.

Reglas de renderizado:

- Si el logo es valido, se muestra en el shell.
- Si el logo falla al cargar, se vuelve al marcador visual KODA.
- Si el favicon es valido, se aplica al documento.
- Si no hay favicon valido, se mantiene el favicon fallback.
- La vista previa usa `loading="lazy"` y `referrerPolicy="no-referrer"`.
- Si la imagen falla al cargar, se muestra fallback textual.

## Decision tecnica

Se aplica `https://` como regla uniforme desde el inicio, no solo cuando se detecta ambiente productivo. Esto evita diferencias peligrosas entre entornos y obliga a que cualquier excepcion futura sea explicita y aprobada.

No se implementa allowlist de dominios en este hito. Es recomendable evaluarla antes de permitir assets de clientes externos en produccion comercial.

## Fuera de alcance

- upload de archivos,
- storage/CDN propio,
- transformacion u optimizacion de imagenes,
- proxy/cache de imagenes,
- allowlist de dominios,
- login publico tenant-aware completo.

## Validacion

- `mvn -B "-Dtest=CompanySettingsServiceTest" test`: 9 pruebas, 0 fallos.
- `npm.cmd run test`: 17 pruebas, 0 fallos.
- `npm.cmd run lint`: 0 errores.
- `npm.cmd run build`: TypeScript y Vite correctos.
