/*
 * Copyright (C) 2023 Dremio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.projectnessie.versioned.storage.serialize;

import static java.util.Collections.emptyList;
import static org.projectnessie.versioned.storage.common.indexes.StoreIndexes.emptyImmutableIndex;
import static org.projectnessie.versioned.storage.common.objtypes.CommitHeaders.EMPTY_COMMIT_HEADERS;
import static org.projectnessie.versioned.storage.common.objtypes.CommitHeaders.newCommitHeaders;
import static org.projectnessie.versioned.storage.common.objtypes.CommitOp.COMMIT_OP_SERIALIZER;
import static org.projectnessie.versioned.storage.common.objtypes.ContentValueObj.contentValue;
import static org.projectnessie.versioned.storage.common.objtypes.IndexObj.index;
import static org.projectnessie.versioned.storage.common.objtypes.IndexSegmentsObj.indexSegments;
import static org.projectnessie.versioned.storage.common.objtypes.RefObj.ref;
import static org.projectnessie.versioned.storage.common.objtypes.StringObj.stringData;
import static org.projectnessie.versioned.storage.common.objtypes.TagObj.tag;
import static org.projectnessie.versioned.storage.common.persist.ObjId.EMPTY_OBJ_ID;
import static org.projectnessie.versioned.storage.common.persist.ObjId.randomObjId;
import static org.projectnessie.versioned.storage.common.persist.Reference.reference;
import static org.projectnessie.versioned.storage.serialize.ProtoSerialization.deserializeObj;
import static org.projectnessie.versioned.storage.serialize.ProtoSerialization.deserializeReference;
import static org.projectnessie.versioned.storage.serialize.ProtoSerialization.serializeObj;
import static org.projectnessie.versioned.storage.serialize.ProtoSerialization.serializeReference;

import java.nio.ByteBuffer;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.projectnessie.nessie.relocated.protobuf.ByteString;
import org.projectnessie.versioned.storage.common.objtypes.CommitObj;
import org.projectnessie.versioned.storage.common.objtypes.Compression;
import org.projectnessie.versioned.storage.common.persist.Obj;
import org.projectnessie.versioned.storage.common.persist.Reference;

@ExtendWith(SoftAssertionsExtension.class)
public class TestProtoSerialization {
  @InjectSoftAssertions protected SoftAssertions soft;

  @ParameterizedTest
  @MethodSource("references")
  void references(Reference reference) {
    byte[] serialized = serializeReference(reference);
    Reference deserialized = deserializeReference(serialized);
    byte[] reserialized = serializeReference(deserialized);
    soft.assertThat(deserialized).isEqualTo(reference);
    soft.assertThat(serialized).isEqualTo(reserialized);
  }

  @ParameterizedTest
  @MethodSource("objs")
  void objs(Obj obj) throws Exception {
    byte[] serialized = serializeObj(obj, Integer.MAX_VALUE, Integer.MAX_VALUE);
    Obj deserialized = deserializeObj(obj.id(), serialized);
    Obj deserializedByteBuffer = deserializeObj(obj.id(), ByteBuffer.wrap(serialized));
    byte[] reserialized = serializeObj(deserialized, Integer.MAX_VALUE, Integer.MAX_VALUE);
    soft.assertThat(deserialized).isEqualTo(obj).isEqualTo(deserializedByteBuffer);
    soft.assertThat(serialized).isEqualTo(reserialized);
  }

  static Stream<Reference> references() {
    return Stream.of(
        reference("a", EMPTY_OBJ_ID, false, 0L, null),
        reference("b", randomObjId(), false, 0L, null),
        reference("c", randomObjId(), true, 0L, null),
        reference("d", EMPTY_OBJ_ID, false, 42L, null),
        reference("e", randomObjId(), false, 42L, null),
        reference("f", randomObjId(), true, 42L, null),
        reference("g", EMPTY_OBJ_ID, false, 0L, randomObjId()),
        reference("h", randomObjId(), false, 0L, randomObjId()),
        reference("i", randomObjId(), true, 0L, randomObjId()),
        reference("j", EMPTY_OBJ_ID, false, 42L, randomObjId()),
        reference("k", randomObjId(), false, 42L, randomObjId()),
        reference("l", randomObjId(), true, 42L, randomObjId()));
  }

  static Stream<Obj> objs() {
    return Stream.of(
        ref(randomObjId(), "hello", randomObjId(), 42L, randomObjId()),
        CommitObj.commitBuilder()
            .id(randomObjId())
            .seq(1L)
            .created(42L)
            .message("msg")
            .headers(EMPTY_COMMIT_HEADERS)
            .incrementalIndex(emptyImmutableIndex(COMMIT_OP_SERIALIZER).serialize())
            .build(),
        tag(
            randomObjId(),
            "tab-msg",
            newCommitHeaders().add("Foo", "bar").build(),
            ByteString.copyFrom(new byte[1])),
        contentValue(randomObjId(), "cid", 0, ByteString.copyFrom(new byte[1])),
        stringData(
            randomObjId(),
            "foo",
            Compression.NONE,
            "foo",
            emptyList(),
            ByteString.copyFrom(new byte[1])),
        indexSegments(randomObjId(), emptyList()),
        index(randomObjId(), emptyImmutableIndex(COMMIT_OP_SERIALIZER).serialize()));
  }
}
