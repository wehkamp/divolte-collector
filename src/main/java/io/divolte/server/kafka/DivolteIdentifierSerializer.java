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

import avro.shaded.com.google.common.collect.ImmutableList;
import io.divolte.server.DivolteIdentifier;
import io.divolte.server.config.KafkaSinkMode;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;

import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@ParametersAreNonnullByDefault
class DivolteIdentifierSerializer extends DivolteSerializer<DivolteIdentifier> {
    private static final int INITIAL_BUFFER_SIZE = 100;

    private static final char SCHEMA_VERSION = '0';
    private static final Schema schema = Schema.createRecord(ImmutableList.of(
            new Schema.Field("version", Schema.create(Schema.Type.STRING), "Divolte Identifier Version", (Object) null),
            new Schema.Field("timestamp", Schema.create(Schema.Type.LONG), "Timestamp", (Object) null),
            new Schema.Field("id", Schema.create(Schema.Type.STRING), "Message identifier", (Object) null)
        ));

    public DivolteIdentifierSerializer(KafkaSinkMode mode, Optional<Integer> schemaId) {
        super(mode, schemaId);
    }

    @Override
    public void configure(final Map<String, ?> configs, final boolean isKey) {
        // Nothing needed here.
    }

    @Override
    protected ByteBuffer serializeData(final DivolteIdentifier identifier, final KafkaSinkMode mode) {
        switch (mode) {
            case CONFLUENT:
                assert identifier.version == SCHEMA_VERSION;

                final ByteBuffer byteBuffer = ByteBuffer.allocate(INITIAL_BUFFER_SIZE);
                final DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
                final Encoder encoder = EncoderFactory.get().directBinaryEncoder(new ByteBufferOutputStream(byteBuffer), null);
                final GenericRecord record = new GenericData.Record(schema);
                record.put("version", "" + identifier.version);
                record.put("timestamp", identifier.timestamp);
                record.put("id", identifier.getId());

                try {
                    writer.write(record, encoder);
                } catch (IOException ioe) {
                    throw new RuntimeException("Unable to serialize divolte identifier", ioe);
                }

                // Prepare buffer for reading, and store it (read-only).
                byteBuffer.flip();
                return byteBuffer.asReadOnlyBuffer();
            default:
                return ByteBuffer.wrap(identifier.value.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Override
    public void close() {
        // Nothing needed here.
    }

    @ParametersAreNonnullByDefault
    private static final class ByteBufferOutputStream extends OutputStream {
        private final ByteBuffer underlying;

        public ByteBufferOutputStream(final ByteBuffer underlying) {
            this.underlying = Objects.requireNonNull(underlying);
        }

        @Override
        public void write(final int b) throws IOException {
            underlying.put((byte) b);
        }

        @Override
        public void write(final byte[] b, final int off, final int len) throws IOException {
            underlying.put(b, off, len);
        }
    }
}
