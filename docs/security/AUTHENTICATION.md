# Authentication

## Objetivo

El Hito 4 agrega la base de autenticacion productiva de KODA PLATFORM: login, access tokens JWT, refresh tokens opacos, roles, permisos y resolucion de tenant desde identidad autenticada.

La regla central se mantiene: el frontend no decide el tenant efectivo para operar datos tenant-scoped. El backend emite tokens solo para tenants a los que el usuario pertenece activamente.

## Endpoints

Base path: `/api/v1/auth`.

- `POST /login`: valida email y password, resuelve tenant y emite access token + refresh token.
- `POST /refresh`: valida refresh token activo, rota el refresh token y emite un access token nuevo.
- `POST /logout`: revoca el refresh token informado.

## Login

Request:

```json
{
  "email": "owner@koda.local",
  "password": "password-elegido",
  "tenantId": "00000000-0000-4000-8000-000000000001"
}
```

`tenantId` es opcional cuando el usuario pertenece a un solo tenant activo. Si pertenece a mas de uno, el backend responde `409 TENANT_SELECTION_REQUIRED` y el cliente debe reenviar el login indicando tenant.

Response:

```json
{
  "tokenType": "Bearer",
  "accessToken": "...",
  "refreshToken": "...",
  "expiresInSeconds": 900,
  "user": {
    "id": "...",
    "email": "owner@koda.local",
    "displayName": "KODA Owner",
    "roles": ["TENANT_OWNER"],
    "permissions": ["products:read"]
  },
  "tenant": {
    "id": "00000000-0000-4000-8000-000000000001",
    "name": "KODA"
  }
}
```

## Access token

- Formato: JWT firmado con HS256.
- TTL por defecto: 15 minutos.
- Issuer por defecto: `koda-platform`.
- Claims principales: `sub`, `tenant_id`, `tenant_name`, `email`, `display_name`, `roles`, `permissions`, `platform_admin`.
- El token se convierte a `KodaAuthenticatedPrincipal`, que alimenta el `TenantContextAuthenticationFilter`.

## Refresh token

- El refresh token es opaco, aleatorio y no es JWT.
- Se guarda en base solo como hash SHA-256.
- TTL por defecto: 30 dias.
- Cada refresh rota el token anterior y persiste uno nuevo.
- Logout revoca el refresh token informado.

## Configuracion obligatoria

`KODA_JWT_SECRET` es obligatorio y debe tener al menos 32 bytes para HS256. No existe secreto por defecto. Esto es intencional: una plataforma SaaS no puede arrancar con una llave conocida.

Variables principales:

```env
KODA_JWT_ISSUER=koda-platform
KODA_JWT_SECRET=
KODA_ACCESS_TOKEN_TTL=15m
KODA_REFRESH_TOKEN_TTL=30d
```

## Bootstrap inicial

El sistema no crea usuarios reales por defecto.

Para desarrollo local o primer arranque controlado puede usarse bootstrap opcional:

```env
KODA_BOOTSTRAP_OWNER_EMAIL=owner@koda.local
KODA_BOOTSTRAP_OWNER_PASSWORD=password-local-seguro
KODA_BOOTSTRAP_OWNER_DISPLAY_NAME=KODA Owner
KODA_BOOTSTRAP_TENANT_ID=00000000-0000-4000-8000-000000000001
```

Reglas:

- Email y password deben estar ambos definidos o ambos vacios.
- Password minimo: 12 caracteres.
- El tenant debe existir.
- Si el usuario ya existe, no se pisa su password si ya tenia una.
- Se asegura membresia activa y rol `TENANT_OWNER` para el tenant indicado.

## Errores

- `400 MALFORMED_REQUEST_BODY`: JSON malformado o cuerpo ilegible.
- `400 VALIDATION_ERROR`: payload invalido.
- `400 INVALID_REQUEST`: tenant id u otro dato con formato invalido.
- `401 AUTHENTICATION_FAILED`: credenciales invalidas o usuario sin acceso activo.
- `401 INVALID_REFRESH_TOKEN`: refresh token inexistente, vencido o revocado.
- `409 TENANT_SELECTION_REQUIRED`: el usuario tiene multiples tenants activos y debe elegir uno.

## Alcance del Hito 4

Incluido:

- Login JWT stateless.
- Refresh tokens persistentes, opacos, hasheados y rotados.
- Logout por revocacion de refresh token.
- Password hashing con BCrypt.
- Principal autenticado de KODA integrado con Tenant Context.
- Bootstrap inicial opt-in sin credenciales hardcodeadas.
- Tests unitarios de autenticacion y conversion JWT.

No incluido:

- Recuperacion de password.
- MFA.
- Bloqueo por intentos fallidos.
- Politica avanzada de sesiones por dispositivo.
- Rotacion de llaves/JWKS o RS256.
- Row Level Security PostgreSQL.
