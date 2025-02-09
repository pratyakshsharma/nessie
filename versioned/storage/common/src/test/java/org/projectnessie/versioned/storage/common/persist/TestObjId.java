/*
 * Copyright (C) 2022 Dremio
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
package org.projectnessie.versioned.storage.common.persist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.projectnessie.versioned.storage.common.persist.ObjId.EMPTY_OBJ_ID;
import static org.projectnessie.versioned.storage.common.persist.ObjId.deserializeObjId;
import static org.projectnessie.versioned.storage.common.persist.ObjId.objIdFromByteArray;
import static org.projectnessie.versioned.storage.common.persist.ObjId.objIdFromBytes;
import static org.projectnessie.versioned.storage.common.persist.ObjId.objIdFromString;

import com.google.common.hash.Hashing;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.stream.Stream;
import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.projectnessie.nessie.relocated.protobuf.ByteString;
import org.projectnessie.versioned.storage.common.persist.ObjId.ObjId256;
import org.projectnessie.versioned.storage.common.persist.ObjId.ObjIdEmpty;
import org.projectnessie.versioned.storage.common.persist.ObjId.ObjIdGeneric;

@ExtendWith(SoftAssertionsExtension.class)
public class TestObjId {
  @InjectSoftAssertions SoftAssertions soft;

  @SuppressWarnings("UnstableApiUsage")
  @Test
  void empty() {
    byte[] emptyHash =
        Hashing.sha256().newHasher().putString("empty", StandardCharsets.UTF_8).hash().asBytes();

    soft.assertThat(EMPTY_OBJ_ID)
        .isInstanceOf(ObjId256.class)
        .extracting(ObjId::asByteBuffer)
        .isEqualTo(ByteBuffer.wrap(emptyHash));
    soft.assertThat(EMPTY_OBJ_ID)
        .isInstanceOf(ObjId256.class)
        .extracting(ObjId::asByteArray)
        .isEqualTo(emptyHash);
    soft.assertThat(EMPTY_OBJ_ID.toString())
        .isEqualTo("2e1cfa82b035c26cbbbdae632cea070514eb8b773f616aaeaf668e2f0be8f10d");
  }

  @SuppressWarnings("ConstantConditions")
  @Test
  void nullCheck() {
    soft.assertThatThrownBy(() -> objIdFromString(null)).isInstanceOf(NullPointerException.class);
    soft.assertThatThrownBy(() -> deserializeObjId(null)).isInstanceOf(NullPointerException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"1", "123"})
  void illegalLengths(String s) {
    assertThatThrownBy(() -> objIdFromString(s))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("length needs to be a multiple of two");
  }

  @Test
  void illegalChars() {
    assertThatThrownBy(() -> objIdFromString("deadex"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Illegal hex character");
  }

  static Stream<Arguments> hashOfString() {
    return Stream.of(
        arguments("", ObjIdEmpty.class),
        arguments("01", ObjIdGeneric.class),
        arguments("23", ObjIdGeneric.class),
        arguments("45", ObjIdGeneric.class),
        arguments("67", ObjIdGeneric.class),
        arguments("89", ObjIdGeneric.class),
        arguments("ab", ObjIdGeneric.class),
        arguments("cd", ObjIdGeneric.class),
        arguments("ef", ObjIdGeneric.class),
        arguments("AB", ObjIdGeneric.class),
        arguments("CD", ObjIdGeneric.class),
        arguments("EF", ObjIdGeneric.class),
        arguments("0123456789abcdef", ObjIdGeneric.class),
        arguments("0123456789abcdef", ObjIdGeneric.class),
        arguments(
            "0011223344556677" + "1213141516171819" + "2123242526272829" + "3132343536373839",
            ObjId256.class),
        arguments(
            "ffddbbaa88665544" + "9192939495969798" + "80776699deadbeef" + "cafebabe12345688",
            ObjId256.class));
  }

  @ParameterizedTest
  @MethodSource("hashOfString")
  void fromString(String s, Class<? extends ObjId> type) {
    ObjId h = objIdFromString(s);
    soft.assertThat(h).isInstanceOf(type);
    soft.assertThat(h.size()).isEqualTo(s.length() / 2);
    soft.assertThat(h.serializedSize()).isEqualTo(h.size() + 1);
    soft.assertThat(h.asByteArray()).hasSize(h.size()).isEqualTo(fromHex(s, false).array());
    soft.assertThat(h.asByteBuffer()).isEqualTo(fromHex(s, false));
    soft.assertThat(h.toString()).isEqualTo(s.toLowerCase(Locale.US));
    soft.assertThat(h).isEqualTo(deserializeObjId(fromHex(s, true)));
  }

  @ParameterizedTest
  @MethodSource("hashOfString")
  void fromByteArray(String s, Class<? extends ObjId> type) {
    byte[] bytes = new byte[s.length() / 2];
    fromHex(s, false).get(bytes);

    ObjId h = objIdFromByteArray(bytes);
    soft.assertThat(h).isInstanceOf(type);
    soft.assertThat(h.size()).isEqualTo(s.length() / 2);
    soft.assertThat(h.serializedSize()).isEqualTo(h.size() + 1);
    soft.assertThat(h.asByteArray()).hasSize(h.size()).isEqualTo(fromHex(s, false).array());
    soft.assertThat(h.asByteBuffer()).isEqualTo(fromHex(s, false));
    soft.assertThat(h.toString()).isEqualTo(s.toLowerCase(Locale.US));
    soft.assertThat(h).isEqualTo(deserializeObjId(fromHex(s, true)));
  }

  @ParameterizedTest
  @MethodSource("hashOfString")
  void fromByteString(String s, Class<? extends ObjId> type) {
    byte[] bytes = new byte[s.length() / 2];
    fromHex(s, false).get(bytes);

    ObjId h = objIdFromBytes(ByteString.copyFrom(bytes));
    soft.assertThat(h).isInstanceOf(type);
    soft.assertThat(h.size()).isEqualTo(s.length() / 2);
    soft.assertThat(h.serializedSize()).isEqualTo(h.size() + 1);
    soft.assertThat(h.asByteArray()).hasSize(h.size()).isEqualTo(fromHex(s, false).array());
    soft.assertThat(h.asByteBuffer()).isEqualTo(fromHex(s, false));
    soft.assertThat(h.toString()).isEqualTo(s.toLowerCase(Locale.US));
    soft.assertThat(h).isEqualTo(deserializeObjId(fromHex(s, true)));
  }

  static Stream<Arguments> hashCodes() {
    return Stream.of(
        arguments("", 0),
        arguments("01", 0x01000000),
        arguments("23", 0x23000000),
        arguments("45", 0x45000000),
        arguments("67", 0x67000000),
        arguments("89", 0x89000000),
        arguments("ab", 0xab000000),
        arguments("cd", 0xcd000000),
        arguments("ef", 0xef000000),
        arguments("AB", 0xab000000),
        arguments("CD", 0xcd000000),
        arguments("EF", 0xef000000),
        arguments("0123456789abcdef", 0x01234567),
        arguments(
            "0011223344556677" + "1213141516171819" + "2123242526272829" + "3132343536373839",
            0x00112233),
        arguments(
            "ffddbbaa88665544" + "9192939495969798" + "80776699deadbeef" + "cafebabe12345688",
            0xffddbbaa));
  }

  @ParameterizedTest
  @MethodSource("hashCodes")
  void hashCodes(String s, int expected) {
    assertThat(objIdFromString(s)).extracting(ObjId::hashCode).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "01",
        "23",
        "cd",
        "ef",
        "0123456789abcdef",
        "0011223344556677" + "1213141516171819" + "2123242526272829" + "3132343536373839",
        "ffddbbaa88665544" + "9192939495969798" + "80776699deadbeef" + "cafebabe12345688"
      })
  void nibbles(String s) {
    ByteBuffer bytes = fromHex(s, false);
    ObjId h = objIdFromString(s);

    soft.assertThatThrownBy(() -> h.nibbleAt(-1)).isInstanceOf(RuntimeException.class);
    soft.assertThatThrownBy(() -> h.nibbleAt(bytes.remaining() * 2))
        .isInstanceOf(RuntimeException.class);

    for (int i = 0; i < bytes.remaining(); i++) {
      int nib = i * 2;
      soft.assertThat(h.nibbleAt(nib))
          .describedAs("nibble %s", nib)
          .isEqualTo((bytes.get(i) >> 4) & 15);
      soft.assertThat(h.nibbleAt(nib + 1))
          .describedAs("nibble %s", nib + 1)
          .isEqualTo(bytes.get(i) & 15);
    }
  }

  static ByteBuffer fromHex(String hex, boolean withLen) {
    int l = hex.length() >> 1;
    ByteBuffer b = ByteBuffer.allocate(l + (withLen ? 1 : 0));
    if (withLen) {
      b.put((byte) 0);
    }
    for (int p = 0; p < l; p++) {
      String hb =
          (p * 2 + 2 >= hex.length()) ? hex.substring(p * 2) : hex.substring(p * 2, p * 2 + 2);
      b.put((byte) Short.parseShort(hb, 16));
    }
    b.flip();
    if (withLen) {
      b.put(0, (byte) (l == 256 ? 0 : l));
    }
    return b;
  }
}
