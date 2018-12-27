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

public class simple_set implements client_profile {
  public boolean do_test(client cli) {
    try {
      if (!do_simple_test(cli))
        return false;
    } catch (Exception e) {
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
    // Pick a key
    String key = cli.ks.get_key();
//    byte[] val = cli.vset.get_value();
    byte[] val = cli.conf.zookeeper.getBytes();
    Future<Boolean> fb =
      cli.next_ac.set(key, cli.conf.client_exptime, val, raw_transcoder.raw_tc);
    System.out.printf("set operation request. key = " + key + ", val = " + val + "\n");
    boolean ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (!ok) {
      System.out.printf("set failed. id=%d key=%s\n", cli.id, key);
    }
    if (!cli.after_request(ok))
      return false;
    if (!ok)
      return true;
    return true;
  }
}
