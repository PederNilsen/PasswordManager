-- Create the database
CREATE DATABASE `password_manager` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Select the new database
USE `password_manager`;

-- Create the users table
CREATE TABLE `users` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(255) NOT NULL,
    `hashed_password` VARCHAR(255) NOT NULL,
    `totp_secret` VARCHAR(255),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_login` DATETIME DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `username_unique` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Create the vault_items table
CREATE TABLE `vault_items` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `service_name` VARCHAR(255) NOT NULL,
    `service_username` VARCHAR(255) NOT NULL,
    `encrypted_password` TEXT NOT NULL,
    `notes` TEXT,
    `user_id` INT UNSIGNED NOT NULL,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    FOREIGN KEY (`user_id`) REFERENCES `users`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
