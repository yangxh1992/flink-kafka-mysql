USE
flink_data;

INSERT INTO sensor_data (sensor_id, temperature, timestamp_ms)
VALUES ('sensor_A', 20.1, 1678887000000),
       ('sensor_B', 22.5, 1678887001000),
       ('sensor_C', 21.0, 1678887002000),
       ('sensor_A', 20.8, 1678887003000),
       ('sensor_B', 23.0, 1678887004000);