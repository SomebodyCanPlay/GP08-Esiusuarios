-- ============================================================
-- Script para crear la base de datos de esiusuarios en MySQL
-- Ejecútalo en MySQL Workbench: abre una Query y pulsa el rayo (Execute)
-- ============================================================

-- 1. Crear la base de datos (si no existe ya)
CREATE DATABASE IF NOT EXISTS esiusuarios;

-- 2. Seleccionar esa base de datos para trabajar con ella
USE esiusuarios;

-- 3. Tabla de usuarios
--    Aquí se guardará cada persona que se registre
CREATE TABLE IF NOT EXISTS usuario (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,  -- ID autoincremental (Spring lo gestiona)
    email         VARCHAR(255) NOT NULL UNIQUE,        -- El correo (único, no puede repetirse)
    password_hash VARCHAR(255) NOT NULL,               -- Contraseña cifrada con BCrypt (NUNCA en texto plano)
    nombre        VARCHAR(100) NOT NULL,               -- Nombre del usuario
    token_sesion  VARCHAR(100) NULL,                   -- Token generado al hacer login (para pasar a esientradas)
    activo        TINYINT(1) NOT NULL DEFAULT 1        -- 1=cuenta activa, 0=cancelada
);

-- 4. Tabla de tokens de recuperación de contraseña
--    Cuando alguien dice "olvidé mi contraseña", se genera un código aquí.
--    Ese código se envía por email con Brevo y caduca en 15 minutos.
CREATE TABLE IF NOT EXISTS token_recuperacion (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,                  -- A qué usuario pertenece este token
    token      VARCHAR(100) NOT NULL UNIQUE,           -- El código aleatorio que se envía por email
    expira_en  BIGINT NOT NULL                         -- Timestamp en milisegundos de cuando caduca
);

-- ============================================================
-- COMPROBACIÓN: ejecuta esto para ver si las tablas se crearon
-- SELECT * FROM usuario;
-- SELECT * FROM token_recuperacion;
-- ============================================================

