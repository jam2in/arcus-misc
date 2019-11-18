/* -*- Mode: Java; tab-width: 2; c-basic-offmap: 2; indent-tabs-mode: nil -*- */
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
import java.util.ArrayList;

import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
public class MapRequestTest implements client_profile {
  String key;
  boolean ok;
  CommandMap map;

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
    map = new CommandMap(cli);
    String getKey = cli.ks.get_key();
    String ops[] = {"mop_crt", "mop_ist", "mop_del", "mop_upd"};
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
      case "mop_crt":
        ok = mapCreateTest(cli);
        break;
      case "mop_ist":
        ok = mapInsertTest(cli);
        break;
      case "mop_crt_ist":
        ok = mapCreatedStoredTest(cli);
        break;
      case "mop_del":
        ok = mapDeleteTest(cli);
        break;
      case "mop_upd":
        ok = mapUpdateTest(cli);
        break;
      case "mop_drop":
        ok = mapDroppedTest(cli);
        break;
      case "mop_get_with_del":
        ok = mapGetWithDeleteTest(cli);
        break;
    }
    return ok;
  }

  public boolean mapCreateTest(client cli) throws Exception {
    return map.create(key);
  }

  public boolean mapInsertTest(client cli) throws Exception {
    ok = map.create(key);
    if (!ok) return false;

    // insert 10 elements.
    for (int i = 0; i < 10; i++) {
      ok = map.insert(key, getField()+Integer.toString(i), getValue(key) + Integer.toString(i), null);
      if (!ok) return false;
    }
    return ok;
  }

  public boolean mapCreatedStoredTest(client cli) throws Exception {
    CollectionAttributes attr = new CollectionAttributes(cli.conf.client_exptime, new Long(3), CollectionOverflowAction.error);
    return map.insert(key, "mkey", getValue(key), attr);
  }

  public boolean mapDeleteTest(client cli) throws Exception {
    ok = map.create(key);
    if (!ok) return false;
    ok = map.insert(key, "mkey", getValue(key), null);
    if (!ok) return false;
    ok = map.delete(key, "mkey", false);
    if (!ok) return false;
    return true;
  }

  public boolean mapUpdateTest(client cli) throws Exception {
    ok = map.create(key);
    if (!ok) return false;

    // insert 4 elements.
    for (int i = 0; i < 4; i++) {
      ok = map.insert(key, getField()+Integer.toString(i), getValue(key) + Integer.toString(i), null);
      if (!ok) return false;
    }
    ok = map.update(key, "mkey0", "jam2in");
    if (!ok) return false;
    ok = map.update(key, "mkey1", "jam2in");
    if (!ok) return false;
    return true;
  }

  public boolean mapDroppedTest(client cli) throws Exception {
    ok = map.create(key);
    if (!ok) return false;
    ok = map.insert(key, "mkey", getValue(key), null);
    if (!ok) return false;
    ok = map.delete(key, "mkey", true);
    if (!ok) return false;
    return true;
  }

  public boolean mapGetWithDeleteTest(client cli) throws Exception {
    ok = map.create(key);
    if (!ok) return false;

    ArrayList<String> flist = new ArrayList<String>();
    // insert 10 elements.
    for (int i = 0; i < 10; i++) {
      ok = map.insert(key, getField() + Integer.toString(i), getValue(key) + Integer.toString(i), null);
      if (!ok) return false;
      flist.add(getField() + Integer.toString(i));
    }
    if (map.get(key, flist, true, false).size() != flist.size()) return false;
    return true;
  }

  public String getField() {
    return "mkey";
  }

  public String getValue(String key) {
    return key + "_val";
  }
}
