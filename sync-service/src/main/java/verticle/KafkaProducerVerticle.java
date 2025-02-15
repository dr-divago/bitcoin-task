package verticle;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import io.vertx.kafka.client.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class KafkaProducerVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(KafkaProducerVerticle.class);

  @Override
  public void start(Promise<Void> promise)  {
        initVerticle(vertx);
        promise.complete();
  }

  private void initVerticle(Vertx vertx) {
    Map<String, String> configMap = new HashMap<>();
    configMap.put("bootstrap.servers", config().getString("bootstrapServers"));
    configMap.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
    configMap.put("value.serializer", "io.vertx.kafka.client.serialization.JsonObjectSerializer");
    configMap.put("acks", "1");
    KafkaProducer<String, JsonObject> producer = KafkaProducer.create(vertx, configMap);
    receiveMsgFromEventBusAndSendToKafka(producer, config().getString("topic"));
  }

  private void receiveMsgFromEventBusAndSendToKafka(KafkaProducer<String, JsonObject> producer, String topic){
    vertx.eventBus().<JsonObject>consumer("kafka.bitcoin.price", message -> {
      logger.info("receiving data from event bus {}", "kafka.bitcoin.price");
      produceToKafka(producer, topic, message.body());
    });
  }

  private void produceToKafka(KafkaProducer<String, JsonObject> producer, String topic, JsonObject message) {
    KafkaProducerRecord<String, JsonObject> record = KafkaProducerRecord.create(topic, message);
    producer.send(record, handler -> {
      if (handler.succeeded()) {
        RecordMetadata recordMetadata = handler.result();
        logger.info("Message " + record.value() + " written on topic=" + recordMetadata.getTopic() +
          ", partition=" + recordMetadata.getPartition() +
          ", offset=" + recordMetadata.getOffset());
      } else if(handler.failed()) {
        logger.error("error receive record from kafka: {}", handler.cause());
      }
    });
  }
}
