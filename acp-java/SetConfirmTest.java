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

public class SetConfirmTest implements client_profile {
  String key;
  boolean ok;
  CommandSet set;

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
    set = new CommandSet(cli);
    String getKey = cli.ks.get_key();
    String ops[] = {"sop_crt", "sop_ist",
                    "sop_del"};
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
      case "sop_crt":
        ok = setCreateTest(cli);
        break;
      case "sop_ist":
        ok = setInsertTest(cli);
        break;
      case "sop_crt_ist":
        ok = setCreatedStoredTest(cli);
        break;
      case "sop_del":
        ok = setDeleteTest(cli);
        break;
      case "sop_drop":
        ok = setDroppedTest(cli);
        break;
      case "sop_get_with_del":
        ok = setGetWithDeleteTest(cli);
        break;
    }
    return ok;
  }

  public boolean setCreateTest(client cli) throws Exception {
    return set.exist(key);
  }

  public boolean setInsertTest(client cli) throws Exception {
    // get 10 elements.
    for (int i = 0; i < 9; i++) {
      ok = set.elemExist(key, getValue(key) + Integer.toString(i), true);
      if (!ok) return false;
    }
    return ok;
  }

  public boolean setCreatedStoredTest(client cli) throws Exception {
    return set.elemExist(key, getValue(key), true);
  }

  public boolean setDeleteTest(client cli) throws Exception {
    return set.elemExist(key, getValue(key), false);
  }

  public boolean setDroppedTest(client cli) throws Exception {
    return set.create(key);
  }

  public String getValue(String key) {
    return key + "_val";
  }
    
  public boolean setGetWithDeleteTest(client cli) throws Exception {
    // get 10 elements.
    for (int i = 0; i < 10; i++) {
      ok = set.elemExist(key, getValue(key) + Integer.toString(i), false);
      if (!ok) return false;
    }
    return true;
  }
}
