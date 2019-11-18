/* -*- Mode: Java; tab-width: 2; c-basic-offbtree: 2; indent-tabs-mode: nil -*- */
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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
public class BtreeRequestTest implements client_profile {
  String key;
  boolean ok;
  CommandBtree btree;

  public boolean do_test(client cli) {
    try {
      if (!do_simple_test(cli)) {
        return false;
      }
    } catch (Exception e) {
      System.out.printf("client_profile exception. id=%d exception=%s\n", 
                        cli.id, e.toString());
      if (cli.conf.print_stack_trace)
        e.printStackTrace();
      //System.exit(0);
    }
    return true;
  }

  public boolean do_simple_test(client cli) throws Exception {
    if (!cli.before_request())
        return false;
    btree = new CommandBtree(cli);
    String getKey = cli.ks.get_key();
    String ops[] = {"bop_crt", "bop_ist", "bop_ust",
                    "bop_del", "bop_upd", "bop_incr", "bop_decr"};
    for (int i = 0; i < ops.length; i++) {
      key = TestUtil.getTestKey(getKey, ops[i]);
      TestUtil.printRequestStart(ops[i]);
      ok = execute(cli, ops[i]);
      if (ok) TestUtil.printRequestSuccess(ops[i]);
      else TestUtil.printRequestError(ops[i], key);
    }
    if (!cli.after_request(true))
      return false;
    return true;
  }

  public boolean execute(client cli, String command) throws Exception {
    switch (command) {
      case "bop_crt":
        ok = btreeCreateTest(cli);
        break;
      case "bop_ist":
        ok = btreeInsertTest(cli);
        break;
      case "bop_ust":
        ok = btreeUpsertTest(cli);
        break;
      case "bop_crt_ist":
        ok = btreeCreatedStoredTest(cli);
        break;
      case "bop_smt":
        ok = btreeSmallestTrimTest(cli);
        break;
      case "bop_lgt":
        ok = btreeLargestTrimTest(cli);
        break;
      case "bop_smst":
        ok = btreeSmallestSilentTrimTest(cli);
        break;
      case "bop_lgst":
        ok = btreeLargestSilentTrimTest(cli);
        break;
      case "bop_del":
        ok = btreeDeleteTest(cli);
        break;
      case "bop_upd":
        ok = btreeUpdateTest(cli);
        break;
      case "bop_incr":
        ok = btreeIncrementTest(cli);
        break;
      case "bop_decr":
        ok = btreeDecrementTest(cli);
        break;
      case "bop_drop":
        ok = btreeDroppedTest(cli);
        break;
      case "bop_get_with_del":
        ok = btreeGetWithDeleteTest(cli);
        break;
    }
    return ok;
  }

  public boolean btreeCreateTest(client cli) throws Exception {
    return btree.create(key);
  }

  public boolean btreeInsertTest(client cli) throws Exception {
    ok = btree.create(key);
    if (!ok) return false;

    // insert 10 elements.
    for (int i = 0; i < 10; i++) {
      ok = btree.insert(key, i, getValue(key) + Integer.toString(i), null);
      if (!ok) return false;
    }
    return ok;
  }

  public boolean btreeUpsertTest(client cli) throws Exception {
    ok = btree.create(key);
    if (!ok) return false;

    // insert 10 elements.
    for (int i = 0; i < 10; i++) {
      ok = btree.upsert(key, i, getValue(key) + Integer.toString(i), null);
      if (!ok) return false;
    }
    // upsert 5 elements.
    for (int i = 4; i < 10; i++) {
      ok = btree.upsert(key, i, "jam2in", null);
      if (!ok) return false;
    }
    return ok;
  }

  public boolean btreeCreatedStoredTest(client cli) throws Exception {
    return btree.insert(key, 0, getValue(key), btree.createAndGetCollectionAttributes(100L));
  }

  public boolean btreeSmallestTrimTest(client cli) throws Exception {
    CollectionAttributes attr = new CollectionAttributes(cli.conf.client_exptime, new Long(3), CollectionOverflowAction.smallest_trim);
    ok = btree.create(key, attr);
    if (!ok) return false;
    ok = btree.insert(key, 0, getValue(key), null);
    if (!ok) return false;
    ok = btree.insert(key, 3, getValue(key), null);
    if (!ok) return false;
    ok = btree.insert(key, 5, getValue(key), null);
    if (!ok) return false;
    ok = btree.insert(key, 2, "jam2in", null);
    return ok;
  }

