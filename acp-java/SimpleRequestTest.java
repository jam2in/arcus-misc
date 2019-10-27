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

public class SimpleRequestTest implements client_profile {
  String key;
  boolean ok;
  CommandSimple simple;

  public boolean do_test(client cli) {
    try {
      if (!do_simple_test(cli)) {
        return false;
      }
    } catch (Exception e) {
      cli.after_request(false);
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
      case "set":
        ok = setTest(cli);
        break;
      case "add":
        ok = addTest(cli);
        break;
      case "rpl":
        ok = replaceTest(cli);
        break;
      case "ppd":
        ok = prependTest(cli);
        break;
      case "apd":
        ok = appendTest(cli);
        break;
      case "del":
        ok = deleteTest(cli);
        break;
      case "cas":
        ok = casTest(cli);
        break;
      case "incr":
        ok = incrTest(cli);
        break;
      case "decr":
        ok = decrTest(cli);
        break;
    }
    return ok;
  }

  public boolean setTest(client cli) throws Exception {
    return simple.set(key, getValue(key));
  }

  public boolean addTest(client cli) throws Exception {
    return simple.add(key, getValue(key));
  }

  public boolean replaceTest(client cli) throws Exception {
    ok = simple.set(key, getValue(key));
    if (!ok) return false;
    ok = simple.replace(key, "jam2in");
    return ok;
  }

  public boolean prependTest(client cli) throws Exception {
    ok = simple.set(key, getValue(key));
    if (!ok) return false;
    ok = simple.prepend(key, "jam2in");
    return ok;
  }

  public boolean appendTest(client cli) throws Exception {
    ok = simple.set(key, getValue(key));
    if (!ok) return false;
    ok = simple.append(key, "jam2in");
    return ok;
  }

  public boolean deleteTest(client cli) throws Exception {
    ok = simple.set(key, getValue(key));
    if (!ok) return false;
    ok = simple.delete(key);
    return ok;
  }
        
  public boolean casTest(client cli) throws Exception {
    ok = simple.set(key, getValue(key));
    if (!ok) return false;
    ok = simple.cas(key, "jam2in");
    return ok;
  }

  public boolean incrTest(client cli) throws Exception {
    ok = simple.set(key, "7777");
    if (!ok) return false;
    long result = simple.increment(key, 2222);
    if(result == 0L) return false;
    return true;
  }

  public boolean decrTest(client cli) throws Exception {
    ok = simple.set(key, "7777");
    long result = simple.decrement(key, 2222);
    if(result == 0L) return false;
    return true;
  }

  public String getValue(String key) {
    return key + "_val";
  }
}
