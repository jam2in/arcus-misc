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

public class ListConfirmTest implements client_profile {
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
    return list.exist(key);
  }

  public boolean listInsertTest(client cli) throws Exception {
    // get 10 elements.
    for (int i = 0; i < 9; i++) {
      ok = list.confirm(key, i, getValue(key) + Integer.toString(i));
      if (!ok) return false;
    }
    return ok;
  }

  public boolean listCreatedStoredTest(client cli) throws Exception {
    return list.confirm(key, 0, getValue(key));
  }

  public boolean listOverflowTest(client cli) throws Exception {
    /*
    assert (list.insert(key, 0, getValue(key) + "0", list.create_collattr(2L, CollectionOverflowAction.error)));
    if (!ok) return false;
    ok = list.insert(key, 1, getValue(key) + "1", null);
    if (!ok) return false;
    ok = list.insert(key, 1, getValue(key) + "overflow", null);
    if (ok) return false;
    */
    return true;
  }

  public boolean listHeadTrimTest(client cli) throws Exception {
    ok = list.confirm(key, 0, getValue(key) + "1");
    if (!ok) return false;
    ok = list.confirm(key, 1, "jam2in");
    if (!ok) return false;
    ok = list.confirm(key, 2, getValue(key) + "2");
    if (!ok) return false;
    return true;
  }

  public boolean listTailTrimTest(client cli) throws Exception {
    ok = list.confirm(key, 0, getValue(key) + "0");
    if (!ok) return false;
    ok = list.confirm(key, 1, "jam2in");
    return ok;
  }

  public boolean listDeleteTest(client cli) throws Exception {
    return list.confirm(key, 0, null);
  }

  public boolean listDroppedTest(client cli) throws Exception {
    return list.create(key);
  }

  public String getValue(String key) {
    return key + "_val";
  }
}

