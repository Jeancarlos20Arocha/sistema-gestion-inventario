-- ============================================================
-- SGM - Sistema de Gestion de Inventario, POS y Usuarios
-- Esquema MySQL con integridad referencial y CHECK constraints
-- Compatible: MySQL 8.0.x / MariaDB 10.4+
-- Uso Docker: se ejecuta solo en /docker-entrypoint-initdb.d/
-- Uso local:  CREATE DATABASE sgm_db CHARACTER SET utf8mb4; USE sgm_db; SOURCE schema.sql;
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ------------------------------------------------------------
-- Tabla: users (seguridad: intentos fallidos y bloqueo)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sale_details;
DROP TABLE IF EXISTS sales;
DROP TABLE IF EXISTS stock_movements;
DROP TABLE IF EXISTS batches;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS security_questions;
DROP TABLE IF EXISTS machine_terminals;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    username       VARCHAR(50)   NOT NULL,
    password       VARCHAR(100)  NOT NULL,
    full_name      VARCHAR(120)  NOT NULL,
    email          VARCHAR(120)  NOT NULL,
    role           VARCHAR(20)   NOT NULL,
    enabled        BOOLEAN       NOT NULL DEFAULT TRUE,
    account_locked BOOLEAN       NOT NULL DEFAULT FALSE,
    lock_expires_at DATETIME     NULL,
    failed_attempts INT          NOT NULL DEFAULT 0,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_role (role),
    CONSTRAINT ck_users_failed_attempts CHECK (failed_attempts >= 0),
    CONSTRAINT ck_users_role CHECK (role IN ('ROLE_ADMIN', 'ROLE_USER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: security_questions (preguntas y respuestas hasheadas BCrypt)
-- ------------------------------------------------------------
CREATE TABLE security_questions (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    question     VARCHAR(200) NOT NULL,
    answer_hash  VARCHAR(100) NOT NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_secq_user_question (user_id, question),
    CONSTRAINT fk_secq_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: machine_terminals (identificador de maquina para auditoria)
-- ------------------------------------------------------------
CREATE TABLE machine_terminals (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    machine_id VARCHAR(64)  NOT NULL,
    name       VARCHAR(100) NOT NULL,
    last_seen  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_machine_id (machine_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: categories
-- ------------------------------------------------------------
CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255) NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: items (CHECKs de precios y stock)
-- ------------------------------------------------------------
CREATE TABLE items (
    id          BIGINT         NOT NULL AUTO_INCREMENT,
    barcode     VARCHAR(60)    NOT NULL,
    name        VARCHAR(120)   NOT NULL,
    description VARCHAR(255)   NULL,
    category_id BIGINT         NOT NULL,
    unit        VARCHAR(20)    NOT NULL DEFAULT 'UND',
    cost        DECIMAL(12, 2) NOT NULL,
    price       DECIMAL(12, 2) NOT NULL,
    min_stock   DECIMAL(12, 2) NOT NULL DEFAULT 0,
    stock       DECIMAL(12, 2) NOT NULL DEFAULT 0,
    active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_items_barcode (barcode),
    KEY idx_items_name (name),
    KEY idx_items_category (category_id),
    CONSTRAINT fk_items_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_items_cost CHECK (cost > 0),
    CONSTRAINT ck_items_price CHECK (price > 0),
    CONSTRAINT ck_items_min_stock CHECK (min_stock >= 0),
    CONSTRAINT ck_items_stock CHECK (stock >= 0),
    CONSTRAINT ck_items_price_gt_cost CHECK (price >= cost)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: batches (lotes por articulo)
-- ------------------------------------------------------------
CREATE TABLE batches (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    item_id       BIGINT         NOT NULL,
    batch_code    VARCHAR(60)    NOT NULL,
    provider      VARCHAR(120)   NULL,
    quantity      DECIMAL(12, 2) NOT NULL,
    cost          DECIMAL(12, 2) NULL,
    expiry_date   DATE           NULL,
    received_date DATE           NOT NULL,
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_batch_item_code (item_id, batch_code),
    CONSTRAINT fk_batches_item FOREIGN KEY (item_id)
        REFERENCES items (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_batches_quantity CHECK (quantity > 0),
    CONSTRAINT ck_batches_cost CHECK (cost IS NULL OR cost > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: stock_movements (auditoria de movimientos, machine_id)
-- ------------------------------------------------------------
CREATE TABLE stock_movements (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    item_id       BIGINT         NOT NULL,
    batch_id      BIGINT         NULL,
    user_id       BIGINT         NOT NULL,
    machine_id    BIGINT         NULL,
    movement_type VARCHAR(20)    NOT NULL,
    quantity      DECIMAL(12, 2) NOT NULL,
    note          VARCHAR(255)   NULL,
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_mov_item (item_id),
    KEY idx_mov_date (created_at),
    CONSTRAINT fk_mov_item FOREIGN KEY (item_id)
        REFERENCES items (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_mov_batch FOREIGN KEY (batch_id)
        REFERENCES batches (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT fk_mov_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_mov_machine FOREIGN KEY (machine_id)
        REFERENCES machine_terminals (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_mov_type CHECK (movement_type IN ('ENTRADA', 'SALIDA', 'AJUSTE', 'DEVOLUCION')),
    CONSTRAINT ck_mov_qty CHECK (quantity > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: sales (ventas POS con auditoria de maquina y cajero)
-- ------------------------------------------------------------
CREATE TABLE sales (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    user_id        BIGINT         NOT NULL,
    machine_id     BIGINT         NULL,
    total          DECIMAL(12, 2) NOT NULL,
    cash_received  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    change_amount  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    status         VARCHAR(20)    NOT NULL DEFAULT 'COMPLETADA',
    created_at     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_sale_date (created_at),
    KEY idx_sale_user (user_id),
    CONSTRAINT fk_sale_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_sale_machine FOREIGN KEY (machine_id)
        REFERENCES machine_terminals (id) ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT ck_sale_total CHECK (total > 0),
    CONSTRAINT ck_sale_cash CHECK (cash_received >= 0),
    CONSTRAINT ck_sale_change CHECK (change_amount >= 0),
    CONSTRAINT ck_sale_status CHECK (status IN ('COMPLETADA', 'ANULADA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- Tabla: sale_details
-- ------------------------------------------------------------
CREATE TABLE sale_details (
    id       BIGINT         NOT NULL AUTO_INCREMENT,
    sale_id  BIGINT         NOT NULL,
    item_id  BIGINT         NOT NULL,
    quantity DECIMAL(12, 2) NOT NULL,
    price    DECIMAL(12, 2) NOT NULL,
    subtotal DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_saledetail_sale_item (sale_id, item_id),
    KEY idx_saledetail_item (item_id),
    CONSTRAINT fk_sd_sale FOREIGN KEY (sale_id)
        REFERENCES sales (id) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_sd_item FOREIGN KEY (item_id)
        REFERENCES items (id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT ck_sd_qty CHECK (quantity > 0),
    CONSTRAINT ck_sd_price CHECK (price > 0),
    CONSTRAINT ck_sd_subtotal CHECK (subtotal > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;