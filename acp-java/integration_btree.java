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

import net.spy.memcached.collection.ByteArrayBKey;
import net.spy.memcached.collection.BTreeOrder;
import net.spy.memcached.collection.ElementFlagFilter;
import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.Element;
import net.spy.memcached.collection.ElementValueType;
import net.spy.memcached.internal.CollectionFuture;
import net.spy.memcached.ops.CollectionOperationStatus;

/* for mget */
import net.spy.memcached.collection.BTreeGetResult;
import net.spy.memcached.internal.CollectionGetBulkFuture;

/* for smget */
import java.util.List;
import java.util.ArrayList;
import net.spy.memcached.collection.SMGetElement;
import net.spy.memcached.collection.SMGetMode;
import net.spy.memcached.collection.SMGetTrimKey;
import net.spy.memcached.internal.SMGetFuture;


public class integration_btree implements client_profile {
  String key;
  long bkey;
  boolean ok;

  ElementValueType vtype;
  ElementFlagFilter filter;
  CollectionAttributes attr;
  CollectionResponse response;
  CollectionFuture<Boolean> colfb;                      /* collection future boolean */
  CollectionFuture<Long> colfl;                         /* collection future long */
  CollectionFuture<Integer> colfi;                      /* collection future int */
  CollectionFuture<Map<Long, Element<Object>>> colfbtr; /* collemtion future btree : for bop get test */
  Future<Boolean> simplefb;                             /* simplekv future boolean */

  Map<Long, Element<Object>> btrval; /* for bop get operation */
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
    String delkey[] = {"bop_create_test"
                     , "bop_typemismatch_test"
                     , "bop_insert_test1"
                     , "bop_insert_test2"
                     , "bop_delete_test1"
                     , "bop_delete_test2"
                     , "bop_get_test1"
                     , "bop_get_test2"
                     , "bop_unreadable"
                     , "bop_update_test1"
                     , "bop_update_test2"
                     , "bop_count_test1"
                     , "bop_count_test2"
                     , "bop_arithmetic_test1"
                     , "bop_arithmetic_test2"
                     /* mget, smget test */
                     , "bop_mget_test1"
                     , "bop_mget_test2"
                     , "bop_mget_test3"
                     , "bop_mget_test4"
                     , "bop_smget_test1"
                     , "bop_smget_test2"
                     , "bop_smget_test3"

                     , "bop_position_test1"
                     , "bop_position_test2"
                     , "bop_pwg_test1"
                     , "bop_pwg_test2"
                     , "bop_gbp_test"
                     , "bop_update_test2"};

    for (int i = 0; i < delkey.length; i++) {
      if (!cli.before_request(false)) return false;

      simplefb = cli.next_ac.delete(delkey[i]);
      ok = simplefb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

      if (!cli.after_request(true)) return false;
    }
    // prepare test
    System.out.println("####### BOP TEST START! #######");
    prepare_test(cli);
    // test 1 : create
    btree_create_test(cli);
    System.out.println("BOP TEST : CREATE_TEST SUCCESS!");
    // test 2 : insert
    btree_insert_test(cli);
    System.out.println("BOP TEST : INSERT_TEST SUCCESS!");
    // test 3 : delete
    btree_delete_test(cli);
    System.out.println("BOP TEST : DELETE_TEST SUCCESS!");
    // test 4 : get
    btree_get_test(cli);
    System.out.println("BOP TEST : GET_TEST SUCCESS!");
    // test 5 : update
    btree_update_test(cli);
    System.out.println("BOP TEST : UPDATE_TEST SUCCESS!");
    // test 6 : count
    btree_count_test(cli);
    System.out.println("BOP TEST : COUNT_TEST SUCCESS!");
    // test 7 : arithmetic(incr|decr)
    btree_arithmetic_test(cli);
    System.out.println("BOP TEST : ARITHMETIC_TEST SUCCESS!");
    // test 8 : mget
    btree_mget_test(cli);
    System.out.println("BOP TEST : MGET_TEST SUCCESS!");
    // test 9 : smget
    btree_smget_test(cli);
    System.out.println("BOP TEST : SMGET_TEST SUCCESS!");
    // test 10 : position
    btree_position_test(cli);
    System.out.println("BOP TEST : POSITION_TEST SUCCESS!");
    // test 11 : pwg
    btree_pwg_test(cli);
    System.out.println("BOP TEST : PWG_TEST SUCCESS!");
    // test 12 : gbp
    btree_gbp_test(cli);
    System.out.println("BOP TEST : GBP_TEST SUCCESS!");

