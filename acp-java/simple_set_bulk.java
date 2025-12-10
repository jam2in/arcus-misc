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
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;

import net.spy.memcached.ops.OperationStatus;
import net.spy.memcached.ops.StoreType;

public class simple_set_bulk implements client_profile {

  public boolean do_test(client cli) {
    try {
      if (!do_simple_test(cli))
        return false;
    } catch (Exception e) {
      System.out.printf("client_profile exception. id=%d exception=%s\n",
                        cli.id, e.toString());
      if (cli.conf.print_stack_trace)
        e.printStackTrace();
    }
    return true;
  }

  public boolean do_simple_test(client cli) throws Exception {
    int loop_cnt = 100;

    // Prepare Key list
    byte[] val = cli.vset.get_value();

    List<String> key_list = new LinkedList<String>();
    for (int i = 0; i < loop_cnt; i++) {
      String key = cli.ks.get_key();
      key_list.add(key);
    }

    // Set Bulk
    if (!cli.before_request())
      return false;

    Future<Map<String, OperationStatus>> f =
      cli.next_ac.asyncStoreBulk(StoreType.set, key_list, cli.conf.client_exptime, val);
    Map<String, OperationStatus> result =
      f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (result == null) {
      System.out.printf("set bulk failed. id=%d key=%s\n", cli.id, key_list.get(0));
    }

    if (!cli.after_request(true))
      return false;

    return true;
  }
}
