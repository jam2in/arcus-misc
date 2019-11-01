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

public class persistence_recovery_onlyget implements client_profile {
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
    String key;
    int get_try = 5;
    byte[] val = null;
    boolean ok = false;

    if (!cli.before_request())
      return false;
    key = cli.ks.get_key_by_cliid(cli);
      do {
      try {
            Future<byte[]> f = cli.next_ac.asyncGet(key, raw_transcoder.raw_tc);
            val = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
            if (val == null) {
                System.out.printf("recovery onlyget test end key : %s, stop client : %d\n",key, cli.id);
                cli.set_stop(true);
                return true;
            }
        if (!cli.write_cli_operation(key, null)) {
          System.out.println("recovery onlyget test failed : file write occur exception.");
          System.exit(1);
        }
      } catch(net.spy.memcached.internal.CheckedOperationTimeoutException te) {
        if (get_try-- > 0) {
          System.out.printf("recovery get failed. id=%d, key=%s remain try count : %d\n", cli.id, key, get_try);
          Thread.sleep(1000);
        } else {
          System.exit(1);
        }
      } catch (Exception e) {
        System.out.println("recovery onlyget test not occur exception.");
        e.printStackTrace();
        System.exit(1);
      }
    } while (val == null);

    if (!cli.after_request(true))
      return false;

    return true;
  }
}
