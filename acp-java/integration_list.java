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

import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.ElementValueType;
import net.spy.memcached.internal.CollectionFuture;
import net.spy.memcached.ops.CollectionOperationStatus;

public class integration_list implements client_profile {
  String key;
  boolean ok;

  ElementValueType vtype;
  CollectionAttributes attr;
  CollectionResponse response;
  CollectionFuture<Boolean> colfb;      /* collection future boolean */
  CollectionFuture<List<Object>> colfl; /* collection future list : for lop get test */
  Future<Boolean> simplefb;             /* simplekv future boolean */

  List<Object> listval; /* for lop get operation */
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
    String delkey[] = {"lop_create_test"
                     , "lop_typemismatch_test"
                     , "lop_insert_test"
                     , "lop_delete_test1"
                     , "lop_delete_test2"
                     , "lop_get_test1"
                     , "lop_get_test2"
                     , "lop_unreadable"};

    for (int i = 0; i < delkey.length; i++) {
      if (!cli.before_request(false)) return false;

      simplefb = cli.next_ac.delete(delkey[i]);
      ok = simplefb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

      if (!cli.after_request(true)) return false;
    }
    // prepare test
    System.out.println("####### LOP TEST START! #######");
    prepare_test(cli);
    // test 1 : create
    list_create_test(cli);
    System.out.println("LOP TEST : CREATE_TEST SUCCESS!");
    // test 2 : insert
    list_insert_test(cli);
    System.out.println("LOP TEST : INSERT_TEST SUCCESS!");
    // test 3 : delete
    list_delete_test(cli);
    System.out.println("LOP TEST : DELETE_TEST SUCCESS!");
    // test 4 : get
    list_get_test(cli);
    System.out.println("LOP TEST : GET_TEST SUCCESS!");
    System.out.println("###### LOP TEST SUCCESS! ######");

