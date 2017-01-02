/*
 * Copyright 2015 GoDataDriven B.V.
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

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.divolte.server.AvroRecordBuffer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
class ConfluentAvroRecordBufferSerializer extends AvroRecordBufferSerializer {

    private final KafkaAvroSerializer kas;

    public ConfluentAvroRecordBufferSerializer(int schemaId) {
        kas = new KafkaAvroSerializer(new SingleSchemaRegistryClient(schemaId));
    }

    @Override
    public byte[] serialize(String topic, AvroRecordBuffer data) {
        final byte[] avroBytes = super.serialize(topic, data);
        return kas.serialize(topic, avroBytes);
    }

}