  public boolean btreeLargestTrimTest(client cli) throws Exception {
    CollectionAttributes attr = new CollectionAttributes(cli.conf.client_exptime, new Long(3), CollectionOverflowAction.largest_trim);
    ok = btree.create(key, attr);
    if (!ok) return false;
    ok = btree.insert(key, 0, getValue(key), null);
    if (!ok) return false;
    ok = btree.insert(key, 3, getValue(key), null);
    if (!ok) return false;
    ok = btree.insert(key, 5, getValue(key), null);
    if (!ok) return false;
    ok = btree.insert(key, 2, "jam2in", null);
    return ok;
  }

  public boolean btreeSmallestSilentTrimTest(client cli) throws Exception {
    CollectionAttributes attr = new CollectionAttributes(cli.conf.client_exptime, new Long(3), CollectionOverflowAction.smallest_silent_trim);
    ok = btree.create(key, attr);
    if (!ok) return false;
    ok = btree.insert(key, 0, getValue(key), null);
    if (!ok) return false;
    ok = btree.insert(key, 3, getValue(key), null);
    if (!ok) return false;
    ok = btree.insert(key, 5, getValue(key), null);
    if (!ok) return false;
    ok = btree.insert(key, 2, "jam2in", null);
    return ok;
  }

  public boolean btreeLargestSilentTrimTest(client cli) throws Exception {
    CollectionAttributes attr = new CollectionAttributes(cli.conf.client_exptime, new Long(3), CollectionOverflowAction.largest_silent_trim);
    ok = btree.create(key, attr);
    if (!ok) return false;
    ok = btree.insert(key, 0, getValue(key), null);
    if (!ok) return false;
    ok = btree.insert(key, 3, getValue(key), null);
    if (!ok) return false;
    ok = btree.insert(key, 5, getValue(key), null);
    if (!ok) return false;
    ok = btree.insert(key, 2, "jam2in", null);
    return ok;
  }

  public boolean btreeDeleteTest(client cli) throws Exception {
    ok = btree.create(key);
    if (!ok) return false;
    ok = btree.insert(key, 0, getValue(key), null);
    if (!ok) return false;
    ok = btree.delete(key, 0, false);
    if (!ok) return false;
    return true;
  }

  public boolean btreeUpdateTest(client cli) throws Exception {
    ok = btree.create(key);
    if (!ok) return false;

    // insert 4 elements.
    for (int i = 0; i < 4; i++) {
      ok = btree.insert(key, i, getValue(key) + Integer.toString(i), null);
      if (!ok) return false;
    }
    ok = btree.update(key, 0, "jam2in");
    if (!ok) return false;
    ok = btree.update(key, 1, "jam2in");
    if (!ok) return false;
    return true;
  }

  public boolean btreeDroppedTest(client cli) throws Exception {
    ok = btree.create(key);
    if (!ok) return false;
    ok = btree.insert(key, 0, getValue(key), null);
    if (!ok) return false;
    ok = btree.delete(key, 0, true);
    if (!ok) return false;
    return true;
  }

  public boolean btreeGetWithDeleteTest(client cli) throws Exception {
    ok = btree.create(key);
    if (!ok) return false;

    // insert 10 elements.
    for (int i = 0; i < 10; i++) {
      ok = btree.insert(key, i, getValue(key) + Integer.toString(i), null);
      if (!ok) return false;
    }
    if(btree.get(key, 0, 10, true, false).size() != 10) return false;
    return true;
  }

  public boolean btreeIncrementTest(client cli) throws Exception {
    ok = btree.create(key);
    if (!ok) return false;
    ok = btree.insert(key, 0, "7777", null);
    if (!ok) return false;
    long result = btree.incr(key, 0, 2222);
    if (result != 9999) return false;
    return true;
  }

  public boolean btreeDecrementTest(client cli) throws Exception {
    ok = btree.create(key);
    if (!ok) return false;
    ok = btree.insert(key, 0, "7777", null);
    if (!ok) return false;
    long result = btree.decr(key, 0, 2222);
    if (result != 5555) return false;
    return true;
  }
  public String getValue(String key) {
    return key + "_val";
  }
}
