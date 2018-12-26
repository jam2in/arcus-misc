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
import java.util.Set;

import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.ElementValueType;
import net.spy.memcached.internal.CollectionFuture;
import net.spy.memcached.ops.CollectionOperationStatus;

public class integration_set implements client_profile {
  String key;
  boolean ok;

  ElementValueType vtype;
  CollectionAttributes attr;
  CollectionResponse response;
  CollectionFuture<Boolean> colfb;      /* collection future boolean */
  CollectionFuture<Set<Object>> colfs;  /* collection future set : for sop get test */
  Future<Boolean> simplefb;             /* simplekv future boolean */

  Set<Object> setval; /* for sop get operation */
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
    String delkey[] = {"sop_create_test"
                     , "sop_typemismatch_test"
                     , "sop_insert_test"
                     , "sop_delete_test1"
                     , "sop_delete_test2"
                     , "sop_get_test1"
                     , "sop_get_test2"
                     , "sop_get_test3"
                     , "sop_unreadable"
                     , "sop_exist_test1"
                     , "sop_exist_test2"};

    for (int i = 0; i < delkey.length; i++) {
      if (!cli.before_request(false)) return false;

      simplefb = cli.next_ac.delete(delkey[i]);
      ok = simplefb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

      if (!cli.after_request(true)) return false;
    }
    // prepare test
    System.out.println("####### SOP TEST START! #######");
    prepare_test(cli);
    // test 1 : create
    set_create_test(cli);
    System.out.println("SOP TEST : CREATE_TEST SUCCESS!");
    // test 2 : insert
    set_insert_test(cli);
    System.out.println("SOP TEST : INSERT_TEST SUCCESS!");
    // test 3 : delete
    set_delete_test(cli);
    System.out.println("SOP TEST : DELETE_TEST SUCCESS!");
    // test 4 : get
    set_get_test(cli);
    System.out.println("SOP TEST : GET_TEST SUCCESS!");
    // test 5 : exist
    set_exist_test(cli);
    System.out.println("SOP TEST : EXIST_TEST SUCCESS!");

    System.out.println("###### SOP TEST SUCCESS! ######");

