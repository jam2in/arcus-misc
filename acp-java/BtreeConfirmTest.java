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

import net.spy.memcached.collection.CollectionResponse;
public class BtreeConfirmTest implements client_profile {
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
      TestUtil.printConfirmStart(ops[i]);
      ok = execute(cli, ops[i]);
      if (ok) TestUtil.printConfirmSuccess(ops[i]);
      else TestUtil.printConfirmError(ops[i], key);
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
    return btree.exist(key);
  }

  public boolean btreeInsertTest(client cli) throws Exception {
    // get 10 elements.
    for (int i = 0; i < 10; i++) {
      ok = btree.confirm(key, i, getValue(key) + Integer.toString(i));
      if (!ok) return false;
    }
    return ok;
  }

  public boolean btreeUpsertTest(client cli) throws Exception {
    String value;
    for (int i = 0; i < 10; i++) {
      if (i >= 4) value = "jam2in";
      else value = getValue(key) + Integer.toString(i);
      ok = btree.confirm(key, i,value);
      if (!ok) return false;
    }
    return true;
  }

  public boolean btreeCreatedStoredTest(client cli) throws Exception {
    return btree.confirm(key, 0, getValue(key));
  }

  public boolean btreeSmallestTrimTest(client cli) throws Exception {
    btree.get(key, 0, false, false);
    ok = TestUtil.checkResponse(key, btree.getResponse(), CollectionResponse.OUT_OF_RANGE);
    if (!ok) return false;
    ok = btree.confirm(key, 3, getValue(key));
    if (!ok) return false;
    ok = btree.confirm(key, 5, getValue(key));
    if (!ok) return false;
    ok = btree.confirm(key, 2, "jam2in");
    return ok;
  }

  public boolean btreeLargestTrimTest(client cli) throws Exception {
    ok = btree.confirm(key, 0, getValue(key));
    if (!ok) return false;
    ok = btree.confirm(key, 3, getValue(key));
    if (!ok) return false;
    ok = btree.confirm(key, 2, "jam2in");
    if (!ok) return false;
    btree.get(key, 5, false, false);
    ok = TestUtil.checkResponse(key, btree.getResponse(), CollectionResponse.OUT_OF_RANGE);
    if (!ok) return false;

    return ok;
  }

  public boolean btreeSmallestSilentTrimTest(client cli) throws Exception {
    btree.get(key, 0, false, false);
    ok = TestUtil.checkResponse(key, btree.getResponse(), CollectionResponse.NOT_FOUND_ELEMENT);
    if (!ok) return false;
    ok = btree.confirm(key, 3, getValue(key));
    if (!ok) return false;
    ok = btree.confirm(key, 5, getValue(key));
    if (!ok) return false;
    ok = btree.confirm(key, 2, "jam2in");
    return ok;
  }

  public boolean btreeLargestSilentTrimTest(client cli) throws Exception {
    ok = btree.confirm(key, 0, getValue(key));
    if (!ok) return false;
    ok = btree.confirm(key, 3, getValue(key));
    if (!ok) return false;
    ok = btree.confirm(key, 2, "jam2in");
    if (!ok) return false;
    btree.get(key, 5, false, false);
    ok = TestUtil.checkResponse(key, btree.getResponse(), CollectionResponse.NOT_FOUND_ELEMENT);
    return ok;
  }

  public boolean btreeDeleteTest(client cli) throws Exception {
    return btree.confirm(key, 0, null);
  }

  public boolean btreeUpdateTest(client cli) throws Exception {
    String value;
    for (int i = 0; i < 4; i++) {
      if (i == 0 || i == 1) value = "jam2in";
      else value = getValue(key) + Integer.toString(i);
      ok = btree.confirm(key, i,value);
      if (!ok) return false;
    }
    return true;
  }

  public boolean btreeDroppedTest(client cli) throws Exception {
    return btree.create(key);
  }

  public boolean btreeGetWithDeleteTest(client cli) throws Exception {
    // get 10 elements.
    for (int i = 0; i < 10; i++) {
      ok = btree.confirm(key, i, null);
      if (!ok) return false;
    }
    return true;
  }

  public boolean btreeIncrementTest(client cli) throws Exception {
    return btree.confirm(key, 9999, null);
  }
  public boolean btreeDecrementTest(client cli) throws Exception {
    return btree.confirm(key, 5555, null);
  }
  public String getValue(String key) {
    return key + "_val";
  }
}
