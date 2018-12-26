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

import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;

public class integration_simplekv implements client_profile {
  String key;
  Future<Boolean> fb;
  Future<byte[]> fbyte;
  byte[] val;
  byte[] getval;
  boolean ok;

  public boolean do_test(client cli) {
    try {
      if (!do_simple_test(cli)) {
        return false;
      }
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
    // delete key
    String delkey[] = {"kv_set_test"
                     , "kv_add_test"
                     , "kv_repl_test"
                     , "kv_repl_test1"
                     , "kv_pre_test"
                     , "kv_apd_test"
                     , "kv_get_test"
                     , "kv_casgets_test"
                     , "kv_incr_test"
                     , "kv_decr_test"};

    for (int i = 0; i < delkey.length; i++) {
      if (!cli.before_request())
        return false;
      try {
        fb = cli.next_ac.delete(delkey[i]);
        ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      } catch (NullPointerException e) {
        System.out.printf("nullpointer exception!! delkey : %s, i : %d \n",delkey[i], i);
      } finally {
        if (!cli.after_request(true))
          return false;
      }
    }
    System.out.println("######## KV TEST START! #######");
    // test 1 : set
    simple_set_test(cli);
    System.out.println("KV TEST : SET_TEST SUCCESS!");
    // test 2 : add
    simple_add_test(cli);
    System.out.println("KV TEST : ADD_TEST SUCCESS!");
    // test 3 : replace
    simple_replace_test(cli);
    System.out.println("KV TEST : REPLACE_TEST SUCCESS!");
    // test 4 : prepend/append/delete
    simple_attach_delete_test(cli);
    System.out.println("KV TEST : DELETE_TEST SUCCESS!");
    // test 5 : cas/gets
    simple_cas_gets_test(cli);
    System.out.println("KV TEST : CAS_TEST SUCCESS!");
    // test 6 : incr/decr
    simple_arithmetic_test(cli);
    System.out.println("KV TEST : ARITHMETIC_TEST SUCCESS!");
    System.out.println("####### KV TEST SUCCESS! ######");

    System.exit(0);
    return true;
  }

  public void simple_set_test(client cli) throws Exception {
    // test 1 : set
    key = "kv_set_test";
    val = cli.vset.get_value(); //random value
    fb = cli.next_ac.set(key, cli.conf.client_exptime, val, raw_transcoder.raw_tc);
    System.out.printf("set operation request key = " + key + ", value = " + val + "\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "kv_set_test failed, predicted STORED";
  }

  public void simple_add_test(client cli) throws Exception {
    // test 2 : add
    key = "kv_add_test";
    fb = cli.next_ac.add(key, cli.conf.client_exptime, val);
    System.out.printf("add operation request key = " + key + ", value = " + val + "\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "kv_add_test failed, predicted STORED";

    fb = cli.next_ac.add(key, cli.conf.client_exptime, val);
    System.out.printf("add operation request key = " + key + ", value = " + val + "\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert !ok : "kv_add_test failed, predicted NOT_STORED";
  }

  public void simple_replace_test(client cli) throws Exception {
    // test 3 : replace
    key = "kv_repl_test";
    
    fb = cli.next_ac.set(key, cli.conf.client_exptime, "arcus"); //set key
    System.out.printf("set operation request key = " + key + ", value = arcus\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "kv_repl_test failed, predicted STORED";

    fb = cli.next_ac.replace(key, cli.conf.client_exptime, "jam2in"); //replace value
    System.out.printf("replace operation request key = " + key + ", value = jam2in\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "kv_repl_test failed, predicted STORED";

    fbyte = cli.next_ac.asyncGet(key, raw_transcoder.raw_tc); //confirm replace value
    getval = fbyte.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert "jam2in".equals(new String(getval, "UTF-8")) : "kv_repl_test failed, miss match replace value";

    key = "kv_repl_test2"; //not exist key
    fb = cli.next_ac.replace(key, cli.conf.client_exptime, val);
    System.out.printf("replace operation request key = " + key + ", value = " + val + "\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert !ok : "kv_repl_test failed, predicted NOT_STORED";
  }

  public void simple_attach_delete_test(client cli) throws Exception {
    // test 4 : prepend/append/delete
    long not_used = 100L;
    key = "kv_pre_test";
    fb = cli.next_ac.set(key, cli.conf.client_exptime, "arcus"); //set prepend key
    System.out.printf("set operation request key = " + key + ", value = arcus\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "kv_pre_test failed, predicted STORED";

    fb = cli.next_ac.prepend(not_used, key, "jam2in"); //prepend value
    System.out.printf("prepend operation request key = " + key + ", value = jam2in\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "kv_pre_test failed, predicted STORED";

    fbyte = cli.next_ac.asyncGet(key, raw_transcoder.raw_tc); //confirm prepend value
    getval = fbyte.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert "jam2inarcus".equals(new String(getval, "UTF-8")) : "kv_pre_test failed, miss match prepende value";

    key = "kv_apd_test";
    fb = cli.next_ac.set(key, cli.conf.client_exptime, "arcus"); //set append key
    System.out.printf("set operation request key = " + key + ", value = arcus\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "kv_apd_test failed, predicted STORED";

    fb = cli.next_ac.append(not_used, key, "jam2in"); //append value
    System.out.printf("append operation request key = " + key + ", value = jam2in\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "kv_apd_test failed, predicted STORED";

    fbyte = cli.next_ac.asyncGet(key, raw_transcoder.raw_tc); //confirm prepend value
    getval = fbyte.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert "arcusjam2in".equals(new String(getval, "UTF-8")) : "kv_apd_test failed, miss match append value";

    fb = cli.next_ac.delete(key); //delete key
    System.out.printf("delete operation request key = " + key + "\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "kv_apd_test delete failed, predicted DELETED";

    fb = cli.next_ac.delete(key); //delete key
    System.out.printf("delete operation request key = " + key + "\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert !ok : "kv_apd_test delete failed, predicted NOT_FOUND";
  }

  public void simple_cas_gets_test(client cli) throws Exception {
    //test 5 : cas
    key = "kv_casgets_test";
    fb = cli.next_ac.set(key, cli.conf.client_exptime, "7777"); //set prepend key
    System.out.printf("set operation request key = " + key + ", value = 7777\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "kv_incr_test failed, predicted STORED";

    long cas;
    Future<CASValue<byte[]>> fcas;
    CASValue<byte[]> casvalue;
    fcas = cli.next_ac.asyncGets(key, raw_transcoder.raw_tc);
    casvalue = fcas.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    cas = casvalue.getCas();

    Future<CASResponse> fcasr;
    CASResponse casres;
    fcasr = cli.next_ac.asyncCAS(key, cas, "8888");
    System.out.printf("cas operation request key = " + key + ", cas = " + cas + ", value = 8888\n");
    casres = fcasr.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert (casres == CASResponse.OK) : "kv_casgets_test failed, predicted OK";
  }

  public void simple_arithmetic_test(client cli) throws Exception {
    // test 6 : arithmetic
    // increase
    Future<Long> fl;
    long result;
    key = "kv_incr_test";
    fb = cli.next_ac.set(key, cli.conf.client_exptime, "7777"); //set prepend key
    System.out.printf("set operation request key = " + key + ", value = 7777\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "kv_incr_test failed, predicted STORED";

    fl = cli.next_ac.asyncIncr(key, 2223);
    System.out.printf("incr operation request key = " + key + ", value = 2223\n");
    result = fl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert (result == 10000) : "kv_incr_test failed, predicted value is 10000";

    // decrease
    key = "kv_decr_test";
    fb = cli.next_ac.set(key, cli.conf.client_exptime, "7777"); //set prepend key
    System.out.printf("set operation request key = " + key + ", value = 7777\n");
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "kv_incr_test failed, predicted STORED";

    fl = cli.next_ac.asyncDecr(key, 2222);
    System.out.printf("decr operation request key = " + key + ", value = 2222\n");
    result = fl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert (result == 5555) : "kv_decr_test failed, predicted value is 5555";
  }
}
