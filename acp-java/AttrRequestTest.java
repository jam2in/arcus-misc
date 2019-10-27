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
import net.spy.memcached.util.BTreeUtil;
public class AttrRequestTest implements client_profile {
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
    btree = new CommandBtree(cli);
    String getKey = cli.ks.get_key();
    String ops[] = {"setattr"};
    for (int i = 0; i < ops.length; i++) {
      key = TestUtil.getTestKey(getKey, "bop1");
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
      case "setattr":
        ok = setAttributeTest(cli);
        break;
    }
    return ok;
  }

  public boolean setAttributeTest(client cli) throws Exception {
    CollectionAttributes attr = new CollectionAttributes(new Integer(5000), new Long(2), CollectionOverflowAction.smallest_trim);
    attr.setReadable(false);
    ok = btree.create(key, attr);
    if (!ok) return false;
    attr.setExpireTime(new Integer(0));
    attr.setMaxCount(new Integer(10));
    attr.setOverflowAction(CollectionOverflowAction.largest_trim);
    attr.setReadable(true);
    attr.setMaxBkeyRange(new Long(5));
    ok = attribute.setAttr(key, attr);
    if (!ok) return false;
    return ok;
  }
}
