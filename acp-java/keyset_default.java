/* -*- Mode: Java; tab-width: 2; c-basic-offset: 2; indent-tabs-mode: nil -*- */
/*
 * acp-java : Arcus Java Client Performance benchmark program
 * Copyright 2013-2014 NAVER Corp.
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
// Default key set.

class keyset_default implements keyset {
  String[] set;
  int next_idx;
  // for integration test
  int offset;
  boolean useKeyRoop;
  boolean keyFinished;

  public keyset_default(int num, String prefix, boolean useKeyRoop) {
    set = new String[num];
    for (int i = 0; i < num; i++) {
      set[i] = "testkey-" + i;
      if (prefix != null)
        set[i] = prefix + set[i];
    }
    this.offset = 0;
    this.useKeyRoop = useKeyRoop;
    this.keyFinished = false;
    reset();
  }

  public void reset() {
    next_idx = 0;
  }

  public String get_key_by_cliid(client cli) {
    if (offset == 0) this.offset = set.length / cli.conf.client;
    if (cli.keyidx >= offset) cli.keyidx = 0;
    return set[cli.id*offset + cli.keyidx++];
  }

  synchronized public String get_key() {
    if (keyFinished) return null;

    int idx = next_idx++;
    if (next_idx >= set.length) {
      next_idx = 0;
      if (useKeyRoop == false) {
        keyFinished = true;
      }
    }
    return set[idx];
  }
}
