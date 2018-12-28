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

public class integration_repltest_print implements client_profile {
  public boolean do_test(client cli) {
    try {
      if (!do_simple_test(cli))
        return false;
    } catch (Exception e) {
      cli.after_request(false);
      /*
      System.out.printf("client_profile exception. id=%d exception=%s\n", 
                        cli.id, e.toString());
      */
      if (cli.conf.print_stack_trace)
        e.printStackTrace();
      //System.exit(0);
    }
    return true;
  }

  public boolean do_simple_test(client cli) throws Exception {
    // for replication test
    String key;
    byte[] val;
    Future<Boolean> fb;
    boolean ok = false;

    // set as many keys as cli.conf.keyset_size
    key = cli.ks.get_key();
    if (key != null) {
      if (!cli.before_request())
        return false;
      val = cli.vset.get_value();
      do {
        try {
          fb = cli.next_ac.set(key, cli.conf.client_exptime, val, raw_transcoder.raw_tc);
          System.out.printf("set operation request. key = " + key + ", val = " + val + "\n");
          //System.out.printf("set operation request. key = " + key + ", val = " + val + "\n");
          ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
        } catch (net.spy.memcached.internal.CheckedOperationTimeoutException te) {
          System.out.println("this test should not occur with TimeoutException... retry set key : " + key);
          //System.out.println("this test should not occur with TimeoutException... increase client_timeout");
          //System.exit(1);
        } catch (Exception e) {
          // master node down...
          System.out.printf("master node down... 30 seconds waiting for switchover... cli_id=%d\n",cli.id);
          System.out.println(e.toString());

          // waiting sleep_count secondes....
          int sleep_count = 33;
          while (sleep_count > 0) {
              Thread.sleep(1000);
              sleep_count--;
          }
        }
      } while (!ok);
      if (!cli.after_request(ok))
        return false;
    } else {
      // get operation
      if (!cli.before_request())
        return false;
      key = cli.ks.get_key();
      val = cli.vset.get_value();
      Future<byte[]> f = cli.next_ac.asyncGet(key, raw_transcoder.raw_tc);
      System.out.printf("get operation request. key = " + key + "\n");
      //System.out.printf("get operation request. key = " + key + "\n");
      val = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

      if (val == null) {
        System.out.printf("get failed. id=%d key=%s\n", cli.id, key);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;
    }
    return true;
  }
}
