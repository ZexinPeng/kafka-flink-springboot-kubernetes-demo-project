package com.demo;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;


public class FlinkConsumer {
    final static StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    final static String inputTopic = "Fruit";
    final static String outputTopic = "Count";
    final static String jobTitle = "FruitCount";
    final static String bootstrapServers = "kafka-service:9092";

    public static void main(String[] args) {
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(bootstrapServers)
                .setTopics(inputTopic)
                .setGroupId("flink-group")
                .setStartingOffsets(OffsetsInitializer.earliest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<String> text = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        DataStream<String> counts = text.flatMap(new Tokenizer())
                // Group by the tuple field "0" and sum up tuple field "1"
                .keyBy(value -> value.f0)
                .sum(1)
                .flatMap(new Reducer());

        KafkaRecordSerializationSchema<String> serializer = KafkaRecordSerializationSchema.builder()
                .setValueSerializationSchema(new SimpleStringSchema())
                .setTopic(outputTopic)
                .build();
        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers(bootstrapServers)
                .setRecordSerializer(serializer)
                .build();
        counts.sinkTo(sink);

        try {
            env.execute(jobTitle);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Implements the string tokenizer that splits sentences into words as a user-defined
     * FlatMapFunction. The function takes a line (String) and splits it into multiple pairs in the
     * form of "(word,1)" ({@code Tuple2<String, Integer>}).
     */
    public static final class Tokenizer
            implements FlatMapFunction<String, Tuple2<String, Integer>> {
        @Override
        public void flatMap(String s, Collector<Tuple2<String, Integer>> collector) throws Exception {
            String[] tokens = s.toLowerCase().split("\\W+");

            // Emit the pairs
            for (String token : tokens) {
                if (token.length() > 0) {
                    collector.collect(new Tuple2<>(token, 1));
                }
            }
        }
    }

    // Implements a simple reducer using FlatMap to
    // reduce the Tuple2 into a single string for
    // writing to kafka topics
    public static final class Reducer
            implements FlatMapFunction<Tuple2<String, Integer>, String> {
        @Override
        public void flatMap(Tuple2<String, Integer> value, Collector<String> out) {
            // Convert the pairs to a string
            // for easy writing to Kafka Topic
            String count = value.f0 + " " + value.f1;
            out.collect(count);
        }
    }
}
