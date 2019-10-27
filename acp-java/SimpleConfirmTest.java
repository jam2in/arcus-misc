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

public class SimpleConfirmTest implements client_profile {
  String key;
  boolean ok;
  CommandSimple simple;

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
    simple = new CommandSimple(cli);
    String getKey = cli.ks.get_key();
    String ops[] = {"set", "add", "rpl", "ppd", "apd", "del", "cas", "incr", "decr"};
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
      case "set":
      case "add":
        ok = simple.confirm(key, getValue(key));
        break;
      case "rpl":
      case "cas":
        ok = simple.confirm(key, "jam2in");
        break;
      case "ppd":
        ok = simple.confirm(key, "jam2in" + getValue(key));
        break;
      case "apd":
        ok = simple.confirm(key, getValue(key) + "jam2in");
        break;
      case "del":
        ok = simple.confirm(key, null);
        break;
      case "incr":
        ok = simple.confirm(key, "9999");
        break;
      case "decr":
        ok = simple.confirm(key, "5555");
        break;
    }
    return ok;
  }

  public String getValue(String key) {
    return key + "_val";
  }
}
