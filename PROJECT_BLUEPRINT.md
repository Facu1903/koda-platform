# KODA PLATFORM - Project Blueprint

## 1. Proposito

KODA PLATFORM es una plataforma SaaS profesional de gestion empresarial, multiempresa, modular y extensible. Su primer producto sera KODA ERP, utilizado inicialmente por KODA como cliente piloto, pero disenado desde el dia cero como producto comercial para multiples empresas.

Este proyecto no debe tratarse como un prototipo, ejercicio academico ni "ERP rapido". La arquitectura debe poder evolucionar durante al menos 10 anos, soportando miles de empresas, multiples productos, cientos de modulos y millones de registros.

## 2. Vision de producto

KODA PLATFORM sera el contenedor comun para diferentes productos empresariales:

- KODA ERP
- KODA POS
- KODA CRM
- KODA BI
- KODA AI
- KODA Mobile
- KODA API

KODA ERP es el primer producto, no el limite de la plataforma. Toda decision tecnica debe evaluarse con esta pregunta:

> Esto seguira siendo valido cuando existan 10.000 empresas, 100 modulos y millones de registros?

Si la respuesta es no, la decision debe revisarse antes de implementarse.

## 3. Roles y autoridad

### Product Owner

El dueno del producto define reglas funcionales, prioridades de negocio, alcance por sprint y criterios de aceptacion funcional.

### Arquitecto Funcional y de Negocio

ChatGPT actua como apoyo en arquitectura funcional, procesos de negocio, definicion de modulos, reglas y evolucion comercial del producto.

### Senior Software Engineer y Arquitecto Tecnico

Codex actua como responsable tecnico de implementacion, arquitectura de software, calidad, seguridad, mantenibilidad, testing y consistencia del codigo.

## 4. Reglas de gobierno

- No se modifican reglas funcionales sin aprobacion del Product Owner.
- Las mejoras tecnicas relevantes deben proponerse antes de implementarse si afectan arquitectura, alcance, compatibilidad, seguridad o experiencia de usuario.
- No se escribe codigo de negocio antes de completar Sprint 0.
- Cada sprint debe entregar un sistema funcional, compilable, ejecutable y verificable.
- Cada cambio relevante debe actualizar documentacion, changelog y diagramas cuando corresponda.
- La velocidad nunca justifica deuda estructural en componentes centrales.

## 5. Principios estrategicos

1. Plataforma antes que ERP aislado.
2. Multiempresa real, no filtrado improvisado.
3. Modularidad contractual, no carpetas decorativas.
4. Seguridad por defecto.
5. API estable y versionable.
6. Compatibilidad hacia atras como objetivo explicito.
7. Observabilidad desde el inicio.
8. Testing como requisito de entrega, no tarea posterior.
9. Datos auditables.
10. Personalizacion por configuracion, nunca por forks de codigo.

## 6. Stack obligatorio

### Backend

- Java 21
- Spring Boot 3
- Spring Security
- JWT
- Spring Data JPA
- Hibernate
- Flyway
- Maven

### Base de datos

- PostgreSQL

### Frontend

- React
- TypeScript
- Vite
- Material UI

### Infraestructura

- Docker
- Docker Compose

### Versionado

- Git
- GitHub

## 7. Arquitectura obligatoria

Se aplicara Clean Architecture con separacion estricta:

- Domain
- Application
- Infrastructure
- API

La arquitectura debe respetar SOLID, Repository Pattern, Service Layer, DTOs, validaciones con Bean Validation, manejo global de excepciones, auditoria, logs estructurados y testing desde el inicio.

## 8. Modelo SaaS multiempresa

Cada empresa operara como tenant independiente. Ninguna empresa podra acceder a datos de otra.

Cada tenant podra tener:

- Nombre comercial
- Razon social
- Logo
- Colores corporativos
- Favicon
- Moneda
- Idioma
- Zona horaria
- Pais
- Provincias
- Sucursales
- Depositos
- Usuarios
- Roles
- Permisos
- Clientes
- Proveedores
- Productos
- Configuraciones propias

La estrategia inicial recomendada es base de datos compartida con columna `tenant_id`, aislamiento obligatorio en capa de aplicacion y refuerzo progresivo con Row Level Security de PostgreSQL para tablas sensibles o de alto riesgo. Para clientes enterprise podra evaluarse aislamiento dedicado en el futuro.

## 9. Personalizacion por tenant

Cada empresa podra configurar sin cambios de codigo:

- Logo
- Paleta institucional
- Tema claro u oscuro
- Imagen de login
- Favicon
- Formato de moneda
- Formato de fechas
- Idioma
- Zona horaria
- Configuracion regional

La UI debe tomar estas configuraciones desde servicios de plataforma y aplicarlas dinamicamente.

## 10. Arquitectura modular

Los modulos deben poder activarse o desactivarse por licencia, tenant y producto.

Modulos iniciales previstos:

- Seguridad
- Auditoria
- Configuracion
- Empresas
- Sucursales
- Usuarios
- Roles
- Permisos
- Productos
- Marcas
- Categorias
- Presentaciones
- Unidades de medida
- Stock
- Ventas
- Caja
- Compras
- Clientes
- Mascotas
- Proveedores
- Reportes
- Dashboard
- IA

La habilitacion de modulos debe resolverse por un modelo de licencias/entitlements, no por condicionales dispersos en el codigo.

## 11. Reglas de interfaz

La interfaz debe ser moderna, minimalista, rapida y optimizada para uso intensivo.

Tema base:

- Oscuro por defecto
- Negro
- Blanco
- Naranja `#F6862B`

La interfaz no debe parecer una landing page de marketing. Debe sentirse como una herramienta profesional de trabajo diario: densa cuando corresponde, clara, veloz y sin adornos inutiles.

## 12. Alcance de Sprint 0

Sprint 0 entrega la constitucion del proyecto:

- `PROJECT_BLUEPRINT.md`
- `ARCHITECTURE.md`
- `CODING_STANDARDS.md`
- `ROADMAP.md`
- `CONTRIBUTING.md`
- `CHANGELOG.md`
- `README.md`
- `AGENTS.md`

Tambien define:

- Arquitectura completa
- Estructura de carpetas
- Convenciones de nombres
- Estrategia Git
- Versionado
- Testing
- Seguridad
- Flujo de desarrollo
- Criterios de calidad

## 13. Alcance de Sprint 1

Sprint 1 debe construir la infraestructura inicial:

- Autenticacion
- Usuarios
- Roles
- Permisos
- Empresas/Tenants
- Sucursales
- Configuracion de empresa
- Productos
- Marcas
- Categorias
- Presentaciones
- Unidades de medida
- CRUD completo
- Movimientos de stock
- Auditoria
- Tests

Todo debe compilar y ejecutarse correctamente antes de continuar.

### Advertencia estrategica

Sprint 1 es ambicioso. No se cambia el alcance sin aprobacion del Product Owner, pero tecnicamente debera ejecutarse por hitos internos verificables para evitar construir un castillo de naipes con logo naranja.

## 14. Criterios de exito

Un incremento solo se considera terminado si:

- Compila.
- Ejecuta localmente con Docker Compose cuando aplique.
- Tiene migraciones versionadas.
- Tiene tests relevantes.
- Respeta aislamiento multiempresa.
- No expone entidades JPA directamente.
- Tiene manejo de errores consistente.
- Registra eventos relevantes con logs estructurados.
- Actualiza documentacion y changelog.
- No introduce deuda arquitectonica sin documentarla.

