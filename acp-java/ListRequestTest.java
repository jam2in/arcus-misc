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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;

public class ListRequestTest implements client_profile {
  String key;
  boolean ok;
  CommandList list;

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
    list = new CommandList(cli);
    String getKey = cli.ks.get_key();
    String ops[] = {"lop_crt", "lop_ist",
                    "lop_del"};
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
      case "lop_crt":
        ok = listCreateTest(cli);
        break;
      case "lop_ist":
        ok = listInsertTest(cli);
        break;
      case "lop_crt_ist":
        ok = listCreatedStoredTest(cli);
        break;
      case "lop_hdt":
        ok = listHeadTrimTest(cli);
        break;
      case "lop_tlt":
        ok = listTailTrimTest(cli);
        break;
      case "lop_del":
        ok = listDeleteTest(cli);
        break;
      case "lop_drop":
        ok = listDroppedTest(cli);
        break;
    }
    return ok;
  }

  public boolean listCreateTest(client cli) throws Exception {
    return list.create(key);
  }

  public boolean listInsertTest(client cli) throws Exception {
    assert (list.create(key));
    // insert 10 elements.
    for (int i = 0; i < 10; i++) {
      ok = list.insert(key, i, getValue(key) + Integer.toString(i), null);
      if (!ok) return false;
    }
    return ok;
  }

  public boolean listCreatedStoredTest(client cli) throws Exception {
    CollectionAttributes attr = new CollectionAttributes(cli.conf.client_exptime, new Long(3), CollectionOverflowAction.tail_trim);
    return list.insert(key, 0, getValue(key), attr);
  }

  public boolean listHeadTrimTest(client cli) throws Exception {
    CollectionAttributes attr = new CollectionAttributes(cli.conf.client_exptime, new Long(3), CollectionOverflowAction.head_trim);
    ok = list.create(key, attr);
    if (!ok) return false;
    ok = list.insert(key, 0, getValue(key) + "0", null);
    if (!ok) return false;
    ok = list.insert(key, 1, getValue(key) + "1", null);
    if (!ok) return false;
    ok = list.insert(key, 2, getValue(key) + "2", null);
    if (!ok) return false;
    ok = list.insert(key, 2, "jam2in", null);
    if (!ok) return false;
    return true;
  }

  public boolean listTailTrimTest(client cli) throws Exception {
    ok = list.create(key, 2);
    if (!ok) return false;
    ok = list.insert(key, 0, getValue(key) + "0", null);
    if (!ok) return false;
    ok = list.insert(key, 1, getValue(key) + "1", null);
    if (!ok) return false;
    ok = list.insert(key, 1, "jam2in", null);
    if (!ok) return false;
    return true;
  }

  public boolean listDeleteTest(client cli) throws Exception {
    ok = list.create(key);
    if (!ok) return false;
    ok = list.insert(key, 0, getValue(key), null);
    if (!ok) return false;
    ok = list.delete(key, 0, false);
    if (!ok) return false;
    return true;
  }

  public boolean listDroppedTest(client cli) throws Exception {
    ok = list.create(key);
    if (!ok) return false;
    ok = list.insert(key, 0, getValue(key), null);
    if (!ok) return false;
    ok = list.delete(key, 0, true);
    if (!ok) return false;
    return true;
  }

  public String getValue(String key) {
    return key + "_val";
  }
}
