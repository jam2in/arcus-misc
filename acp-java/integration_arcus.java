/* -*- Mode: Java; tab-width: 2; c-basic-offset: 2; indent-tabs-mode: nil -*- */
/*
 * acp-java : Arcus Java Client Performance benchmark program
 * Copyright 2013-2014 NAVER Corp.
 * Copyright 2014-2016 JaM2in Co., Ltd.
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.Random;

import net.spy.memcached.collection.BTreeGetResult;
import net.spy.memcached.collection.ByteArrayBKey;
import net.spy.memcached.collection.BTreeOrder;
import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.Element;
import net.spy.memcached.collection.ElementFlagFilter;
import net.spy.memcached.collection.ElementFlagUpdate;
import net.spy.memcached.collection.ElementValueType;
import net.spy.memcached.collection.SMGetElement;
import net.spy.memcached.internal.CollectionFuture;
import net.spy.memcached.internal.CollectionGetBulkFuture;
import net.spy.memcached.internal.SMGetFuture;
import net.spy.memcached.ops.CollectionOperationStatus;

// Port of arcus1.6.2-integration.py

public class integration_arcus implements client_profile {

  public integration_arcus() {
    int next_val_idx = 0;
    chunk_values = new String[chunk_sizes.length+1];
    chunk_values[next_val_idx++] = "Not_a_slab_class";
    String lowercase = "abcdefghijlmnopqrstuvwxyz";
    for (int s : chunk_sizes) {
      int len = s*2/3;
      char[] raw = new char[len];
      for (int i = 0; i < len; i++) {
        raw[i] = lowercase.charAt(random.nextInt(lowercase.length()));
      }
      chunk_values[next_val_idx++] = new String(raw);
    }

    //Logger.getLogger("net.spy.memcached").setLevel(Level.DEBUG);
 }

  int KeyLen = 20;
  String DEFAULT_PREFIX = "arcustest-";
  char[] dummystring = 
    ("1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
     "abcdefghijlmnopqrstuvwxyz").toCharArray();
  Random random = new Random(); // repeatable is okay
  int[] chunk_sizes = {
    96, 120, 152, 192, 240, 304, 384, 480, 600, 752, 944, 1184, 1480, 1856,
    2320, 2904, 3632, 4544, 5680, 7104, 8880, 11104, 13880, 17352, 21696,
    27120, 33904, 42384, 52984, 66232, 82792, 103496, 129376, 161720, 202152,
    252696, 315872, 394840, 493552, 1048576
  };
  String[] chunk_values;

  String generateData(int length) {
    String ret = "";
    for (int loop = 0; loop < length; loop++) {
      int randomInt = random.nextInt(60);
      char tempchar = dummystring[randomInt];
      ret = ret + tempchar;
    }
    return ret;
  }

  // Generates a key with given name and postfix
  String gen_key(String name) {
    if (name == null)
      name = "unknown";
    String prefix = DEFAULT_PREFIX;
    String key = generateData(KeyLen);
    return prefix + name + ":" + key;
  }
  
  // Generates a string workload with specific size.
  String gen_workload(boolean is_collection) {
    if (is_collection) {
      // random.choice(chunk_values[0:17]);
      // Why 0 index?  chunk_values[0] is "Not_a_slab_class"?
      return chunk_values[random.nextInt(17+1)];
    }
    else {
      return chunk_values[random.nextInt(chunk_values.length)];
    }
  }

  private boolean delete_key(client cli, String key) throws Exception {
    if (!cli.before_request())
      return false;
    Future<Boolean> f = cli.next_ac.delete(key);
    boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    // DELETED || NOT_FOLUND
    if (!cli.after_request(ok))
      return false;
    return true;
  }

  public boolean do_test(client cli) {
    try {
      if (!do_KeyValue(cli))
        return false;
      
      if (!do_Collection_Btree(cli))
        return false;

      if (!do_Collection_Map(cli))
        return false;
         
      if (!do_Collection_Set(cli))
        return false;
      
      if (!do_Collection_List(cli))
        return false;
    } catch (Exception e) {
      System.out.printf("client_profile exception. id=%d exception=%s\n", 
                        cli.id, e.toString());
      if (cli.conf.print_stack_trace)
        e.printStackTrace();
    }
    return true;
  }

  // get:set:delete:incr:decr = 3:1:0.01:0.1:0.0001
  public boolean do_KeyValue(client cli) throws Exception {
    String key = gen_key("KeyValue");
    String workloads = chunk_values[24];
    Future<Boolean> fb;
    Future<byte[]> fbyte;
    byte[] val = cli.vset.get_value();;
    boolean ok;
    long not_used = 100L;
    
    if (!delete_key(cli, key)) return false;

    // Add
    if (!cli.before_request())
      return false;
    fb = cli.next_ac.add(key, cli.conf.client_exptime, val);
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (!ok) {
      System.out.printf("KeyValue: add failed. predicted STORED. id=%d key=%s\n"
                        , cli.id, key);
      System.exit(1);
    }
    fb = cli.next_ac.add(key, cli.conf.client_exptime, val);
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (ok) {
      System.out.printf("KeyValue: add failed. predicted NOT_STORED. id=%d key=%s\n"
                        , cli.id, key);
      System.exit(1);
    }
    if (!cli.after_request(true))
      return false;

    // Replace
    if (!cli.before_request())
        return false;
    fb = cli.next_ac.replace(key, cli.conf.client_exptime, "jam2in"); //replace value
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (!ok) {
      System.out.printf("KeyValue: replace failed. predicted STORED. id=%d key=%s\n"
                        , cli.id, key);
      System.exit(1);
    }
    if (!cli.after_request(true))
        return false;

    if (!cli.before_request())
        return false;
    fbyte = cli.next_ac.asyncGet(key, raw_transcoder.raw_tc); //confirm replace value
    val = fbyte.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);

    if (!"jam2in".equals(new String(val, "UTF-8"))) {
      System.out.printf("KeyValue: replace failed. predicted jam2in. id=%d key=%s\n"
                        , cli.id, key);
      System.exit(1);
    }
    if (!cli.after_request(true))
      return false;

    // Prepend
    key = gen_key("KeyValue");
    if (!delete_key(cli, key)) return false;
    if (!cli.before_request())
      return false;
    fb = cli.next_ac.set(key, cli.conf.client_exptime, "arcus"); //set prepend key
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (!ok) {
      System.out.printf("KeyValue: prepend failed. predicted STORED. id=%d key=%s\n"
                        , cli.id, key);
      System.exit(1);
    }
    if (!cli.after_request(true))
      return false;
    if (!cli.before_request())
      return false;
    fb = cli.next_ac.prepend(not_used, key, "jam2in"); //prepend value
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (!ok) {
      System.out.printf("KeyValue: jam2in prepend failed. predicted STORED. id=%d key=%s\n"
                        , cli.id, key);
      System.exit(1);
    }
    if (!cli.after_request(true))
      return false;
    if (!cli.before_request())
      return false;
    fbyte = cli.next_ac.asyncGet(key, raw_transcoder.raw_tc); //confirm prepend value
    val = fbyte.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (!"jam2inarcus".equals(new String(val, "UTF-8"))) {
      System.out.printf("KeyValue: jam2in prepend failed. predicted jam2inarcus. id=%d key=%s\n"
                        , cli.id, key);
      System.exit(1);
    }
    if (!cli.after_request(true))
      return false;

    // Append
    key = gen_key("KeyValue");
    if (!delete_key(cli, key)) return false;
    if (!cli.before_request())
      return false;
    fb = cli.next_ac.set(key, cli.conf.client_exptime, "arcus"); //set prepend key
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (!ok) {
      System.out.printf("KeyValue: append failed. predicted STORED. id=%d key=%s\n"
                        , cli.id, key);
      System.exit(1);
    }
    if (!cli.after_request(true))
      return false;
    if (!cli.before_request())
      return false;
    fb = cli.next_ac.append(not_used, key, "jam2in"); //append value
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (!ok) {
      System.out.printf("KeyValue: jam2in append failed. predicted STORED. id=%d key=%s\n"
                        , cli.id, key);
      System.exit(1);
    }
    if (!cli.after_request(true))
      return false;
    if (!cli.before_request())
      return false;
    fbyte = cli.next_ac.asyncGet(key, raw_transcoder.raw_tc); //confirm prepend value
    val = fbyte.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (!"arcusjam2in".equals(new String(val, "UTF-8"))) {
      System.out.printf("KeyValue: jam2in append failed. predicted arcusjam2in. id=%d key=%s\n"
                        , cli.id, key);
      System.exit(1);
    }
    if (!cli.after_request(true))
      return false;


    // Set 
    if (!cli.before_request())
      return false;
    fb = cli.next_ac.set(key, cli.conf.client_exptime, workloads);
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (!ok) {
      System.out.printf("KeyValue: set failed. id=%d key=%s\n", cli.id, key);
      System.exit(1);
    }
    if (!cli.after_request(ok))
      return false;

    // Get
    for (int i = 0; i < 5; i++) {
      if (!cli.before_request())
        return false;
      Future<Object> fs = cli.next_ac.asyncGet(key);
      String s = (String)fs.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      ok = true;
      if (s == null) {
        ok = false;
        System.out.printf("KeyValue: Get failed. id=%d key=%s\n", cli.id, key);
        System.exit(1);
      }
      if (!cli.after_request(ok))
        return false;
    }

    // Delete
    if (random.nextInt(3) == 0) {
      if (!cli.before_request())
        return false;
      fb = cli.next_ac.delete(key);
      ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("KeyValue: delete failed. id=%d key=%s\n", 
                          cli.id, key);
        System.exit(1);
      }
      if (!cli.after_request(ok))
        return false;
    }

    // Incr
    if (random.nextInt(1) == 0) {
      if (!cli.before_request())
        return false;
      fb = cli.next_ac.set(key + "numeric", cli.conf.client_exptime, "1");
      ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("KeyValue: set numeric failed. id=%d key=%s\n",
                          cli.id, key);
        System.exit(1);
      }
      if (!cli.after_request(ok))
        return false;
      if (!cli.before_request())
        return false;
      Future<Long> fl = cli.next_ac.asyncIncr(key + "numeric", 1);
      Long lv = fl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      // The returned value is the result of increment.
      ok = true;
      if (lv.longValue() != 2) {
        ok = false;
        System.out.println("KeyValue: Unexpected value from increment." +
                           " result=" +lv.longValue() +
                           " expected=" + 2);
        System.exit(1);
      }
      if (!cli.after_request(ok))
        return false;
    }
    
    // Decr
    if (random.nextInt(1) == 0) {
      if (!cli.before_request())
        return false;
      fb = cli.next_ac.set(key + "numeric", cli.conf.client_exptime, "1");
      ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("KeyValue: set numeric failed. id=%d key=%s\n",
                          cli.id, key);
        System.exit(1);
      }
      if (!cli.after_request(ok))
        return false;
      if (!cli.before_request())
        return false;
      Future<Long> fl = cli.next_ac.asyncDecr(key + "numeric", 1);
      Long lv = fl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      // The returned value is the result of decrement.
      ok = true;
      if (lv.longValue() != 0) {
        ok = false;
        System.out.println("KeyValue: Unexpected value from decrement." +
                           " result=" + lv.longValue());
        System.exit(1);
      }
      if (!cli.after_request(ok))
        return false;
    }

    // Set Bulk
    val = cli.vset.get_value();
    List<String> key_list = new LinkedList<String>();
    for (int i = 0; i < 50; i++) {
      key = gen_key("SetBulk");
      key_list.add(key);
    }
//    if (!cli.before_request())
//      return false;
//    Future<Map<String, CollectionOperationStatus>> fsb =
//        cli.next_ac.asyncSetBulk(key_list, cli.conf.client_exptime + 3000, val);
//    Map<String, CollectionOperationStatus> resultSetbulk =
//        fsb.get(cli.conf.client_timeout + 3000, TimeUnit.MILLISECONDS);
//    if (resultSetbulk == null) {
//      System.out.println("SetBulk: set bulk operation failed.");
//      System.exit(1);
//    }
//    if (!cli.after_request(ok))
//      return false;
//
//    // Get Bulk
//    if (!cli.before_request())
//      return false;
//    Future<Map<String, Object>> fgb =
//        cli.next_ac.asyncGetBulk(key_list);
//    Map<String, Object> resultGetbulk =
//        fgb.get(cli.conf.client_timeout + 3000, TimeUnit.MILLISECONDS);
//    // FIXME The keylist should be checked individually
//    if (resultGetbulk == null || resultGetbulk.size() <= 0) {
//      System.out.println("GetBulk: get bulk operation failed.");
//      System.exit(1);
//    }
//    if (!cli.after_request(ok))
//      return false;


    return true;
  }
  
  public boolean do_Collection_Btree(client cli) throws Exception {
    String key = gen_key("Collection_Btree");
    List<String> key_list = new LinkedList<String>();
    if (!delete_key(cli, key)) return false;
    for (int i = 0; i < 4; i++)
      key_list.add(key + i);
    
    String bkeyBASE = "bkey_byteArry";

    byte[] eflag = ("EFLAG").getBytes();
    ElementFlagFilter filter = 
      new ElementFlagFilter(ElementFlagFilter.CompOperands.Equal,
                            ("EFLAG").getBytes());
    CollectionAttributes attr = new CollectionAttributes(cli.conf.client_exptime,
                                                         new Long(cli.conf.ins_element_size),
                                                         CollectionOverflowAction.smallest_trim);

    String[] workloads = { chunk_values[1], 
                           chunk_values[1], 
                           chunk_values[2], 
                           chunk_values[2], 
                           chunk_values[3] };

    // BopInsert + byte_array bkey
    for (int j = 0; j < 4; j++) {
      // Insert 50 bkey
      for (int i = 0; i < 50; i++) {
        if (!cli.before_request())
          return false;
        // Uniq bkey
        String bk = bkeyBASE + Integer.toString(j) + Integer.toString(i);
        byte[] bkey = bk.getBytes();
        CollectionFuture<Boolean> f = cli.next_ac.
          asyncBopInsert(key_list.get(j), bkey, eflag, 
                         workloads[random.nextInt(workloads.length)], attr);
        boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
        if (!ok) {
          System.out.printf("Collection_Btree: BopInsert failed." +
                            " id=%d key=%s bkey=%s: %s\n", cli.id,
                            key_list.get(j), bk,
                            f.getOperationStatus().getResponse());
          System.exit(1);
        }
        if (!cli.after_request(ok))
          return false;
      }
    }
    
    // Bop Bulk Insert (Piped Insert)
    {
      List<Element<Object>> elements = new LinkedList<Element<Object>>();
      for (int i = 0; i < 50; i++) {
        String bk = bkeyBASE + "0" + Integer.toString(i) + "bulk";
        elements.add(new Element<Object>(bk.getBytes(), workloads[0], eflag));
      }
      if (!cli.before_request())
        return false;
      CollectionFuture<Map<Integer, CollectionOperationStatus>> f =
        cli.next_ac.asyncBopPipedInsertBulk(key_list.get(0), elements,
                                            attr);
      Map<Integer, CollectionOperationStatus> status_map = 
        f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      Iterator<CollectionOperationStatus> status_iter = 
        status_map.values().iterator();
      while (status_iter.hasNext()) {
        CollectionOperationStatus status = status_iter.next();
        CollectionResponse resp = status.getResponse();
        if (resp != CollectionResponse.STORED) {
          System.out.printf("Collection_Btree: BopPipedInsertBulk failed." +
                            " id=%d key=%s response=%s\n", cli.id,
                            key_list.get(0), resp);
          System.exit(1);
        }
      }
      if (!cli.after_request(true))
        return false;
    }

    // BopGet Range + filter
    for (int j = 0; j < 4; j++) {
      if (!cli.before_request())
        return false;
      String bk = bkeyBASE + Integer.toString(j) + Integer.toString(0);
      String bk_to = bkeyBASE + Integer.toString(j) + Integer.toString(50);
      byte[] bkey = bk.getBytes();
      byte[] bkey_to = bk_to.getBytes();
      CollectionFuture<Map<ByteArrayBKey, Element<Object>>> f =
        cli.next_ac.asyncBopGet(key_list.get(j), bkey, bkey_to, filter,
                                0, random.nextInt(30) + 20,
                                /* random.randint(20, 50) */
                                false, false);
      Map<ByteArrayBKey, Element<Object>> val = 
        f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (val == null || val.size() <= 0) {
        System.out.printf("Collection_Btree: BopGet failed." +
                          " id=%d key=%s val.size=%d\n", cli.id,
                          key_list.get(j), val == null ? -1 : 0);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;
    }

    // BopGetBulk  // 20120319 Ad
    {
      if (!cli.before_request())
        return false;
      String bk = bkeyBASE + "0" + "0";
      String bk_to = bkeyBASE + "4" + "50";
      byte[] bkey = bk.getBytes();
      byte[] bkey_to = bk_to.getBytes();
      CollectionGetBulkFuture
        <Map<String, BTreeGetResult<ByteArrayBKey, Object>>> f =
        cli.next_ac.asyncBopGetBulk(key_list, bkey, bkey_to, filter, 0,
                                    random.nextInt(30) + 20
                                    /* random.randint(20, 50) */);
      Map<String, BTreeGetResult<ByteArrayBKey, Object>> val = 
        f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (val == null || val.size() <= 0) {
        System.out.printf("Collection_Btree: BopGetBulk failed." +
                          " id=%d val.size=%d\n", cli.id,
                          val == null ? -1 : 0);
        System.exit(1);
      }
      else {
        // Should we check individual elements?  FIXME
      }
      if (!cli.after_request(true))
        return false;
    }
    
    // BopEmpty Create
    {
      if (!cli.before_request())
        return false;
      CollectionFuture<Boolean> f = 
        cli.next_ac.asyncBopCreate(key, ElementValueType.STRING, 
                                   attr);
      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("Collection_Btree: BopCreate failed." +
                          " id=%d key=%s: %s\n", cli.id, key,
                          f.getOperationStatus().getResponse());
        System.exit(1);
      }
      if (!cli.after_request(ok))
        return false;
    }

    // BopSMGet
    {
      if (!cli.before_request())
        return false;
      String bk = bkeyBASE + "0" + "0";
      String bk_to = bkeyBASE + "4" + "50";
      byte[] bkey = bk.getBytes();
      byte[] bkey_to = bk_to.getBytes();
      SMGetFuture<List<SMGetElement<Object>>> f =
        cli.next_ac.asyncBopSortMergeGet(key_list, bkey, bkey_to, 
                                         filter, 0, 
                                         random.nextInt(30) + 20
                                         /* random.randint(20, 50) */);
      List<SMGetElement<Object>> val = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (val == null || val.size() <= 0) {
        System.out.printf("Collection_Btree: BopSortMergeGet failed." +
                          " id=%d val.size=%d\n", cli.id,
                          val == null ? -1 : 0);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;
    }
    
    // BopUpdate  (eflag bitOP + value)
    {
      String key0 = key_list.get(0);
      int eflagOffset = 0;
      String value = "ThisIsChangeValue";
      ElementFlagUpdate bitop = 
        new ElementFlagUpdate(eflagOffset, 
                              ElementFlagFilter.BitWiseOperands.AND,
                              ("aflag").getBytes());
      for (int i = 0; i < 2; i++) {
        if (!cli.before_request())
          return false;
        String bk = bkeyBASE + "0" + Integer.toString(i);
        byte[] bkey = bk.getBytes();
        CollectionFuture<Boolean> f = 
          cli.next_ac.asyncBopUpdate(key0, bkey, bitop, value);
        boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
        if (!ok) {
          System.out.printf("Collection_Btree: BopUpdate failed." +
                            " id=%d key=%s: %s\n", cli.id, key0,
                            f.getOperationStatus().getResponse());
          System.exit(1);
        }
        if (!cli.after_request(ok))
          return false;
      }
    }
    
    // SetAttr  (change Expire Time)
//    {
//      if (!cli.before_request())
//        return false;
//      attrExpireTime.client_exptime(100);
//      CollectionFuture<Boolean> f = cli.next_ac.asyncSetAttr(key, attr);
//      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
//      if (!ok) {
//        System.out.printf("Collection_Btree: SetAttr failed." +
//                          " id=%d key=%s: %s\n", cli.id, key,
//                          f.getOperationStatus().getResponse());
//      }
//      if (!cli.after_request(ok))
//        return false;
//    }

    // BopFindPosition
    {
      String key0 = key_list.get(0);
      String bk = bkeyBASE + "0" + Integer.toString(20);
      byte[] bkey = bk.getBytes();
      if (!cli.before_request())
        return false;
      CollectionFuture<Integer> colfi = cli.next_ac.asyncBopFindPosition(key0, bkey, BTreeOrder.ASC);
      int position = colfi.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (position != 25) {
        System.out.printf("Collection_Btree: BopFindPosition failed." +
                          " position=%d id=%d key=%s: %s\n",position, cli.id, key0);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;
    }

    // gbp
    {
      String key1 = key_list.get(1);
      String bk = bkeyBASE + "1" + Integer.toString(30);
      CollectionFuture<Map<Integer, Element<Object>>> colfgbp;
      Map<Integer, Element<Object>> resultgbp;
      CollectionResponse response;
      if (!cli.before_request())
        return false;
      colfgbp = cli.next_ac.asyncBopGetByPosition(key1, BTreeOrder.ASC, 5);
      resultgbp = colfgbp.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      response = colfgbp.getOperationStatus().getResponse();
      if (!response.equals(CollectionResponse.END)) {
        System.out.printf("Collection_Btree: BopGetByPosition failed." +
                          " id=%d key=%s: %s\n", cli.id, key, response);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;
    }

    // pwg
    {
      String key2 = key_list.get(2);
      String bk = bkeyBASE + "2" + Integer.toString(30);
      byte[] bkey = bk.getBytes();
      CollectionFuture<Map<Integer, Element<Object>>> colfpwg;
      Map<Integer,Element<Object>> resultpwg;
      CollectionResponse response;
      if (!cli.before_request())
        return false;
      colfpwg = cli.next_ac.asyncBopFindPositionWithGet(key2, bkey, BTreeOrder.ASC, 10/* pwgCount */);
      resultpwg = colfpwg.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      response = colfpwg.getOperationStatus().getResponse();
      if (!response.equals(CollectionResponse.END)) {
        System.out.printf("Collection_Btree: BopFindPositionWithGet failed." +
                          " id=%d key=%s: %s\n", cli.id, key, response);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;
    }

    // BopUpsert, Bop arithmetic, BopGetItemCount
    {
      key = gen_key("Collection_Btree");
      if (!delete_key(cli, key)) return false;
      String bk = bkeyBASE + "0" + Integer.toString(7);
      int bkey = 30;
      if (!cli.before_request())
        return false;
      CollectionFuture<Boolean> fb = cli.next_ac.asyncBopInsert(key, bkey, null /* eflag(optional)*/,
                                     3000, attr);
      boolean ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("Collection_Btree: BopUpsert insertdata failed." +
                           "id=%d key=%s\n", cli.id, key);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;

      if (!cli.before_request())
        return false;
      String str = String.format("%d", 1000);
      byte[] val = str.getBytes();
      fb = cli.next_ac.asyncBopUpsert(key, bkey, null, val, attr);
      ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("Collection_Btree: BopUpsert failed." +
                           "id=%d key=%s\n", cli.id, key);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;

      // Bop arithmetic(bop incr | bop decr)
      if (!cli.before_request())
        return false;
      CollectionFuture<Long> colfl =
          cli.next_ac.asyncBopIncr(key, bkey, (int)6777);
      Long arithval = colfl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (arithval != 7777) {
        System.out.printf("Collection_Btree: BopIncr failed." +
                           "id=%d key=%s val=%ld\n", cli.id, key, arithval);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;

      if (!cli.before_request())
        return false;
      colfl = cli.next_ac.asyncBopDecr(key, bkey, (int)5555);
      arithval = colfl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (arithval != 2222) {
        System.out.printf("Collection_Btree: BopDecr failed." +
                           "id=%d key=%s val=%ld\n", cli.id, key, arithval);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;

      if (!cli.before_request())
        return false;
      CollectionFuture<Integer> colfi =
          cli.next_ac.asyncBopGetItemCount(key, 30, 100, ElementFlagFilter.DO_NOT_FILTER);;
      int count = colfi.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (count != 1) {
        System.out.printf("Collection_Btree: BopGetItemCount failed." +
                           "id=%d key=%s count=%d\n", cli.id, key, count);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;
    }


    // BopDelete          (eflag filter delete)
    {
      for (int j = 0; j < 4; j++) {
        if (!cli.before_request())
          return false;
        String bk = bkeyBASE + Integer.toString(j) + "0";
        String bk_to = bkeyBASE + Integer.toString(j) + "10";
        byte[] bkey = bk.getBytes();
        byte[] bkey_to = bk_to.getBytes();
        CollectionFuture<Boolean> f = 
          cli.next_ac.asyncBopDelete(key_list.get(j), bkey, bkey_to, filter,
                                     0, false);
        boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
        if (!ok) {
          System.out.printf("Collection_Btree: BopDelete failed." +
                            " id=%d key=%s: %s\n", cli.id, key_list.get(j),
                            f.getOperationStatus().getResponse());
          System.exit(1);
        }
        if (!cli.after_request(ok))
          return false;
      }
    }

    return true;
  }

  public boolean do_Collection_Map(client cli) throws Exception {
    String key = gen_key("Collection_Map");
    List<String> key_list = new LinkedList<String>();
    if (!delete_key(cli, key)) return false;
    for (int i = 0; i < 4; i++)
      key_list.add(key + i);

    String mkeyBASE = "mkey";

    CollectionAttributes attr = new CollectionAttributes(cli.conf.client_exptime,
                                                         new Long(cli.conf.ins_element_size),
                                                         CollectionOverflowAction.error);

    String[] workloads = { chunk_values[1],
            chunk_values[1],
            chunk_values[2],
            chunk_values[2],
            chunk_values[3] };

    // MopInsert
    for (int j = 0; j < 4; j++) {
      // Insert 50 mkey
      for (int i = 0; i < 50; i++) {
        if (!cli.before_request())
          return false;
        // Uniq mkey
        String mkey = mkeyBASE + Integer.toString(j) + Integer.toString(i);
        CollectionFuture<Boolean> f = cli.next_ac.
                asyncMopInsert(key_list.get(j), mkey,
                        workloads[random.nextInt(workloads.length)], attr);
        boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
        if (!ok) {
          System.out.printf("Collection_Map: MopInsert failed." +
                          " id=%d key=%s mkey=%s: %s\n", cli.id,
                  key_list.get(j), mkey,
                  f.getOperationStatus().getResponse());
          System.exit(1);
        }
        if (!cli.after_request(ok))
          return false;
      }
    }

    // MopInsert Bulk (Piped)
    {
      Map<String, Object> elements = new HashMap<String, Object>();
      for (int i = 0; i < 50; i++) {
        String mkey = mkeyBASE + Integer.toString(i) + "bulk";
        elements.put(mkey, workloads[0]);
      }
      if (!cli.before_request())
        return false;
      CollectionFuture<Map<Integer, CollectionOperationStatus>> f =
              cli.next_ac.asyncMopPipedInsertBulk(key_list.get(0), elements,
                      attr);
      Map<Integer, CollectionOperationStatus> status_map =
              f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      Iterator<CollectionOperationStatus> status_iter =
              status_map.values().iterator();
      while (status_iter.hasNext()) {
        CollectionOperationStatus status = status_iter.next();
        CollectionResponse resp = status.getResponse();
        if (resp != CollectionResponse.STORED) {
          System.out.printf("Collection_Map: MopPipedInsertBulk failed." +
                          " id=%d key=%s response=%s\n", cli.id,
                  key_list.get(0), resp);
          System.exit(1);
        }
      }
      if (!cli.after_request(true))
        return false;
    }

    // MopGet all
    {
      if (!cli.before_request())
        return false;
      CollectionFuture<Map<String, Object>> f =
              cli.next_ac.asyncMopGet(key_list.get(0), false, false);
      Map<String, Object> val =
              f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (val == null || val.size() != 100) {
        System.out.printf("Collection_Map: MopGet all failed." +
                        " id=%d key=%s val.size=%d\n", cli.id,
                key_list.get(0), val == null ? -1 : 0);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;
    }

    // MopEmpty Create
    {
      if (!cli.before_request())
        return false;
      CollectionFuture<Boolean> f =
              cli.next_ac.asyncMopCreate(key, ElementValueType.STRING,
                      attr);
      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("Collection_Map: MopCreate failed." +
                        " id=%d key=%s: %s\n", cli.id, key,
                f.getOperationStatus().getResponse());
        System.exit(1);
      }
      if (!cli.after_request(ok))
        return false;
    }

    // MopUpdate
    {
      String key0 = key_list.get(0);
      String value = "ThisIsChangeValue";
      for (int i = 0; i < 2; i++) {
        if (!cli.before_request())
          return false;
        String mkey = mkeyBASE + "0" + Integer.toString(i);
        CollectionFuture<Boolean> f =
                cli.next_ac.asyncMopUpdate(key0, mkey, value);
        boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
        if (!ok) {
          System.out.printf("Collection_Map: MopUpdate failed." +
                          " id=%d key=%s: %s\n", cli.id, key0,
                  f.getOperationStatus().getResponse());
          System.exit(1);
        }
        if (!cli.after_request(ok))
          return false;
      }
    }

    // SetAttr  (change Expire Time)
//    {
//      if (!cli.before_request())
//        return false;
//      attr.setExpireTime(100);
//      CollectionFuture<Boolean> f = cli.next_ac.asyncSetAttr(key, attr);
//      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
//      if (!ok) {
//        System.out.printf("Collection_Map: SetAttr failed." +
//                        " id=%d key=%s: %s\n", cli.id, key,
//                f.getOperationStatus().getResponse());
//      }
//      if (!cli.after_request(ok))
//        return false;
//    }

    // MopDelete
    for (int j = 0; j < 4; j++) {
      // Delete 50 mkey
      for (int i = 0; i < 50; i++) {
        if (!cli.before_request())
          return false;
        // Uniq mkey
        String mkey = mkeyBASE + Integer.toString(j) + Integer.toString(i);
        CollectionFuture<Boolean> f = cli.next_ac.
                asyncMopDelete(key_list.get(j), mkey, true);
        boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
        if (!ok) {
          System.out.printf("Collection_Map: MopDelete failed." +
                          " id=%d key=%s mkey=%s: %s\n", cli.id,
                  key_list.get(j), mkey,
                  f.getOperationStatus().getResponse());
          System.exit(1);
        }
        if (!cli.after_request(ok))
          return false;
      }
    }

    return true;
  }

  public boolean do_Collection_Set(client cli) throws Exception {
    String key = gen_key("Collection_Set");
    List<String> key_list = new LinkedList<String>();
    if (!delete_key(cli, key)) return false;
    for (int i = 0; i < 4; i++)
      key_list.add(key + i);

    CollectionAttributes attr = new CollectionAttributes(cli.conf.client_exptime,
                                                         new Long(cli.conf.ins_element_size),
                                                         CollectionOverflowAction.error);

    String[] workloads = { chunk_values[1], 
                           chunk_values[1], 
                           chunk_values[2], 
                           chunk_values[2], 
                           chunk_values[3] };

    // SopInsert
    {
      for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 19; j++) {
          if (!cli.before_request())
            return false;
          String set_value = workloads[i] + Integer.toString(j);
          CollectionFuture<Boolean> f = 
            cli.next_ac.asyncSopInsert(key_list.get(i), set_value, attr);
          boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
          if (!ok) {
            System.out.printf("Collection_Set: SopInsert failed." +
                              " id=%d key=%s: %s\n", cli.id, key_list.get(i),
                              f.getOperationStatus().getResponse());
            System.exit(1);
          }
          if (!cli.after_request(ok))
            return false;
        }
      }
    }

    // SopGet
    {
      if (!cli.before_request())
        return false;
      CollectionFuture<Set<Object>> colfs =
          cli.next_ac.asyncSopGet(key_list.get(0), cli.conf.act_element_size
                                , false, false);;
      Set<Object> setval = colfs.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      CollectionResponse response = colfs.getOperationStatus().getResponse();
      if (!response.equals(CollectionResponse.END) || setval.size() <= 0) {
        System.out.printf("Collection_Set: SopGet failed." +
                          " id=%d key=%s response=%s\n", cli.id,
                          key_list.get(0), response);
        System.exit(1);
      }
      if (!cli.after_request(true))
        return false;
    }
    
    // SopInsert Bulk (Piped)
    {
      List<Object> elements = new LinkedList<Object>();
      for (int i = 0; i < 50; i++) {
        elements.add((Integer.toString(i) + "_" + workloads[0]));
      }
      if (!cli.before_request())
        return false;
      CollectionFuture<Map<Integer, CollectionOperationStatus>> f =
        cli.next_ac.asyncSopPipedInsertBulk(key_list.get(0), elements, 
                                            attr);
      Map<Integer, CollectionOperationStatus> status_map = 
        f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      Iterator<CollectionOperationStatus> status_iter = 
        status_map.values().iterator();
      while (status_iter.hasNext()) {
        CollectionOperationStatus status = status_iter.next();
        CollectionResponse resp = status.getResponse();
        if (resp != CollectionResponse.STORED) {
          System.out.printf("Collection_Set: SopPipedInsertBulk failed." +
                            " id=%d key=%s response=%s\n", cli.id,
                            key_list.get(0), resp);
          System.exit(1);
        }
      }
      if (!cli.after_request(true))
        return false;
    }
    
    // SopEmpty Create
    {
      if (!cli.before_request())
        return false;
      CollectionFuture<Boolean> f = 
        cli.next_ac.asyncSopCreate(key, ElementValueType.STRING, 
                                   attr);
      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("Collection_Set: SopCreate failed." +
                          " id=%d key=%s: %s\n", cli.id, key,
                          f.getOperationStatus().getResponse());
        System.exit(1);
      }
      if (!cli.after_request(ok))
        return false;
    }
    
    // SopExist (Piped exist)
    {
      for (int i = 0; i < 4; i++) {
        List<Object> list_value = new LinkedList<Object>();
        for (int j = 0; j < 9; j++) {
          if (!cli.before_request())
            return false;
          list_value.add(workloads[i] + Integer.toString(j));
          CollectionFuture<Map<Object, Boolean>> f = 
            cli.next_ac.asyncSopPipedExistBulk(key_list.get(i), list_value);
          Map<Object, Boolean> result_map =
            f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
          if (result_map == null || result_map.size() != list_value.size()) {
            System.out.printf("Collection_Set: SopPipedExistBulk failed." +
                              " id=%d key=%s result_map.size=%d" +
                              " list_value.size=%d\n",
                              cli.id, key_list.get(i), 
                              result_map == null ? -1 : result_map.size(),
                              list_value.size());
            System.exit(1);
          }
          if (!cli.after_request(true))
            return false;
        }
      }
    }
    
    // SetAttr  (change Expire Time)
//    {
//      if (!cli.before_request())
//        return false;
//      attr.setExpireTime(100);
//      CollectionFuture<Boolean> f = cli.next_ac.asyncSetAttr(key, attr);
//      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
//      if (!ok) {
//        System.out.printf("Collection_Set: SetAttr failed." +
//                          " id=%d key=%s: %s\n", cli.id, key,
//                          f.getOperationStatus().getResponse());
//      }
//      if (!cli.after_request(ok))
//        return false;
//    }
    
    // SopDelete
    {
      for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
          if (!cli.before_request())
            return false;
          String del_value = workloads[i] + Integer.toString(j);
          CollectionFuture<Boolean> f = 
            cli.next_ac.asyncSopDelete(key_list.get(i), del_value, true);
          boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
          if (!ok) {
            System.out.printf("Collection_Set: SopDelete failed." +
                              " id=%d key=%s: %s\n", cli.id, key_list.get(i),
                              f.getOperationStatus().getResponse());
            System.exit(1);
          }
          if (!cli.after_request(ok))
            return false;
        }
      }
    }

    return true;
  }

  public boolean do_Collection_List(client cli) throws Exception {
    String key = gen_key("Collection_List");
    List<String> key_list = new LinkedList<String>();
    if (!delete_key(cli, key)) return false;
    for (int i = 0; i < 4; i++)
      key_list.add(key + i);

    CollectionAttributes attr = new CollectionAttributes(cli.conf.client_exptime,
                                                         new Long(50000),
                                                         CollectionOverflowAction.head_trim);

    String[] workloads = { chunk_values[1], 
                           chunk_values[1], 
                           chunk_values[2], 
                           chunk_values[2], 
                           chunk_values[3] };

    // LopInsert
    {
      int index = -1; // tail insert
      for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 50; j++) {
          if (!cli.before_request())
            return false;
          CollectionFuture<Boolean> f = cli.next_ac
            .asyncLopInsert(key_list.get(i), index, 
                            workloads[random.nextInt(workloads.length)], attr);
          boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
          if (!ok) {
            System.out.printf("Collection_List: LopInsert failed." +
                              " id=%d key=%s: %s\n", cli.id, key_list.get(i),
                              f.getOperationStatus().getResponse());
            System.exit(1);
          }
          if (!cli.after_request(ok))
            return false;
        }
      }
    }

    // LopInsert Bulk (Piped)
    {
      List<Object> elements = new LinkedList<Object>();
      for (int i = 0; i < 50; i++) {
        elements.add(Integer.toString(i) + "_" + workloads[0]);
      }
      if (!cli.before_request())
        return false;
      CollectionFuture<Map<Integer, CollectionOperationStatus>> f =
        cli.next_ac.asyncLopPipedInsertBulk(key_list.get(0), -1, elements, 
                                            attr);
      Map<Integer, CollectionOperationStatus> status_map = 
        f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      Iterator<CollectionOperationStatus> status_iter = 
        status_map.values().iterator();
      while (status_iter.hasNext()) {
        CollectionOperationStatus status = status_iter.next();
        CollectionResponse resp = status.getResponse();
        if (resp != CollectionResponse.STORED) {
          System.out.printf("Collection_List: LopPipedInsertBulk failed." +
                            " id=%d key=%s response=%s\n", cli.id,
                            key_list.get(0), resp);
          System.exit(1);
        }
      }
      if (!cli.after_request(true))
        return false;
    }

    // LopGet
    {
      for (int i = 0; i < 4; i++) {
        if (!cli.before_request())
          return false;
        int index = 0;
        int index_to = index + 
          /* random.randint(20, 50) */ random.nextInt(30) + 20;
        CollectionFuture<List<Object>> f =
          cli.next_ac.asyncLopGet(key_list.get(i), index, index_to, 
                                  false, false);
        List<Object> val = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
        if (val == null || val.size() <= 0) {
          System.out.printf("Collection_List: LopGet failed." +
                            " id=%d key=%s val.size=%d\n",
                            cli.id, key_list.get(i), 
                            val == null ? -1 : val.size());
          System.exit(1);
        }
        if (!cli.after_request(true))
          return false;
      }
    }

    // LopAttr
