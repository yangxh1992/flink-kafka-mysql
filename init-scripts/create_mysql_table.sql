CREATE DATABASE IF NOT EXISTS flink_data;

USE flink_data;

CREATE TABLE IF NOT EXISTS sensor_data (
                                           id INT AUTO_INCREMENT PRIMARY KEY,
                                           sensor_id VARCHAR(255) NOT NULL,
    temperature DOUBLE NOT NULL,
    timestamp_ms BIGINT NOT NULL
    );