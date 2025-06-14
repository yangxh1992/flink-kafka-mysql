package com.example;

import com.alibaba.fastjson2.JSON;
import lombok.Data;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.util.Objects;

public class FlinkKafkaMySQLJob {

    public static void main(String[] args) throws Exception {
        // 1. 设置 Flink 执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(60000); // 每 60 秒进行一次 Checkpoint

        // 2. 配置 Kafka Source
        KafkaSource<String> kafkaSource = KafkaSource.<String>builder()
//                .setBootstrapServers("129.4.46.112:9092")
//                .setTopics("flink-topic")
//                .setGroupId("flink-group")
                .setBootstrapServers("kafka:9092")
                .setTopics("sensor_readings") // 我们将创建一个名为 sensor_readings 的 Kafka Topic
                .setGroupId("flink-kafka-mysql-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> kafkaStream = env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 3. 解析 JSON 数据并转换为 SensorData 对象
        // 假设 Kafka 中的数据是 JSON 字符串，例如：{"sensorId": "sensor_1", "temperature": 25.5, "timestampMs": 1678886400000}
        DataStream<SensorData> sensorDataStream = kafkaStream.map(new RichMapFunction<String, SensorData>() {

            @Override
            public SensorData map(String value) {
                try {
                    return JSON.parseObject(value, SensorData.class);
                } catch (Exception e) {
                    System.err.println("Error parsing JSON with Fastjson2: " + value + " - " + e.getMessage());
                    return null; // 或者抛出异常，或者根据业务逻辑处理
                }
            }
        }).filter(Objects::nonNull); // 过滤掉解析失败的数据

        // 4. 将数据写入 MySQL
        sensorDataStream.addSink(
                JdbcSink.sink(
                        "INSERT INTO sensor_data (sensor_id, temperature, timestamp_ms) VALUES (?, ?, ?)",
                        (preparedStatement, sensorData) -> {
                            preparedStatement.setString(1, sensorData.getSensorId());
                            preparedStatement.setDouble(2, sensorData.getTemperature());
                            preparedStatement.setLong(3, sensorData.getTimestampMs());
                        },
                        new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
//                                .withUrl("jdbc:mysql://129.4.46.111:3306/flink_data") // MySQL 连接字符串
                                .withUrl("jdbc:mysql://host.docker.internal:3306/flink_data") // MySQL 连接字符串
                                .withDriverName("com.mysql.cj.jdbc.Driver")
                                .withUsername("root")
                                .withPassword("123456")
                                .build()
                )
        );

        // 5. 执行 Flink 作业
        env.execute("Flink Kafka to MySQL Job");
    }

    // SensorData 实体类，用于解析 Kafka 中的 JSON 数据
    @Data
    public static class SensorData {
        private String sensorId;
        private double temperature;
        private long timestampMs;
    }
}