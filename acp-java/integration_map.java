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
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.ElementValueType;
import net.spy.memcached.internal.CollectionFuture;
import net.spy.memcached.ops.CollectionOperationStatus;

public class integration_map implements client_profile {
  String key;
  String mkey;
  boolean ok;

  ElementValueType vtype;
  CollectionAttributes attr;
  CollectionResponse response;
  CollectionFuture<Boolean> colfb;              /* collection future boolean */
  CollectionFuture<Map<String, Object>> colfm;  /* collemtion future map : for mop get test */
  Future<Boolean> simplefb;                     /* simplekv future boolean */

  Map<String, Object> mapval; /* for mop get operation */
  byte[] val;

  public boolean do_test(client cli) {
    try {
      if (!do_simple_test(cli)) {
        return false;
      }
    } catch (Exception e) {
      System.out.printf("client_profile exception. id=%d exception=%s\n", 
                        cli.id, e.toString());
      if (cli.conf.print_stack_trace)
        e.printStackTrace();
    }
    return true;
  }

  public boolean do_simple_test(client cli) throws Exception {
    // delete key
    String delkey[] = {"mop_create_test"
                     , "mop_typemismatch_test"
                     , "mop_insert_test"
                     , "mop_delete_test"
                     , "mop_get_test1"
                     , "mop_get_test2"
                     , "mop_unreadable"
                     , "mop_update_test1"
                     , "mop_update_test2"};

    for (int i = 0; i < delkey.length; i++) {
      if (!cli.before_request(false)) return false;

      simplefb = cli.next_ac.delete(delkey[i]);
      ok = simplefb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

      if (!cli.after_request(true)) return false;
    }
    // prepare test
    System.out.println("####### MOP TEST START! #######");
    prepare_test(cli);
    // test 1 : create
    map_create_test(cli);
    System.out.println("MOP TEST : CREATE_TEST SUCCESS!");
    // test 2 : insert
    map_insert_test(cli);
    System.out.println("MOP TEST : INSERT_TEST SUCCESS!");
    // test 3 : delete
    map_delete_test(cli);
    System.out.println("MOP TEST : DELETE_TEST SUCCESS!");
    // test 4 : get
    map_get_test(cli);
    System.out.println("MOP TEST : GET_TEST SUCCESS!");
    // test 5 : update
    map_update_test(cli);
    System.out.println("MOP TEST : UPDATE_TEST SUCCESS!");

    System.out.println("###### MOP TEST SUCCESS! ######");

    System.exit(0);
    return true;
  }

  public void map_create_test(client cli) throws Exception {
    // test 1 : create
    // created
    key = "mop_create_test";
    attr = new CollectionAttributes(cli.conf.client_exptime,
           new Long(cli.conf.ins_element_size),
           CollectionOverflowAction.error);
    colfb = cli.next_ac.asyncMopCreate(key, vtype, attr);
    System.out.printf("mop create operation request. key = " + key + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED, key);

    //exists
    colfb = cli.next_ac.asyncMopCreate(key, vtype, attr);
    System.out.printf("mop create operation request. key = " + key + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.EXISTS, key);
  }