    System.out.println("###### BOP TEST SUCCESS! ######");

    System.exit(0);
    return true;
  }

  public void btree_create_test(client cli) throws Exception {
    // test 1 : create
    // create
    key = "bop_create_test";
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(cli.conf.ins_element_size),
                                    CollectionOverflowAction.smallest_trim);
    colfb = cli.next_ac.asyncBopCreate(key, vtype, attr);
    System.out.printf("bop create operation request. key = " + key + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED, key);

    //exists
    colfb = cli.next_ac.asyncBopCreate(key, vtype, attr);
    System.out.printf("bop create operation request. key = " + key + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.EXISTS, key);
  }

  public void btree_insert_test(client cli) throws Exception {
    // test 2 : insert
    // typemismatch
    key = "bop_typemismatch_test";
    bkey = 1L;
    val = cli.vset.get_value();
    colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                       val, null /* Do not auto-create item */);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", value = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);

    // not_found
    key = "bop_insert_test1";
    bkey = 10L;
    colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                       val, null /* Do not auto-create item */);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", value = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // created_stored
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(2),
                                    CollectionOverflowAction.error);
    colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                       val, attr /* Do not auto-create item */);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", value = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    // element_exists
    colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                       val, attr /* Do not auto-create item */);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", value = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.ELEMENT_EXISTS, key);

    // stored
    bkey = 20L;
    colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                       val, attr /* Do not auto-create item */);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", value = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.STORED, key);

    // overflowed
    bkey = 30L;
    colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                       val, attr /* Do not auto-create item */);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", value = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.OVERFLOWED, key);

    // out_of_range
    key = "bop_insert_test2";
    attr.setOverflowAction(CollectionOverflowAction.largest_trim);
    attr.setMaxCount(1);
    colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                       val, attr /* Do not auto-create item */);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", value = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    bkey = 40L;
    colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                       val, attr /* Do not auto-create item */);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", value = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.OUT_OF_RANGE, key);
  }

  public void btree_delete_test(client cli) throws Exception {
    // test 3 : delete
    // not_found
    key = "bop_delete_test2";
    bkey = 10;
    colfb = cli.next_ac.asyncBopDelete(key, bkey, null /* eflag */,
                                      true /* dropIfEmpty */);
    System.out.printf("bop delete operation request. key = " + key + ", bkey = " + bkey + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // not_found_element
    key = "bop_delete_test1";
    bkey = 200;
    colfb = cli.next_ac.asyncBopDelete(key, bkey, null /* eflag */,
                                      true /* dropIfEmpty */);
    System.out.printf("bop delete operation request. key = " + key + ", bkey = " + bkey + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND_ELEMENT, key);

    // deleted
    bkey = 20;
    colfb = cli.next_ac.asyncBopDelete(key, bkey, null /* eflag */,
                                      true /* dropIfEmpty */);
    System.out.printf("bop delete operation request. key = " + key + ", bkey = " + bkey + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED, key);

    // deleted_dropped
    bkey = 10;
    colfb = cli.next_ac.asyncBopDelete(key, bkey, null /* eflag */,
                                      true /* dropIfEmpty */);
    System.out.printf("bop delete operation request. key = " + key + ", bkey = " + bkey + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED_DROPPED, key);

    // type_mismatch
    key = "bop_typemismatch_test";
    colfb = cli.next_ac.asyncBopDelete(key, bkey, null /* eflag */,
                                      true /* dropIfEmpty */);
    System.out.printf("bop delete operation request. key = " + key + ", bkey = " + bkey + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);
  }

  public void btree_get_test(client cli) throws Exception {
    // test 4 : get
    // not_found
    key = "bop_get_test2";
    bkey = 200;
    colfbtr = cli.next_ac.asyncBopGet(key, bkey, filter
                                    , true /* withdelete */
                                    , true /* dropifempty */);
    System.out.printf("bop get operation request. key = " + key + ", bkey = " + bkey + "\n");
    btrval = colfbtr.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfbtr.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // not_found_element
    key = "bop_get_test1";
    colfbtr = cli.next_ac.asyncBopGet(key, bkey, filter
                                    , true /* withdelete */
                                    , true /* dropifempty */);
    System.out.printf("bop get operation request. key = " + key + ", bkey = " + bkey + "\n");
    btrval = colfbtr.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfbtr.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND_ELEMENT, key);

    // end
    colfbtr = cli.next_ac.asyncBopGet(key, 10, 100, filter, 0, 0
                                    , false /* withdelete */
                                    , false /* dropifempty */);
    System.out.printf("bop get operation request. key = " + key + ", bkey = 10 ~ 100\n");
    btrval = colfbtr.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfbtr.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.END, key);

    // deleted
    colfbtr = cli.next_ac.asyncBopGet(key, 20, 100, filter, 0, 0
                                    , true /* withdelete */
                                    , true /* dropifempty */);
    System.out.printf("bop get operation request. key = " + key + ", bkey = 20 ~ 100\n");
    btrval = colfbtr.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfbtr.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED, key);

    // deleted_dropped
    colfbtr = cli.next_ac.asyncBopGet(key, 10, filter
                                    , true /* withdelete */
                                    , true /* dropifempty */);
    System.out.printf("bop get operation request. key = " + key + ", bkey = 10\n");
    btrval = colfbtr.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfbtr.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.DELETED_DROPPED, key);

    // unreadable
    key = "bop_unreadable";
    colfbtr = cli.next_ac.asyncBopGet(key, 10, filter
                                    , true /* withdelete */
                                    , true /* dropifempty */);
    System.out.printf("bop get operation request. key = " + key + ", bkey = 10\n");
    btrval = colfbtr.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfbtr.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.UNREADABLE, key);

    // type_mismatch
    key = "bop_typemismatch_test";
    colfbtr = cli.next_ac.asyncBopGet(key, 10, filter
                                    , true /* withdelete */
                                    , true /* dropifempty */);
    System.out.printf("bop get operation request. key = " + key + ", bkey = 10\n");
    btrval = colfbtr.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfbtr.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);
  }

  public void btree_update_test(client cli) throws Exception {
    // test 5 : update
    // not_found
    key = "bop_update_test2";
    bkey = 100;
    colfb = cli.next_ac.asyncBopUpdate(key, bkey, null, "updatevalue");
    System.out.printf("bop update operation request. key = " + key + ", bkey = " + bkey + ", val = updatevalue\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // not_found_element
    key = "bop_update_test1";
    bkey = 30;
    colfb = cli.next_ac.asyncBopUpdate(key, bkey, null, "updatevalue");
    System.out.printf("bop update operation request. key = " + key + ", bkey = " + bkey + ", val = updatevalue\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND_ELEMENT, key);

    // updated
    bkey = 20;
    colfb = cli.next_ac.asyncBopUpdate(key, bkey, null, "updatevalue");
    System.out.printf("bop update operation request. key = " + key + ", bkey = " + bkey + ", val = updatevalue\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.UPDATED, key);

    // type_mismatch
    key = "bop_typemismatch_test";
    colfb = cli.next_ac.asyncBopUpdate(key, bkey, null, "updatevalue");
    System.out.printf("bop update operation request. key = " + key + ", bkey = " + bkey + ", val = updatevalue\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);
  }

  public void btree_count_test(client cli) throws Exception {
    // test 6 : count
    // not_found
    int count;
//    key = "bop_count_test2";
//    colfi = cli.next_ac.asyncBopGetItemCount(key, 10, 100, ElementFlagFilter.DO_NOT_FILTER);
//    count = colfi.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
//    response = colfi.getOperationStatus().getResponse();
//
//    check_response(response, CollectionResponse.NOT_FOUND, key);
//
//     // unreadable
//    key = "bop_unreadable";
//    colfi = cli.next_ac.asyncBopGetItemCount(key, 10, 100, ElementFlagFilter.DO_NOT_FILTER);
//    count = colfi.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
//    response = colfi.getOperationStatus().getResponse();
//
//    check_response(response, CollectionResponse.UNREADABLE, key);

    // count
    key = "bop_count_test1";
    colfi = cli.next_ac.asyncBopGetItemCount(key, 10, 100, ElementFlagFilter.DO_NOT_FILTER);
    System.out.printf("bop count operation request. key = " + key + ", bkey = 10 ~ 100\n");
    count = colfi.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfi.getOperationStatus().getResponse();

    assert (count == 2) : "bop_count_test1 failed, predicted count == 2";
    check_response(response, CollectionResponse.END, key);
    
    // type_mismatch
//    key = "bop_typemismatch_test";
//    colfi = cli.next_ac.asyncBopGetItemCount(key, 10, 100, ElementFlagFilter.DO_NOT_FILTER);
//    count = colfi.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
//    response = colfi.getOperationStatus().getResponse();
//
//    check_response(response, CollectionResponse.TYPE_MISMATCH, key);

  }

  public void btree_arithmetic_test(client cli) throws Exception {
    // test 7 : arithmetic(incr|decr)
    // incr
    key = "bop_arithmetic_test1";
    bkey = 10;
    colfl = cli.next_ac.asyncBopIncr(key, bkey, (int)1000);
    System.out.printf("bop incr operation request. key = " + key + ", bkey = " + bkey + ", val = 1000\n");
    Long arithval = colfl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert arithval == 2000 : "bop_arithmetic_test(increase) failed, pridicted 2000";
 
    // decr
    colfl = cli.next_ac.asyncBopDecr(key, bkey, (int)1000);
    System.out.printf("bop decr operation request. key = " + key + ", bkey = " + bkey + ", val = 1000\n");
    arithval = colfl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert arithval == 1000 : "bop_arithmetic_test(decrease) failed, pridicted 1000";
  }

  public void btree_mget_test(client cli) throws Exception {
    // test 8 : mget
    List<String> keyList = new ArrayList<String>() {{
        add("bop_mget_test1");
        add("bop_mget_test2");
        add("bop_mget_test3");
        add("bop_mget_test4");
    }};

    CollectionGetBulkFuture<Map<String, BTreeGetResult<Long, Object>>> colfmget;
    Map<String, BTreeGetResult<Long, Object>> result;
    colfmget = cli.next_ac.asyncBopGetBulk(keyList, 10, 20, ElementFlagFilter.DO_NOT_FILTER
                                         , 0 /* offset */, 10 /* count */);
    System.out.printf("bop getbulk operation request. key = [bop_mget_test1, bop_mget_test2, bop_mget_test3, bop_mget_test4], bkey = 10 ~ 20\n");
    result = colfmget.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    if (result.size() <= 0) {
        assert false : "bop_mget_test failed, result size <= 0";
    }

    for (Map.Entry<String, BTreeGetResult<Long, Object>> entry : result.entrySet()) {
        response = entry.getValue().getCollectionResponse().getResponse();
        key = entry.getKey();
        if (entry.getKey().equals("bop_mget_test1")) { //ok
            check_response(response, CollectionResponse.OK, key);
        } else if (entry.getKey().equals("bop_mget_test2")) { //trimmed
            check_response(response, CollectionResponse.TRIMMED, key);
        } else if (entry.getKey().equals("bop_mget_test3")) { //not_found_element
            check_response(response, CollectionResponse.NOT_FOUND_ELEMENT, key);
        } else if (entry.getKey().equals("bop_mget_test4")) { //not_found
            check_response(response, CollectionResponse.NOT_FOUND, key);
        }
    }
  }

  public void btree_smget_test(client cli) throws Exception {
    // test 9 : smget
    List<String> keyList = new ArrayList<String>() {{
        add("bop_smget_test1");
        add("bop_smget_test2");
        add("bop_smget_test3");
    }};
    SMGetMode smgetMode = SMGetMode.DUPLICATE;
    SMGetFuture<List<SMGetElement<Object>>> colfsmget;
    List<SMGetElement<Object>> result;
    colfsmget = cli.next_ac.asyncBopSortMergeGet(keyList, 40, 10, ElementFlagFilter.DO_NOT_FILTER, 10, smgetMode);
    System.out.printf("bop smget operation request. key = [bop_smget_test1, bop_smget_test2, bop_smget_test3], bkey = 40 ~ 10\n");
    result = colfsmget.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    if (result.size() <= 0) {
        assert false : "bop_smget_test failed, result size <= 0";
    }

    for (SMGetElement<Object> element : result)
        assert element.getKey().equals("bop_smget_test1")
             : "bop_smget_test failed, Missmatched \"found key\"";

    for (Map.Entry<String, CollectionOperationStatus> m : colfsmget.getMissedKeys().entrySet()) {
        response = m.getValue().getResponse();
        key = m.getKey();
        if (m.getKey().equals("bop_smget_test2")) { // out_of_range
            check_response(response, CollectionResponse.OUT_OF_RANGE, key);
        } else if (m.getKey().equals("bop_smget_test3")) { // not_found
            check_response(response, CollectionResponse.NOT_FOUND, key);
        } else {
            assert false : "bop_smget_test failed, Missmatched \"Missed key\" response";
        }
    }

    for (SMGetTrimKey e : colfsmget.getTrimmedKeys())
        assert e.getKey().equals("bop_smget_test1")
             : "bop_smget_test failed, Missmatched \"trimmed key\"";

  }

  public void btree_position_test(client cli) throws Exception {
    // test 10 : position
    // position
    int position;
    key = "bop_position_test1";
    bkey = 50;
    colfi = cli.next_ac.asyncBopFindPosition(key, bkey, BTreeOrder.ASC);
    System.out.printf("bop position operation request. key = " + key + ", bkey = " + bkey + ", order = asc\n");
    position = colfi.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert position == 4 : "bop_position_test failed, pridicted position = 4";

    bkey = 70;
    colfi = cli.next_ac.asyncBopFindPosition(key, bkey, BTreeOrder.DESC);
    System.out.printf("bop position operation request. key = " + key + ", bkey = " + bkey + ", order = desc\n");
    position = colfi.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert position == 3 : "bop_position_test failed, pridicted position = 3";
  }

  public void btree_pwg_test(client cli) throws Exception {
    // test 11 : pwg
    // not_found
    CollectionFuture<Map<Integer, Element<Object>>> colfpwg;
    Map<Integer,Element<Object>> result;
    key = "bop_pwg_test2";
    colfpwg = cli.next_ac.asyncBopFindPositionWithGet(key, 40, BTreeOrder.ASC, 10/* pwgCount */);
    System.out.printf("bop position operation request. key = " + key + ", bkey = 40, order = asc, count = 10\n");
    result = colfpwg.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfpwg.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND, key);

    // asc
    key = "bop_pwg_test1";
    colfpwg = cli.next_ac.asyncBopFindPositionWithGet(key, 40, BTreeOrder.ASC, 10/* pwgCount */);
    System.out.printf("bop position operation request. key = " + key + ", bkey = 40, order = asc, count = 10\n");
    result = colfpwg.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfpwg.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.END, key);

    // desc
    colfpwg = cli.next_ac.asyncBopFindPositionWithGet(key, 40, BTreeOrder.DESC, 10/* pwgCount */);
    System.out.printf("bop position operation request. key = " + key + ", bkey = 40, order = desc, count = 10\n");
    result = colfpwg.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfpwg.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.END, key);

    // not_found_element
    colfpwg = cli.next_ac.asyncBopFindPositionWithGet(key, 99, BTreeOrder.DESC, 10/* pwgCount */);
    System.out.printf("bop position operation request. key = " + key + ", bkey = 99, order = desc, count = 10\n");
    result = colfpwg.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfpwg.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.NOT_FOUND_ELEMENT, key);

    // unreadable
    key = "bop_unreadable";
    colfpwg = cli.next_ac.asyncBopFindPositionWithGet(key, 40, BTreeOrder.DESC, 10/* pwgCount */);
    System.out.printf("bop position operation request. key = " + key + ", bkey = 40, order = desc, count = 10\n");
    result = colfpwg.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfpwg.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.UNREADABLE, key);

    // type_mismatch
    key = "bop_typemismatch_test";
    colfpwg = cli.next_ac.asyncBopFindPositionWithGet(key, 40, BTreeOrder.DESC, 10/* pwgCount */);
    System.out.printf("bop position operation request. key = " + key + ", bkey = 40, order = desc, count = 10\n");
    result = colfpwg.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfpwg.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.TYPE_MISMATCH, key);
  }

  public void btree_gbp_test(client cli) throws Exception {
    // test 12 : gbp
    // asc
    CollectionFuture<Map<Integer, Element<Object>>> colfgbp;
    Map<Integer,Element<Object>> result;
    key = "bop_gbp_test";
    colfgbp = cli.next_ac.asyncBopGetByPosition(key, BTreeOrder.ASC, 5 /* position */);
    System.out.printf("bop getbyposition operation request. key = " + key + ", order = asc, count = 5\n");
    result = colfgbp.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfgbp.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.END, key);

    // desc
    colfgbp = cli.next_ac.asyncBopGetByPosition(key, BTreeOrder.DESC, 5 /* position */);
    System.out.printf("bop getbyposition operation request. key = " + key + ", order = desc, count = 5\n");
    result = colfgbp.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfgbp.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.END, key);

  }

  public void prepare_test(client cli) throws Exception {
    // prepare test
    // common
    vtype = ElementValueType.STRING;
    // vtype = ElementValueType.BYTEARRAY;
    // typemismatch
    key = "bop_typemismatch_test";
    val = cli.vset.get_value();
    simplefb = cli.next_ac.set(key, cli.conf.client_exptime, val, raw_transcoder.raw_tc); //typemismatch key insert(simple kv)
    System.out.printf("set operation request. key = " + key + ", val = " + val + "\n");
    ok = simplefb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    assert ok : "sop_typemismatch_test failed, predicted STORED";

    // prepare delete test
    key = "bop_delete_test1";
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(cli.conf.ins_element_size),
                                    CollectionOverflowAction.error);
    val = cli.vset.get_value();
    for (long i = 10; i < 30 ; i += 10) { // 10, 20
      bkey = i;
      colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                         val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "bop_delete_test failed, predicted STORED or CREATED_STORED";
    }

    // prepare get test
    key = "bop_get_test1";
    for (long i = 10; i < 110 ; i += 10) { // 10, 20,....90, 100
      bkey = i;
      colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                         val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "bop_delete_test failed, predicted STORED or CREATED_STORED";
    }

    // unreadable
    key = "bop_unreadable";
    attr.setReadable(false);
    colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                       val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
    attr.setReadable(true);
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    // prepare update test
    key = "bop_update_test1";
    for (long i = 10; i < 30 ; i += 10) { // 10, 20
      bkey = i;
      colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                         val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "bop_update_test failed, predicted STORED or CREATED_STORED";
    }

    // prepare count test
    key = "bop_count_test1";
    for (long i = 10; i < 30 ; i += 10) { // 10, 20
      bkey = i;
      colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                         val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "bop_count_test failed, predicted STORED or CREATED_STORED";
    }

    // prepare arithmetic test
    String str;
    key = "bop_arithmetic_test1";
    bkey = 10;
    str = String.format("%d", bkey * 100);
    val = str.getBytes();
    colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                       val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    // prepare mget test
    key = "bop_mget_test1";
    bkey = 10;
    colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                       val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    key = "bop_mget_test2";
    attr.setMaxCount(new Long(2));
    attr.setOverflowAction(CollectionOverflowAction.smallest_trim);
    for (long i = 10; i < 40 ; i += 10) { // 10, 20, 30
      bkey = i;
      colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                         val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "bop_mget_test failed, predicted STORED or CREATED_STORED";
    }

    key = "bop_mget_test3";
    bkey = 40;
    colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                       val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
    ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = colfb.getOperationStatus().getResponse();

    check_response(response, CollectionResponse.CREATED_STORED, key);

    // prepare smget test
    key = "bop_smget_test1";
    attr.setMaxCount(new Long(2));
    attr.setOverflowAction(CollectionOverflowAction.smallest_trim);
    for (long i = 10; i < 50 ; i += 10) { // 10, 20, 30, 40
      bkey = i;
      colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                         val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "bop_smget_test failed, predicted STORED or CREATED_STORED";
    }

    key = "bop_smget_test2";
    for (long i = 100; i < 130 ; i += 10) { // 100, 110, 120
      bkey = i;
      colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                         val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "bop_smget_test failed, predicted STORED or CREATED_STORED";
    }

    // prepare position test
    attr.setMaxCount(cli.conf.ins_element_size);
    attr.setOverflowAction(CollectionOverflowAction.error);
    key = "bop_position_test1";
    for (long i = 10; i < 110 ; i += 10) { // 10, 20,....90, 100
      bkey = i;
      colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                         val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "bop_position_test failed, predicted STORED or CREATED_STORED";
    }

    // prepare pwg test
    key = "bop_pwg_test1";
    for (long i = 10; i < 110 ; i += 10) { // 10, 20,....90, 100
      bkey = i;
      colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                         val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "bop_pwg_test1 failed, predicted STORED or CREATED_STORED";
    }

    // prepare gbp test
    key = "bop_gbp_test";
    for (long i = 10; i < 110 ; i += 10) { // 10, 20,....90, 100
      bkey = i;
      colfb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                         val, attr);
    System.out.printf("bop insert operation request. key = " + key + ", bkey = " + bkey + ", val = " + val + "\n");
      ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      assert ok : "bop_gbp_test failed, predicted STORED or CREATED_STORED";
    }
  }

  public void check_response(CollectionResponse res, CollectionResponse confirm, String tname) {
    assert res.equals(confirm)
         : tname + " failed\n"
         + "predicted       : " + confirm.toString() + "\n"
         + "response string : " + res.toString() + "\n";
  }
}
