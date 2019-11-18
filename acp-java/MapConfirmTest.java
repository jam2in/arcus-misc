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

public class MapConfirmTest implements client_profile {
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
    String ops[] = {"mop_crt", "mop_ist",
                    "mop_del", "mop_upd"};
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
    return map.exist(key);
  }

  public boolean mapInsertTest(client cli) throws Exception {
    // get 10 elements.
    for (int i = 0; i < 10; i++) {
      ok = map.confirm(key, getField()+Integer.toString(i), getValue(key) + Integer.toString(i));
      if (!ok) return false;
    }
    return ok;
  }

  public boolean mapCreatedStoredTest(client cli) throws Exception {
    return map.confirm(key, "mkey", getValue(key));
  }

  public boolean mapDeleteTest(client cli) throws Exception {
    return map.confirm(key, "mkey", null);
  }

  public boolean mapUpdateTest(client cli) throws Exception {
    String value;
    for (int i = 0; i < 4; i++) {
      if (i == 0 || i == 1) value = "jam2in";
      else value = getValue(key) + Integer.toString(i);
      ok = map.confirm(key, getField()+Integer.toString(i),value);
      if (!ok) return false;
    }
    return true;
  }

  public boolean mapDroppedTest(client cli) throws Exception {
    return map.create(key);
  }

  public boolean mapGetWithDeleteTest(client cli) throws Exception {
    // get 10 elements.
    for (int i = 0; i < 10; i++) {
      ok = map.confirm(key, getField()+Integer.toString(i), null);
      if (!ok) return false;
    }
    return true;
  }

  public String getField() {
    return "mkey";
  }

  public String getValue(String key) {
    return key + "_val";
  }
}