  public void map_insert_test(client cli) throws Exception {
    // test 2 : insert
    // type_mismatch
    key = "mop_typemismatch_test";
    colfb = cli.next_ac.asyncMopInsert(key, mkey, val,
                                       null /* Do not auto-create item */);
    System.out.printf("mop insert operation request. key = " + key + ", mkey = " + mkey + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);

    // not_found
    key = "mop_insert_test";
    colfb = cli.next_ac.asyncMopInsert(key, mkey, val,
                                       null /* Do not auto-create item */);
    System.out.printf("mop insert operation request. key = " + key + ", mkey = " + mkey + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // created_stored
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(2),
                                    CollectionOverflowAction.error);
    mkey += "1";
    colfb = cli.next_ac.asyncMopInsert(key, mkey, val, attr);
    System.out.printf("mop insert operation request. key = " + key + ", mkey = " + mkey + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    // element_exists
    colfb = cli.next_ac.asyncMopInsert(key, mkey, val, attr);
    System.out.printf("mop insert operation request. key = " + key + ", mkey = " + mkey + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.ELEMENT_EXISTS, key);

    // stored
    mkey += "1";
    colfb = cli.next_ac.asyncMopInsert(key, mkey, val, attr);
    System.out.printf("mop insert operation request. key = " + key + ", mkey = " + mkey + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.STORED, key);

    // overflowed
    mkey += "1";
    colfb = cli.next_ac.asyncMopInsert(key, mkey, val, attr);
    System.out.printf("mop insert operation request. key = " + key + ", mkey = " + mkey + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.OVERFLOWED, key);
  }

  public void map_delete_test(client cli) throws Exception {
    // test 3 : delete
    // deleted
    key = "mop_delete_test";
    mkey = "mkey1";
    colfb = cli.next_ac.asyncMopDelete(key, mkey, true /* dropIfEmpty */);
    System.out.printf("mop delete operation request. key = " + key + ", mkey = " + mkey + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED, key);

    // not_found_element
    colfb = cli.next_ac.asyncMopDelete(key, mkey, true /* dropIfEmpty */);
    System.out.printf("mop delete operation request. key = " + key + ", mkey = " + mkey + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND_ELEMENT, key);

    // deleted_dropped
    mkey = "mkey2";
    colfb = cli.next_ac.asyncMopDelete(key, mkey, true /* dropIfEmpty */);
    System.out.printf("mop delete operation request. key = " + key + ", mkey = " + mkey + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED_DROPPED, key);

    // not_found
    colfb = cli.next_ac.asyncMopDelete(key, mkey, true /* dropIfEmpty */);
    System.out.printf("mop delete operation request. key = " + key + ", mkey = " + mkey + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);
  }

  public void map_get_test(client cli) throws Exception {
    // test 4 : get
    // type_mismatch
    key = "mop_typemismatch_test";
    colfm = cli.next_ac.asyncMopGet(key
                                  , true /* withDelete */
                                  , true /* dropIfEmpty */);
    System.out.printf("mop get operation request. key = " + key + "\n");
    mapval = colfm.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfm.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);

    // unreadable
    key = "mop_unreadable";
    colfm = cli.next_ac.asyncMopGet(key
                                  , true /* withDelete */
                                  , true /* dropIfEmpty */);
    System.out.printf("mop get operation request. key = " + key + "\n");
    mapval = colfm.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfm.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.UNREADABLE, key);

