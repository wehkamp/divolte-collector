/*
 * Copyright 2014 GoDataDriven B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.divolte.server.kafka;

import io.divolte.server.AvroRecordBuffer;
import io.divolte.server.DivolteIdentifier;
import io.divolte.server.SchemaRegistry;
import io.divolte.server.config.KafkaConfiguration;
import io.divolte.server.config.KafkaSinkConfiguration;
import io.divolte.server.config.ValidatedConfiguration;
import io.divolte.server.processing.ProcessingPool;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
public class KafkaFlushingPool extends ProcessingPool<KafkaFlusher, AvroRecordBuffer> {

    private final Producer<DivolteIdentifier, AvroRecordBuffer> producer;

    public static KafkaFlushingPool createPool(final ValidatedConfiguration vc, final String name, final SchemaRegistry schemaRegistry) {
        KafkaSinkConfiguration sinkConfiguration = vc.configuration().getSinkConfiguration(name, KafkaSinkConfiguration.class);
        KafkaConfiguration kafkaConfiguration = vc.configuration().global.kafka;
        KafkaProducer<DivolteIdentifier, AvroRecordBuffer> producer = new KafkaProducer<>(kafkaConfiguration.producer,
            new DivolteIdentifierSerializer(
                sinkConfiguration.mode,
                kafkaConfiguration.keySchemaId
            ),
            new AvroRecordBufferSerializer(
                sinkConfiguration.mode,
                schemaRegistry.getSchemaBySinkName(name)
            ));
        return new KafkaFlushingPool(name,
            kafkaConfiguration.threads,
            kafkaConfiguration.bufferSize,
            sinkConfiguration.topic,
            producer
        );
    }

    public KafkaFlushingPool(final String name,
                             final int numThreads,
                             final int maxWriteQueue,
                             final String topic,
                             final Producer<DivolteIdentifier, AvroRecordBuffer> producer ) {
        super(numThreads,
              maxWriteQueue,
              String.format("Kafka Flusher [%s]", Objects.requireNonNull(name)),
              () -> new KafkaFlusher(topic, producer));
        this.producer = Objects.requireNonNull(producer);
    }

    @Override
    public void stop() {
        super.stop();
        producer.close();
    }
}