    System.exit(0);
    return true;
  }

  public void set_create_test(client cli) throws Exception {
    // test 1 : create
    // created
    key = "sop_create_test";
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(cli.conf.ins_element_size),
                                    CollectionOverflowAction.error);
    colfb = cli.next_ac.asyncSopCreate(key, vtype, attr);
    System.out.printf("sop create operation request. key = " + key + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED, key);

    // exists
    colfb = cli.next_ac.asyncSopCreate(key, vtype, attr);
    System.out.printf("sop create operation request. key = " + key + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.EXISTS, key);
  }

  public void set_insert_test(client cli) throws Exception {
    // test 2 : insert
    // type_mismatch
    key = "sop_typemismatch_test";
    colfb = cli.next_ac.asyncSopInsert(key, val,
                                       null /* Do not auto-create item */);
    System.out.printf("sop insert operation request. key = " + key + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);

    // not_found
    key = "sop_insert_test";
    colfb = cli.next_ac.asyncSopInsert(key, val,
                                       null /* Do not auto-create item */);
    System.out.printf("sop insert operation request. key = " + key + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // created_stored
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(2),
                                    CollectionOverflowAction.error);
    val = cli.vset.get_value();
    colfb = cli.next_ac.asyncSopInsert(key, val, attr);
    System.out.printf("sop insert operation request. key = " + key + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    // element_exists
    colfb = cli.next_ac.asyncSopInsert(key, val, attr);
    System.out.printf("sop insert operation request. key = " + key + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.ELEMENT_EXISTS, key);

    // stored
    val = cli.vset.get_value();
    colfb = cli.next_ac.asyncSopInsert(key, val, attr);
    System.out.printf("sop insert operation request. key = " + key + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.STORED, key);

    // overflowd
    val = cli.vset.get_value();
    colfb = cli.next_ac.asyncSopInsert(key, val, attr);
    System.out.printf("sop insert operation request. key = " + key + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.OVERFLOWED, key);
  }

  public void set_delete_test(client cli) throws Exception {
    // test 3 : delete
    // not_found
    key = "sop_delete_test2";
    colfb = cli.next_ac.asyncSopDelete(key, "notexist", true /* dropIfEmpty */);
    System.out.printf("sop delete operation request. key = " + key + ", val = notexist\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // type_mismatch
    key = "sop_typemismatch_test";
    val = cli.vset.get_value();
    colfb = cli.next_ac.asyncSopDelete(key, val, true /* dropIfEmpty */);
    System.out.printf("sop insert operation request. key = " + key + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);

    // not_found_element
    key = "sop_delete_test1";
    colfb = cli.next_ac.asyncSopDelete(key, "snum10", true /* dropIfEmpty */);
    System.out.printf("sop delete operation request. key = " + key + ", val = snum10\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND_ELEMENT, key);

    // deleted
    colfb = cli.next_ac.asyncSopDelete(key, "snum01", true /* dropIfEmpty */);
    System.out.printf("sop delete operation request. key = " + key + ", val = snum01\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED, key);

    // deleted_dropped
    colfb = cli.next_ac.asyncSopDelete(key, "snum02", true /* dropIfEmpty */);
    System.out.printf("sop delete operation request. key = " + key + ", val = snum02\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED_DROPPED, key);
  }

  public void set_get_test(client cli) throws Exception {
    // test 4 : get
    // not_found
    key = "sop_get_test3";
    colfs = cli.next_ac.asyncSopGet(key, cli.conf.act_element_size
                                  , false /* withDelete */
                                  , false /* dropIfEmpty */);
    System.out.printf("sop get operation request. key = " + key + ",val = " + cli.conf.act_element_size + "\n");
    setval = colfs.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfs.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // not_found_element
    key = "sop_get_test2";
    colfs = cli.next_ac.asyncSopGet(key, 1
                                  , true /* withDelete */
                                  , true /* dropIfEmpty */);
    System.out.printf("sop get operation request. key = " + key + ", val = 1\n");
    setval = colfs.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfs.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND_ELEMENT, key);

    // end
    key = "sop_get_test1";
    colfs = cli.next_ac.asyncSopGet(key, 10
                                  , false /* withDelete */
                                  , false /* dropIfEmpty */);
    System.out.printf("sop get operation request. key = " + key + ", val = 10\n");
    setval = colfs.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfs.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.END, key);

    // deleted
    colfs = cli.next_ac.asyncSopGet(key, 9
                                  , true /* withDelete */
                                  , true /* dropIfEmpty */);
    System.out.printf("sop get operation request. key = " + key + ", val = 9\n");
    setval = colfs.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfs.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED, key);

    // deleted_dropped
    colfs = cli.next_ac.asyncSopGet(key, 1
                                  , true /* withDelete */
                                  , true /* dropIfEmpty */);
    System.out.printf("sop get operation request. key = " + key + ", val = 1\n");
    setval = colfs.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfs.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED_DROPPED, key);

    // unreadabl
    key = "sop_unreadable";
    colfs = cli.next_ac.asyncSopGet(key, 100
                                  , true /* withDelete */
                                  , true /* dropIfEmpty */);
    System.out.printf("sop get operation request. key = " + key + ", val = 100\n");
    setval = colfs.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfs.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.UNREADABLE, key);

    // type_mismatch
    key = "sop_typemismatch_test";
    colfs = cli.next_ac.asyncSopGet(key, 100
                                  , true /* withDelete */
                                  , true /* dropIfEmpty */);
    System.out.printf("sop get operation request. key = " + key + ", val = 100\n");
    setval = colfs.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfs.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);
  }

  public void set_exist_test(client cli) throws Exception {
    // test 5 : exist
    // not_found
    key = "sop_exist_test2";
    colfb = cli.next_ac.asyncSopExist(key, "snum01");
    System.out.printf("sop exist operation request. key = " + key + ", val = snum01\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // exist
    key = "sop_exist_test1";
    colfb = cli.next_ac.asyncSopExist(key, "snum01");
    System.out.printf("sop exist operation request. key = " + key + ", val = snum01\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.EXIST, key);

    // not_exist
    key = "sop_exist_test1";
    colfb = cli.next_ac.asyncSopExist(key, "snum10");
    System.out.printf("sop exist operation request. key = " + key + ", val = snum10\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_EXIST, key);

    // unreadable
    key = "sop_unreadable";
    colfb = cli.next_ac.asyncSopExist(key, "snum10");
    System.out.printf("sop exist operation request. key = " + key + ", val = snum10\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.UNREADABLE, key);

    // type_mismatch
    key = "sop_typemismatch_test";
    colfb = cli.next_ac.asyncSopExist(key, "snum10");
    System.out.printf("sop exist operation request. key = " + key + ", val = snum10\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);
  }

  public void prepare_test(client cli) throws Exception {
    // typemismatch
    key = "sop_typemismatch_test";
    val = cli.vset.get_value();
    simplefb = cli.next_ac.set(key, cli.conf.client_exptime, val, raw_transcoder.raw_tc); //typemismatch key insert(simple kv)
    System.out.printf("set operation request. key = " + key + ", val = " + val + "\n");
    ok = simplefb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "sop_typemismatch_test failed, predicted STORED";

    // prepare delete test
    key = "sop_delete_test1";
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(cli.conf.ins_element_size),
                                    CollectionOverflowAction.error);
    colfb = cli.next_ac.asyncSopInsert(key, "snum01", attr);
    System.out.printf("sop insert operation request. key = " + key + ", val = snum01\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    colfb = cli.next_ac.asyncSopInsert(key, "snum02", attr);
    System.out.printf("sop insert operation request. key = " + key + ", val = snum02\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.STORED, key);

    // prepare get test
    key = "sop_get_test1";
    for (int i = 0; i < 10; i++) {
      colfb = cli.next_ac.asyncSopInsert(key, "snum" + i, attr);
    System.out.printf("sop insert operation request. key = " + key + ", val = snum"+i+"\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "sop_get_test failed, predicted STORED or CREATED_STORED\n";
    }

    key = "sop_get_test2";
    vtype = ElementValueType.BYTEARRAY;
    colfb = cli.next_ac.asyncSopCreate(key, vtype, attr);
    System.out.printf("sop create operation request. key = " + key + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED, key);

    // unreadable
    key = "sop_unreadable";
    attr.setReadable(false);
    colfb = cli.next_ac.asyncSopInsert(key, "unreadable", attr);
    System.out.printf("sop insert operation request. key = " + key + ", val = unreadable\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    // prepare exist test
    key = "sop_exist_test1";
    attr.setReadable(true);
    colfb = cli.next_ac.asyncSopInsert(key, "snum01", attr);
    System.out.printf("sop insert operation request. key = " + key + ", val = snum01\n");
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