    // not_found
    key = "mop_get_test2";
    colfm = cli.next_ac.asyncMopGet(key
                                  , true /* withDelete */
                                  , true /* dropIfEmpty */);
    System.out.printf("mop get operation request. key = " + key + "\n");
    mapval = colfm.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfm.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // not_found_element
    key = "mop_get_test1";
    colfm = cli.next_ac.asyncMopGet(key, "mkey0"
                                  , true /* withDelete */
                                  , true /* dropIfEmpty */);
    System.out.printf("mop get operation request. key = " + key + ", mkey = mkey0\n");
    mapval = colfm.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfm.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND_ELEMENT, key);

    // end
    List<String> mkeyList = new ArrayList<String>();
    mkey = "mkey";
    for (int i = 1; i < 10; i++) {
        mkeyList.add(mkey+i);
    }
    colfm = cli.next_ac.asyncMopGet(key, mkeyList
                                  , false /* withDelete */
                                  , false /* dropIfEmpty */);
    System.out.printf("mop get operation request. key = " + key + ", mkey = mkey1 ~ 9\n");
    mapval = colfm.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfm.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.END, key);

    // deleted
    colfm = cli.next_ac.asyncMopGet(key, mkeyList
                                  , true /* withDelete */
                                  , true /* dropIfEmpty */);
    System.out.printf("mop get operation request. key = " + key + ", mkey = mkey1 ~ 9\n");
    mapval = colfm.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfm.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED, key);

    // deleted_dropped
    colfm = cli.next_ac.asyncMopGet(key, "mkey10"
                                  , true /* withDelete */
                                  , true /* dropIfEmpty */);
    System.out.printf("mop get operation request. key = " + key + ", mkey = mkey10\n");
    mapval = colfm.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfm.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED_DROPPED, key);
  }

  public void map_update_test(client cli) throws Exception {
    // test 5 : update
    // not_found
    key = "mop_update_test2";
    mkey = "mkey1";
    val = cli.vset.get_value();
    colfb = cli.next_ac.asyncMopUpdate(key, mkey, val);
    System.out.printf("mop update operation request. key = " + key + ", mkey = " + mkey + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // not_found_element
    key = "mop_update_test1";
    mkey = "mkey2";
    colfb = cli.next_ac.asyncMopUpdate(key, mkey, "mvalue");
    System.out.printf("mop update operation request. key = " + key + ", mkey = " + mkey + ", val = mvalue\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND_ELEMENT, key);

    // updated
    mkey = "mkey1";
    colfb = cli.next_ac.asyncMopUpdate(key, mkey, "update_mvalue");
    System.out.printf("mop update operation request. key = " + key + ", mkey = " + mkey + ", val = update_mvalue\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.UPDATED, key);

    // typemismatch
    key = "mop_typemismatch_test";
    colfb = cli.next_ac.asyncMopUpdate(key, mkey, "update_mvalue");
    System.out.printf("mop update operation request. key = " + key + ", mkey = " + mkey + ", val = update_mvalue\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);
  }

  public void prepare_test(client cli) throws Exception {
    mkey = "mkey";
    // typemismatch
    key = "mop_typemismatch_test";
    val = cli.vset.get_value();
    simplefb = cli.next_ac.set(key, cli.conf.client_exptime, val, raw_transcoder.raw_tc); //typemismatch key insert(simple kv)
    System.out.printf("set operation request. key = " + key + ", val = " + val + "\n");
    ok = simplefb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "sop_typemismatch_test failed, predicted STORED";

    // prepare delete test
    key = "mop_delete_test";
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(cli.conf.ins_element_size),
                                    CollectionOverflowAction.error);
    for (int i = 1; i < 3 ; i++) { // mkey1 ~ mkey2
      val = cli.vset.get_value();
      colfb = cli.next_ac.asyncMopInsert(key, mkey+i, val, attr);
    System.out.printf("mop insert operation request. key = " + key + ", mkey = " + mkey + ", val = " + val + "\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "sop_get_test failed, predicted STORED or CREATED_STORED\n";
    }

    // prepare get test
    key = "mop_get_test1";
    for (int i = 1; i < 11 ; i++) { // mkey1 ~ mkey10
      val = cli.vset.get_value();
      colfb = cli.next_ac.asyncMopInsert(key, mkey+i, val, attr);
    System.out.printf("mop insert operation request. key = " + key + ", mkey = " + mkey + ", val = " + val + "\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "sop_get_test failed, predicted STORED or CREATED_STORED\n";
    }

    // unreadable
    key = "mop_unreadable";
    attr.setReadable(false);
    colfb = cli.next_ac.asyncMopInsert(key, "unreadable", val, attr);
    System.out.printf("mop insert operation request. key = " + key + ", mkey = unreadable, val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    // prepare update test
    key = "mop_update_test1";
    attr.setReadable(true);
    colfb = cli.next_ac.asyncMopInsert(key, "mkey1", val, attr);
    System.out.printf("mop insert operation request. key = " + key + ", mkey = mkey1, val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);
  }

  public void check_response(CollectionResponse res, CollectionResponse confirm, String tname) {
    assert res.equals(confirm)
         : tname + " failed\n"
         + "predicted       : " + confirm.toString() + "\n"
         + "response string : " + res.toString() + "\n";
  }
  
}
