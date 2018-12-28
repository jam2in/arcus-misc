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

public class integration_getset_ratio implements client_profile {
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
    // All keyset entries must already be in cluster
    // Please refer to integration_onlyset.java
    // Do one set and <get_count> get.

    String key;
    byte[] val;
    Future<Boolean> fb;
    Future<byte[]> f;
    boolean ok;
    int ratio = cli.get_ratio();

    long start = System.nanoTime();
    long end;

    if (ratio == 0) {
      if (!cli.before_request())
        return false;
      // Pick a key
      key = cli.ks.get_key();
      val = cli.vset.get_value();
      fb = cli.next_ac.set(key, cli.conf.client_exptime, val, raw_transcoder.raw_tc);
      ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("integration test set failed. id=%d key=%s\n", cli.id, key);
        System.exit(1);
      }
      if (!cli.after_request(ok))
        return false;
    } else {
      if (!cli.before_request())
        return false;
      key = cli.ks.get_key();
      f = cli.next_ac.asyncGet(key, raw_transcoder.raw_tc);
      val = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

      if (val == null) {
        System.out.printf("integration test get failed. id=%d key=%s\n", cli.id, key);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;
    }
    end = System.nanoTime();
    cli.add_optime(end - start);
    return true;
  }
}
