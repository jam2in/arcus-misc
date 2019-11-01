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

public class persistence_recovery_onlyset implements client_profile {
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
    // Pick a key
    String key;
    byte[] val;
    Future<Boolean> fb;
    boolean ok = false;

    if (!cli.before_request())
      return false;
    key = cli.ks.get_key_by_cliid(cli);
    val = cli.vset.get_value();

    try {
      fb = cli.next_ac.set(key, cli.conf.client_exptime, val, raw_transcoder.raw_tc);
      ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!cli.write_cli_operation(key, null)) {
        System.out.println("recovery onlyset test failed : file write occur exception.");
        System.exit(1);
      }
    } catch (Exception e) {
      System.out.println("node is killed stop the test");
      cli.set_stop(true);
      return true;
    }

    if (!cli.after_request(ok))
      return false;

    return true;
  }
}
