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

public class integration_repl_onlyget implements client_profile {
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
    // Do one set and one get.  The same key.

    int get_try = 10;

    String key;
    byte[] val = null;

    key = cli.ks.get_key();
    if (key == null) {
      cli.set_stop(true);
      return true;
    }
    if (!cli.before_request())
      return false;
    do {
      try {
        Future<byte[]> f = cli.next_ac.asyncGet(key, raw_transcoder.raw_tc);
        val = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
        if (val == null) {
          System.out.printf("repl onlyget test fail key miss : %s\n",key);
          System.exit(1);
        }
      } catch (net.spy.memcached.internal.CheckedOperationTimeoutException te) {
        if (get_try-- > 0) {
          System.out.printf("repl get failed. id=%d key=%s remain try count : %d\n",cli.id, key, get_try);
          Thread.sleep(1000);
        } else {
          System.out.printf("repl get failed. id=%d key=%s get operation exceeded 10 times\n",cli.id, key);
          System.exit(1);
        }
      } catch (Exception e) {
        System.out.printf("repl get failed. id=%d key=%s\n",cli.id);
        e.printStackTrace();
        System.exit(1);
      }
    } while (val == null);
    if (!cli.after_request(true))
      return false;

    return true;
  }
}
