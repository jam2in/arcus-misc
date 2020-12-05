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

public class FlushConfirmTest implements client_profile {
  String key;
  boolean ok;
  CommandFlush flush;
  CommandSimple simple;

  public boolean do_test(client cli) {
    try {
      if (!do_flush_test(cli)) {
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

  public boolean do_flush_test(client cli) throws Exception {
    if (!cli.before_request())
        return false;
    flush = new CommandFlush(cli);
    simple = new CommandSimple(cli);
    String ops[] = {"flush_all", "flush_prefix"};
    for (int i = 0; i < ops.length; i++) {
      key = ops[i];
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
      case "flush_all":
        ok = flushAllTest(cli);
        break;
      case "flush_prefix":
        ok = flushPrefixTest(cli);
        break;
    }
    return ok;
  }

  public boolean flushAllTest(client cli) throws Exception {
    ok = simple.confirm("flush", null);
    if (!ok) return false;
    ok = simple.confirm("jam2in:flush", null);
    return ok;
  }

  public boolean flushPrefixTest(client cli) throws Exception {
    ok = simple.confirm("jam2in:flush", null);
    if (!ok) return false;
    ok = simple.confirm("arcus-jam2in:flush", "val");
    if (!ok) return false;
    ok = simple.confirm("jam2in", "val");
    return ok;
  }
}
