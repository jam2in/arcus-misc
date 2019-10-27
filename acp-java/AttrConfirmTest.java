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

import net.spy.memcached.collection.Attributes;
import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
public class AttrConfirmTest implements client_profile {
  String key;
  boolean ok;
  CommandAttribute attribute;
  CommandBtree btree;

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
    attribute = new CommandAttribute(cli);
    String getKey = cli.ks.get_key();
    String ops[] = {"setattr"};
    for (int i = 0; i < ops.length; i++) {
      key = TestUtil.getTestKey(getKey, "bop1");
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
      case "setattr":
        ok = setAttributeTest(cli);
        break;
    }
    return ok;
  }

  public boolean setAttributeTest(client cli) throws Exception {
    CollectionAttributes attr = attribute.getAttr(key);
    assert (attr.getExpireTime() == 0);
    assert (attr.getMaxCount() == 10);
    assert (attr.getOverflowAction() == CollectionOverflowAction.largest_trim);
    assert (attr.getReadable() == true);
    assert (attr.getMaxBkeyRange() == 5);
    return true;
  }
}