//    {
//      if (!cli.before_request())
//        return false;
//      attr.setExpireTime(100);
//      CollectionFuture<Boolean> f =
//        cli.next_ac.asyncSetAttr(key_list.get(0), attr);
//      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
//      if (!ok) {
//        System.out.printf("Collection_List: SetAttr failed." +
//                          " id=%d key=%s: %s\n", cli.id, key_list.get(0),
//                          f.getOperationStatus().getResponse());
//      }
//      if (!cli.after_request(ok))
//        return false;
//    }

    // LopDelete
    {
      int index = 0;
      int index_to = 19;
      for (int i = 0; i < 1; i++) {
        if (!cli.before_request())
          return false;
        CollectionFuture<Boolean> f = 
          cli.next_ac.asyncLopDelete(key_list.get(i), index, index_to, true);
        boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
        if (!ok) {
          System.out.printf("Collection_List: LopDelete failed." +
                            " id=%d key=%s: %s\n", cli.id, key_list.get(i),
                            f.getOperationStatus().getResponse());
          System.exit(1);
        }
        if (!cli.after_request(ok))
          return false;
      }
    }

    // LopCreate
    {
      key = gen_key("Collection_List");
      CollectionResponse response;
      if (!delete_key(cli, key)) return false;
      if (!cli.before_request())
        return false;
      CollectionFuture<Boolean> colfb =
        cli.next_ac.asyncLopCreate(key, ElementValueType.BYTEARRAY, attr);
      boolean ok = colfb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      response = colfb.getOperationStatus().getResponse();
      if (!response.equals(CollectionResponse.CREATED)) {
        System.out.printf("Collection_List: LopCreate failed." +
                          " id=%d key=%s: %s\n", cli.id, key, response);
        System.exit(1);
      }
      if (!cli.after_request(ok))
        return false;
    }
    return true;
  }
}