    System.exit(0);
    return true;
  }

  public void list_create_test(client cli) throws Exception {
    // test 1 : create
    // created
    key = "lop_create_test";
    vtype = ElementValueType.BYTEARRAY;
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(4000),
                                    CollectionOverflowAction.head_trim);
    colfb = cli.next_ac.asyncLopCreate(key, vtype, attr);
    System.out.printf("lop create operation request. key = " + key + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED, key);

    // exists
    colfb = cli.next_ac.asyncLopCreate(key, vtype, attr); //exist key create
    System.out.printf("lop create operation request. key = " + key + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.EXISTS, key);
  }

  public void list_insert_test(client cli) throws Exception {
    // test 2 : insert
    // type_mismatch
    key = "lop_typemismatch_test";
    colfb = cli.next_ac.asyncLopInsert(key, -1 /* tail */, val,
                                       null /* Do not auto-create item */);
    System.out.printf("lop insert operation request. key = " + key + ", index = -1, val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);

    // not_found
    key = "lop_insert_test";
    colfb = cli.next_ac.asyncLopInsert(key, -1 /* tail */, val,
                                       null /* Do not auto-create item */);
    System.out.printf("lop insert operation request. key = " + key + ", index = -1, val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // created_stored
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(2),
                                    CollectionOverflowAction.error);
    colfb = cli.next_ac.asyncLopInsert(key, -1 /* tail */, val, attr);
    System.out.printf("lop insert operation request. key = " + key + ", index = -1, val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    // stored
    colfb = cli.next_ac.asyncLopInsert(key, -1 /* tail */, val,
                                       null /* Do not auto-create item */);
    System.out.printf("lop insert operation request. key = " + key + ", index = -1, val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.STORED, key);

    // overflow
    colfb = cli.next_ac.asyncLopInsert(key, -1 /* tail */, val,
                                       null /* Do not auto-create item */);
    System.out.printf("lop insert operation request. key = " + key + ", index = -1, val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.OVERFLOWED, key);
  }

  public void list_delete_test(client cli) throws Exception {
    // test 3 : delete
    // not_found_element
    key = "lop_delete_test1";
    colfb = cli.next_ac.asyncLopDelete(key, 3, true /* dropIfEmpty */);
    System.out.printf("lop delete operation request. key = " + key + ", index = 3\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND_ELEMENT, key);

    // not_found
    key = "lop_delete_test2";
    colfb = cli.next_ac.asyncLopDelete(key, -1, true /* dropIfEmpty */);
    System.out.printf("lop delete operation request. key = " + key + ", index = -1\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // type_mismatch
    key = "lop_typemismatch_test";
    colfb = cli.next_ac.asyncLopDelete(key, -1, true /* dropIfEmpty */);
    System.out.printf("lop delete operation request. key = " + key + ", index = -1\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);

    // deleted
    key = "lop_delete_test1";
    colfb = cli.next_ac.asyncLopDelete(key, -1, true /* dropIfEmpty */);
    System.out.printf("lop delete operation request. key = " + key + ", index = -1\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED, key);

    // deleted_dropped
    key = "lop_delete_test1";
    colfb = cli.next_ac.asyncLopDelete(key, -1, true /* dropIfEmpty */);
    System.out.printf("lop delete operation request. key = " + key + ", index = -1\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED_DROPPED, key);
  }

  public void list_get_test(client cli) throws Exception {
    // test 4 : get
    // not_found
    key = "lop_get_test2";
    colfl = cli.next_ac.asyncLopGet(key, 0, true /* withDelete */, 
                                    true /* dropIfEmpty */);
    System.out.printf("lop get operation request. key = " + key + ", index = 0\n");
    listval = colfl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfl.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // not_found_element
    key = "lop_get_test1";
    colfl = cli.next_ac.asyncLopGet(key, 10, 11, true /* withDelete */, 
                                    true /* dropIfEmpty */);
    System.out.printf("lop get operation request. key = " + key + ", index = 10 ~ 11\n");
    listval = colfl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfl.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND_ELEMENT, key);

    // end
    colfl = cli.next_ac.asyncLopGet(key, 0, 9, false /* withDelete */, 
                                    false /* dropIfEmpty */);
    System.out.printf("lop get operation request. key = " + key + ", index = 0 ~ 9\n");
    listval = colfl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfl.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.END, key);

    // deleted
    colfl = cli.next_ac.asyncLopGet(key, 0, 8, true /* withDelete */, 
                                    true /* dropIfEmpty */);
    System.out.printf("lop get operation request. key = " + key + ", index = 0 ~ 8\n");
    listval = colfl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfl.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED, key);

    // deleted_dropped
    colfl = cli.next_ac.asyncLopGet(key, 0, true /* withDelete */, 
                                    true /* dropIfEmpty */);
    System.out.printf("lop get operation request. key = " + key + ", index = 0\n");
    listval = colfl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfl.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED_DROPPED, key);
    
    // unreadable
    key = "lop_unreadable";
    colfl = cli.next_ac.asyncLopGet(key, 0, true /* withDelete */, 
                                    true /* dropIfEmpty */);
    System.out.printf("lop get operation request. key = " + key + ", index = 0\n");
    listval = colfl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfl.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.UNREADABLE, key);
  
    // type_missmatch
    key = "lop_typemismatch_test";
    colfl = cli.next_ac.asyncLopGet(key, 0, true /* withDelete */, 
                                    true /* dropIfEmpty */);
    System.out.printf("lop get operation request. key = " + key + ", index = 0\n");
    listval = colfl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfl.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);
  }

  public void prepare_test(client cli) throws Exception {
    // typemismatch
    key = "lop_typemismatch_test";
    val = cli.vset.get_value();
    simplefb = cli.next_ac.set(key, cli.conf.client_exptime, val, raw_transcoder.raw_tc);
    System.out.printf("set operation request. key = " + key + ", val = " + val + "\n");
    ok = simplefb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "lop_typemismatch_test failed, predicted STORED\n"
              + "response string : " + response.toString() + "\n";

    // prepare delete test
    key = "lop_delete_test1";
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(4000),
                                    CollectionOverflowAction.tail_trim);
    colfb = cli.next_ac.asyncLopInsert(key, -1 /* tail */, val, attr);
    System.out.printf("lop insert operation request. key = " + key + ", index = -1\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    colfb = cli.next_ac.asyncLopInsert(key, -1 /* tail */, val, attr);
    System.out.printf("lop insert operation request. key = " + key + ", index = -1\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.STORED, key);

    // prepare get test
    key = "lop_get_test1";
    for (int i = 0; i < 10; i++) {
      colfb = cli.next_ac.asyncLopInsert(key, -1 /* tail */, val, attr);
      System.out.printf("lop insert operation request. key = " + key + ", index = -1\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "lop_get_test failed, predicted STORED or CREATED_STORED\n";
    }
    
    // unreadable
    key = "lop_unreadable";
    attr.setReadable(false);
    colfb = cli.next_ac.asyncLopInsert(key, -1 /* tail */, val, attr);
    System.out.printf("lop insert operation request. key = " + key + ", index = -1\n");
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
