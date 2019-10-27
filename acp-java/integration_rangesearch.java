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
import java.util.List;
import java.util.Random;

import net.spy.memcached.internal.CollectionFuture;
import net.spy.memcached.ops.OperationStatus;

public class integration_rangesearch implements client_profile {
  public boolean do_test(client cli) {
    try {
      if (!do_simple_test(cli))
        return false;
    } catch (Exception e) {
      cli.after_request(false);
      if (cli.conf.print_stack_trace)
        e.printStackTrace();
    }
    return true;
  }

  public boolean do_simple_test(client cli) throws Exception {
    // run only single client.

    String key;
    byte[] val;
    Future<Boolean> fb;
    boolean ok;

    List<Object> keylist;
    CollectionFuture<List<Object>> fl;
    OperationStatus status;

    key = cli.ks.get_key();
    if (key != null) {
      if (!cli.before_request())
        return false;
      val = cli.vset.get_value();
      fb = cli.next_ac.set(key, cli.conf.client_exptime, val, raw_transcoder.raw_tc);
      System.out.printf("set operation request. key = " + key + "\n");
      ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("set failed on rangesearch test. id=%d key=%s\n", cli.id, key);
        System.exit(1);
      }
      if (!cli.after_request(ok))
        return false;
    } else {
      // range get oepration
      key = cli.conf.key_prefix + "testkey-";
      String frkey;
      String tokey;
      int maxGetCnt = 1000;
      int getcount;
      int min_rng = 0;
      int max_rng = cli.conf.keyset_size - 1;
      Random rand = new Random();
      System.out.printf("range get operation!!!\n");
      for (int i = min_rng; i < (max_rng - maxGetCnt); i++) {
        if (!cli.before_request())
          return false;

        getcount = rand.nextInt(maxGetCnt) + 1;
        frkey = key + i;
        tokey = key + (i + getcount);
        /*
        try {
          fl = cli.next_ac.asyncRangeGet(frkey, tokey, getcount);
          keylist = fl.get(5000, TimeUnit.MILLISECONDS);
          System.out.printf("range get frkey = " + frkey + ", tokey = " + tokey + "\n");
          for (int j = 0; j < keylist.size(); j++) {
              System.out.printf("rget key[" + j + "] = " + keylist.get(j) + "\n");
          }
          status = fl.getOperationStatus();

//          if ((keylist.size() != (getcount + 1)) ||
          if ((keylist.size() == 0) ||
               status.isSuccess() != true ||
               !("END".equals(status.getMessage()))) {
            System.out.println("getcount : " + getcount + "keysize : " + keylist.size() + ", isSucess : " + status.isSuccess() + ", messages : " + status.getMessage() + "\n");
            System.exit(1);
          }
        } catch (Exception e) {
          e.printStackTrace();
          System.exit(1);
        }
        */
        if (!cli.after_request(true))
          return false;
      }
      cli.set_stop(true);
    }

    return true;
  }
}
