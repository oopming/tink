// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.cloud.crypto.tink.aead;

import static com.google.common.io.BaseEncoding.base16;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertArrayEquals;

import com.google.cloud.crypto.tink.Random;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for AesCtrJceCipher.
 * TODO(quangnguyen): Add more tests.
 */
public class AesCtrJceCipherTest {
  // NIST SP 800-38A pp 55.
  private static final String NIST_KEY = "2b7e151628aed2a6abf7158809cf4f3c";
  private static final String NIST_PLAINTEXT = "6bc1bee22e409f96e93d7e117393172a"
      + "ae2d8a571e03ac9c9eb76fac45af8e51"
      + "30c81c46a35ce411e5fbc1191a0a52ef"
      + "f69f2445df4f9b17ad2b417be66c3710";
  private static final String NIST_CIPHERTEXT = "874d6191b620e3261bef6864990db6ce"
      + "9806f66b7970fdff8617187bb9fffdff"
      + "5ae4df3edbd5d35e5b4f09020db03eab"
      + "1e031dda2fbe03d1792170a0f3009cee";
  private static final String NIST_IV = "f0f1f2f3f4f5f6f7f8f9fafbfcfdfeff";

  private static final String PLAINTEXT = "I'm counter mode and I'm not vulnerable to padding"
      + " oracle attack like CBC mode";

  private byte[] msg;
  @Before public void setUp() {
    try {
      msg = PLAINTEXT.getBytes("UTF-8");
    } catch (Exception ignored) {
      // Ignored
    }
  }

  @Test
  public void testNistVector() throws Exception {
    byte[] rawCiphertext = decode(NIST_CIPHERTEXT);
    byte[] iv = decode(NIST_IV);
    byte[] ciphertext = new byte[iv.length + rawCiphertext.length];
    System.arraycopy(iv, 0, ciphertext, 0, iv.length);
    System.arraycopy(rawCiphertext, 0, ciphertext, iv.length, rawCiphertext.length);
    AesCtrJceCipher cipher = new AesCtrJceCipher(decode(NIST_KEY), iv.length);
    assertArrayEquals(decode(NIST_PLAINTEXT), cipher.decrypt(ciphertext));
  }

  @Test
  public void testMultipleEncrypts() throws Exception {
    // Checks whether multiple encryptions result in different ciphertexts.
    byte[] key = Random.randBytes(16);
    int ivSize = 16;
    AesCtrJceCipher cipher = new AesCtrJceCipher(key, ivSize);
    byte[] c1 = cipher.encrypt(msg);
    byte[] c2 = cipher.encrypt(msg);
    assertEquals(c1.length, c2.length);
    assertFalse(Arrays.equals(c1, c2));
  }

  @Test
  public void testCtrProperty() throws Exception {
    // Counter mode is malleable, i.e., if we flip the ciphertext, the plaintext is flipped.
    byte[] key = Random.randBytes(16);
    int ivSize = 16;
    AesCtrJceCipher cipher = new AesCtrJceCipher(key, ivSize);
    byte[] c1 = cipher.encrypt(msg);
    for (int i = 0; i < msg.length; i++) {
      for (int j = 0; j < 8; j++) {
        byte[] p1 = Arrays.copyOf(msg, msg.length);
        byte[] c2 = Arrays.copyOf(c1, c1.length);
        p1[i] = (byte) (p1[i] ^ (1 << j));
        c2[i + ivSize] = (byte) (c2[i + ivSize] ^ (1 << j));
        byte[] p2 = cipher.decrypt(c2);
        assertArrayEquals(p1, p2);
        assertFalse(Arrays.equals(p2, msg));
      }
    }
  }

  @Test
  public void testEncryptDecrypt() throws Exception {
    byte[] key = Random.randBytes(16);
    int ivSize = 16;
    AesCtrJceCipher c = new AesCtrJceCipher(key, ivSize);
    byte[] ciphertext = c.encrypt(msg);
    assertArrayEquals(msg, c.decrypt(ciphertext));
  }

  private static byte[] decode(String hexData) {
    return base16().lowerCase().decode(hexData);
  }
}
