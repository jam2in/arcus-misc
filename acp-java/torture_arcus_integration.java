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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;

import net.spy.memcached.collection.BTreeGetResult;
import net.spy.memcached.collection.ByteArrayBKey;
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
import net.spy.memcached.ArcusClientPool;

// Port of arcus1.6.2-integration.py

public class torture_arcus_integration implements client_profile {

  private static final ReentrantLock lock = new ReentrantLock(true);
  private static final AtomicBoolean flushed = new AtomicBoolean(false);

  private static final long _24hours = 24 * 60 * 60 * 1000;
  private static final AtomicLong lastTouchFailed = new AtomicLong(0);

  public torture_arcus_integration() {
    int next_val_idx = 0;
    chunk_values = new String[chunk_sizes.length+1];
    chunk_values[next_val_idx++] = "Not_a_slab_class";
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

  String lowercase = "abcdefghijlmnopqrstuvwxyz";
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

/* Not used API
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
*/

  public boolean do_test(client cli) {
    try {
      if (!do_Flush(cli))
        return false;

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

      if (lastTouchFailed.get() + _24hours >= System.currentTimeMillis()) {
        return true;
      }

      lock.lock();
      try {
        if (lastTouchFailed.get() + _24hours >= System.currentTimeMillis()) {
          return true;
        }

        if (!do_Touch(cli))
          return false;
      } catch (ExecutionException e) {
        if (cli.conf.print_stack_trace) {
          e.printStackTrace();
        }
        lastTouchFailed.set(System.currentTimeMillis());
      } finally {
        lock.unlock();
      }
    } catch (Exception e) {
      System.out.printf("client_profile exception. id=%d exception=%s\n",
                        cli.id, e.toString());
      if (cli.conf.print_stack_trace)
        e.printStackTrace();
    }
    return true;
  }

  private Method getTouchMethod() {
    try {
      return ArcusClientPool.class.getMethod("touch", String.class, int.class);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private boolean hasTouchMethod() {
    return getTouchMethod() != null;
  }

  @SuppressWarnings("unchecked")
  private Future<Boolean> touch(client cli, String key, int exptime) {
    Method touchMethod = getTouchMethod();
    if (touchMethod == null) {
      return null;
    }

    try {
      return (Future<Boolean>) touchMethod.invoke(cli.next_ac, key, exptime);
    } catch (InvocationTargetException | IllegalAccessException e) {
      return null;
    }
  }

  public boolean do_Touch(client cli) throws Exception {
    if (!hasTouchMethod()) {
      return true;
    }

    // Pick a key and a value
    String key = cli.ks.get_key() + "-for-touch";
    byte[] val = cli.vset.get_value();

    // Exptime
    int set_exptime = cli.conf.client_exptime / 2;
    int touch_exptime = cli.conf.client_exptime;

    if (!cli.before_request()) {
      return false;
    }

    Future<Boolean> fb = cli.next_ac.set(key, set_exptime, val, raw_transcoder.raw_tc);
    if (!fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS)) {
      System.out.printf("set before touch failed. id=%d key=%s\n", cli.id, key);
      return cli.after_request(false);
    }

    fb = touch(cli, key, touch_exptime);
    if (!fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS)) {
      System.out.printf("touch failed. id=%d key=%s\n", cli.id, key);
      return cli.after_request(false);
    }

    CollectionAttributes attr = cli.next_ac.asyncGetAttr(key).get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    if (attr == null || attr.getExpireTime() == null) {
      System.out.printf("getAttrs after touch failed. id=%d key=%s\n", cli.id, key);
      return cli.after_request(false);
    }
    if (attr.getExpireTime() <= set_exptime) {
      System.out.printf("getAttrs after touch failed for exptime. touched %d but got %d. id=%d key=%s\n",
                        touch_exptime, set_exptime, cli.id, key);
      return cli.after_request(false);
    }

    return cli.after_request(true);
  }

  public boolean do_Flush(client cli) throws Exception {
    if (flushed.get()) {
      return true;
    }

    lock.lock();
    try {
      if (flushed.get()) {
        return true;
      }

      if (!cli.before_request()) {
        return false;
      }

      String prefix = cli.conf.key_prefix;
      if (prefix.endsWith(":")) {
        prefix = prefix.substring(0, prefix.length() - 1);
      }

      boolean ok = cli.next_ac.flush(prefix).get();
      flushed.set(ok);

      return cli.after_request(ok);
    } finally {
      lock.unlock();
    }
  }

  // get:set:delete:incr:decr = 3:1:0.01:0.1:0.0001
  public boolean do_KeyValue(client cli) throws Exception {
    String key = cli.ks.get_key_by_cliid(cli);
    String[] workloads = { chunk_values[4],
                           chunk_values[5],
                           chunk_values[6],
                           chunk_values[7],
                           chunk_values[8] };

    // Set
    for (int i = 0; i < 1; i++) {
      if (!cli.before_request())
        return false;
      Future<Boolean> fb = cli.next_ac.set(key, cli.conf.client_exptime, workloads[random.nextInt(workloads.length)]);
      boolean ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("KeyValue: set failed. id=%d key=%s\n", cli.id, key);
      }
      if (!cli.after_request(ok))
        return false;
    }

    // Get
    for (int i = 0; i < 5; i++) {
      if (!cli.before_request())
        return false;
      Future<Object> fs = cli.next_ac.asyncGet(key);
      String s = (String)fs.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      boolean ok = true;
      if (s == null) {
        ok = false;
        System.out.printf("KeyValue: Get failed. id=%d key=%s\n", cli.id, key);
      }
      if (!cli.after_request(ok))
        return false;
    }

    // Delete
    if (random.nextInt(3) == 0) {
      if (!cli.before_request())
        return false;
      Future<Boolean> f = cli.next_ac.delete(key);
      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("KeyValue: delete failed. id=%d key=%s\n",
                          cli.id, key);
      }
      if (!cli.after_request(ok))
        return false;
    }

    // Incr
    if (random.nextInt(1) == 0) {
      if (!cli.before_request())
        return false;
      Future<Boolean> fb = cli.next_ac.set(key + "numeric", cli.conf.client_exptime, "1");
      boolean ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("KeyValue: set numeric failed. id=%d key=%s\n",
                          cli.id, key);
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
      }
      if (!cli.after_request(ok))
        return false;
    }

    // Decr
    if (random.nextInt(1) == 0) {
      if (!cli.before_request())
        return false;
      Future<Boolean> fb = cli.next_ac.set(key + "numeric", cli.conf.client_exptime, "1");
      boolean ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("KeyValue: set numeric failed. id=%d key=%s\n",
                          cli.id, key);
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
      }
      if (!cli.after_request(ok))
        return false;
    }

    return true;
  }

  public boolean do_Collection_Btree(client cli) throws Exception {
    String key = cli.ks.get_key_by_cliid(cli);
    List<String> key_list = new LinkedList<String>();
    for (int i = 0; i < 1; i++)
      key_list.add(key + lowercase.charAt(i));

    String bkeyBASE = "bkey_byteArry";

    byte[] eflag = ("EFLAG").getBytes();
    ElementFlagFilter filter =
      new ElementFlagFilter(ElementFlagFilter.CompOperands.Equal,
                            ("EFLAG").getBytes());
    CollectionAttributes attr = new CollectionAttributes();
    attr.setExpireTime(cli.conf.client_exptime);

    String[] workloads = { chunk_values[4],
                           chunk_values[5],
                           chunk_values[6],
                           chunk_values[7],
                           chunk_values[8] };

    // BopInsert + byte_array bkey
    for (int j = 0; j < 1; j++) {
      // Insert 10 bkey
      for (int i = 0; i < 10; i++) {
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
        }
        if (!cli.after_request(ok))
          return false;
      }
    }

    // Bop Bulk Insert (Piped Insert)
    {
      List<Element<Object>> elements = new LinkedList<Element<Object>>();
      for (int i = 0; i < 10; i++) {
        String bk = bkeyBASE + "0" + Integer.toString(i) + "bulk";
        elements.add(new Element<Object>(bk.getBytes(), workloads[0], eflag));
      }
      if (!cli.before_request())
        return false;
      CollectionFuture<Map<Integer, CollectionOperationStatus>> f =
        cli.next_ac.asyncBopPipedInsertBulk(key_list.get(0), elements,
                                            new CollectionAttributes());
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
        }
      }
      if (!cli.after_request(true))
        return false;
    }

    // BopGet Range + filter
    for (int j = 0; j < 1; j++) {
      if (!cli.before_request())
        return false;
      String bk = bkeyBASE + Integer.toString(j) + Integer.toString(0);
      String bk_to = bkeyBASE + Integer.toString(j) + Integer.toString(10);
      byte[] bkey = bk.getBytes();
      byte[] bkey_to = bk_to.getBytes();
      CollectionFuture<Map<ByteArrayBKey, Element<Object>>> f =
        cli.next_ac.asyncBopGet(key_list.get(j), bkey, bkey_to, filter,
                                0, random.nextInt(10) + 10,
                                /* random.randint(10, 20) */
                                false, false);
      Map<ByteArrayBKey, Element<Object>> val =
        f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (val == null || val.size() <= 0) {
        System.out.printf("Collection_Btree: BopGet failed." +
                          " id=%d key=%s val.size=%d\n", cli.id,
                          key_list.get(j), val == null ? -1 : 0);
      }
      if (!cli.after_request(true))
        return false;
    }

    // BopGetBulk  // 20120319 Ad
    {
      if (!cli.before_request())
        return false;
      String bk = bkeyBASE + "0" + "0";
      String bk_to = bkeyBASE + "2" + "20";
      byte[] bkey = bk.getBytes();
      byte[] bkey_to = bk_to.getBytes();
      CollectionGetBulkFuture
        <Map<String, BTreeGetResult<ByteArrayBKey, Object>>> f =
        cli.next_ac.asyncBopGetBulk(key_list, bkey, bkey_to, filter, 0,
                                    random.nextInt(10) + 10
                                    /* random.randint(10, 20) */);
      Map<String, BTreeGetResult<ByteArrayBKey, Object>> val =
        f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (val == null || val.size() <= 0) {
        System.out.printf("Collection_Btree: BopGetBulk failed." +
                          " id=%d val.size=%d\n", cli.id,
                          val == null ? -1 : 0);
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
                                   new CollectionAttributes());
      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("Collection_Btree: BopCreate failed." +
                          " id=%d key=%s: %s\n", cli.id, key,
                          f.getOperationStatus().getResponse());
      }
      if (!cli.after_request(ok))
        return false;
    }

    // BopSMGet
    {
      if (!cli.before_request())
        return false;
      String bk = bkeyBASE + "0" + "0";
      String bk_to = bkeyBASE + "2" + "10";
      byte[] bkey = bk.getBytes();
      byte[] bkey_to = bk_to.getBytes();
      SMGetFuture<List<SMGetElement<Object>>> f =
        cli.next_ac.asyncBopSortMergeGet(key_list, bkey, bkey_to,
                                         filter,
                                         random.nextInt(10) + 10
                                         /* random.randint(10, 20) */,
                                         false);
      List<SMGetElement<Object>> val = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (val == null || val.size() <= 0) {
        System.out.printf("Collection_Btree: BopSortMergeGet failed." +
                          " id=%d val.size=%d\n", cli.id,
                          val == null ? -1 : 0);
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
      for (int i = 0; i < 1; i++) {
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
        }
        if (!cli.after_request(ok))
          return false;
      }
    }

    // SetAttr  (change Expire Time)
    {
      if (!cli.before_request())
        return false;
      attr.setExpireTime(100);
      CollectionFuture<Boolean> f = cli.next_ac.asyncSetAttr(key, attr);
      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("Collection_Btree: SetAttr failed." +
                          " id=%d key=%s: %s\n", cli.id, key,
                          f.getOperationStatus().getResponse());
      }
      if (!cli.after_request(ok))
        return false;
    }

    // BopDelete          (eflag filter delete)
    {
      for (int j = 0; j < 1; j++) {
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
        }
        if (!cli.after_request(ok))
          return false;
      }
    }

    return true;
  }

  public boolean do_Collection_Map(client cli) throws Exception {
    String key = cli.ks.get_key_by_cliid(cli);
    List<String> key_list = new LinkedList<String>();
    for (int i = 0; i < 1; i++)
      key_list.add(key + lowercase.charAt(i));

    String mkeyBASE = "mkey";

    CollectionAttributes attr = new CollectionAttributes();
    attr.setExpireTime(cli.conf.client_exptime);

    String[] workloads = { chunk_values[4],
                           chunk_values[5],
                           chunk_values[6],
                           chunk_values[7],
                           chunk_values[8] };

    // MopInsert
    for (int j = 0; j < 1; j++) {
      // Insert 10 mkey
      for (int i = 0; i < 10; i++) {
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
        }
        if (!cli.after_request(ok))
          return false;
      }
    }

    // MopInsert Bulk (Piped)
    {
      Map<String, Object> elements = new HashMap<String, Object>();
      for (int i = 0; i < 10; i++) {
        String mkey = mkeyBASE + Integer.toString(i) + "bulk";
        elements.put(mkey, workloads[0]);
      }
      if (!cli.before_request())
        return false;
      CollectionFuture<Map<Integer, CollectionOperationStatus>> f =
              cli.next_ac.asyncMopPipedInsertBulk(key_list.get(0), elements,
                      new CollectionAttributes());
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
      if (val == null || val.size() != 20) {
        System.out.printf("Collection_Map: MopGet all failed." +
                        " id=%d key=%s val.size=%d\n", cli.id,
                key_list.get(0), val == null ? -1 : 0);
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
                      new CollectionAttributes());
      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("Collection_Map: MopCreate failed." +
                        " id=%d key=%s: %s\n", cli.id, key,
                f.getOperationStatus().getResponse());
      }
      if (!cli.after_request(ok))
        return false;
    }

    // MopUpdate
    {
      String key0 = key_list.get(0);
      String value = "ThisIsChangeValue";
      for (int i = 0; i < 1; i++) {
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
        }
        if (!cli.after_request(ok))
          return false;
      }
    }

    // SetAttr  (change Expire Time)
    {
      if (!cli.before_request())
        return false;
      attr.setExpireTime(100);
      CollectionFuture<Boolean> f = cli.next_ac.asyncSetAttr(key, attr);
      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("Collection_Map: SetAttr failed." +
                        " id=%d key=%s: %s\n", cli.id, key,
                f.getOperationStatus().getResponse());
      }
      if (!cli.after_request(ok))
        return false;
    }

    // MopDelete
    for (int j = 0; j < 1; j++) {
      // Delete 50 mkey
      for (int i = 0; i < 10; i++) {
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
        }
        if (!cli.after_request(ok))
          return false;
      }
    }

    return true;
  }

  public boolean do_Collection_Set(client cli) throws Exception {
    String key = cli.ks.get_key_by_cliid(cli);
    List<String> key_list = new LinkedList<String>();
    for (int i = 0; i < 1; i++)
      key_list.add(key + lowercase.charAt(i));

    CollectionAttributes attr = new CollectionAttributes();
    attr.setExpireTime(cli.conf.client_exptime);

    String[] workloads = { chunk_values[4],
                           chunk_values[5],
                           chunk_values[6],
                           chunk_values[7],
                           chunk_values[8] };

    // SopInsert
    {
      for (int i = 0; i < 1; i++) {
        for (int j = 0; j < 10; j++) {
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
          }
          if (!cli.after_request(ok))
            return false;
        }
      }
    }

    // SopInsert Bulk (Piped)
    {
      List<Object> elements = new LinkedList<Object>();
      for (int i = 0; i < 10; i++) {
        elements.add((Integer.toString(i) + "_" + workloads[0]));
      }
      if (!cli.before_request())
        return false;
      CollectionFuture<Map<Integer, CollectionOperationStatus>> f =
        cli.next_ac.asyncSopPipedInsertBulk(key_list.get(0), elements,
                                            new CollectionAttributes());
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
                                   new CollectionAttributes());
      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("Collection_Set: SopCreate failed." +
                          " id=%d key=%s: %s\n", cli.id, key,
                          f.getOperationStatus().getResponse());
      }
      if (!cli.after_request(ok))
        return false;
    }

    // SopExist    (Piped exist)
    {
      for (int i = 0; i < 1; i++) {
        List<Object> list_value = new LinkedList<Object>();
        for (int j = 0; j < 10; j++) {
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
          }
          if (!cli.after_request(true))
            return false;
        }
      }
    }

    // SetAttr  (change Expire Time)
    {
      if (!cli.before_request())
        return false;
      attr.setExpireTime(100);
      CollectionFuture<Boolean> f = cli.next_ac.asyncSetAttr(key, attr);
      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("Collection_Set: SetAttr failed." +
                          " id=%d key=%s: %s\n", cli.id, key,
                          f.getOperationStatus().getResponse());
      }
      if (!cli.after_request(ok))
        return false;
    }

    // SopDelete
    {
      for (int i = 0; i < 1; i++) {
        for (int j = 0; j < 10; j++) {
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
          }
          if (!cli.after_request(ok))
            return false;
        }
      }
    }

    return true;
  }

  public boolean do_Collection_List(client cli) throws Exception {
    String key = cli.ks.get_key_by_cliid(cli);
    List<String> key_list = new LinkedList<String>();
    for (int i = 0; i < 1; i++)
      key_list.add(key + lowercase.charAt(i));


    CollectionAttributes attr = new CollectionAttributes();
    attr.setExpireTime(cli.conf.client_exptime);

    String[] workloads = { chunk_values[4],
                           chunk_values[5],
                           chunk_values[6],
                           chunk_values[7],
                           chunk_values[8] };

    // LopInsert
    {
      int index = -1; // tail insert
      for (int i = 0; i < 1; i++) {
        for (int j = 0; j < 10; j++) {
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
          }
          if (!cli.after_request(ok))
            return false;
        }
      }
    }

    // LopInsert Bulk (Piped)
    {
      List<Object> elements = new LinkedList<Object>();
      for (int i = 0; i < 10; i++) {
        elements.add(Integer.toString(i) + "_" + workloads[0]);
      }
      if (!cli.before_request())
        return false;
      CollectionFuture<Map<Integer, CollectionOperationStatus>> f =
        cli.next_ac.asyncLopPipedInsertBulk(key_list.get(0), -1, elements,
                                            new CollectionAttributes());
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
        }
      }
      if (!cli.after_request(true))
        return false;
    }

    // LopGet
    {
      for (int i = 0; i < 1; i++) {
        if (!cli.before_request())
          return false;
        int index = 0;
        int index_to = index +
          /* random.randint(10, 20) */ random.nextInt(10) + 10;
        CollectionFuture<List<Object>> f =
          cli.next_ac.asyncLopGet(key_list.get(i), index, index_to,
                                  false, false);
        List<Object> val = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
        if (val == null || val.size() <= 0) {
          System.out.printf("Collection_List: LopGet failed." +
                            " id=%d key=%s val.size=%d\n",
                            cli.id, key_list.get(i),
                            val == null ? -1 : val.size());
        }
        if (!cli.after_request(true))
          return false;
      }
    }

    // LopAttr
    {
      if (!cli.before_request())
        return false;
      attr.setExpireTime(100);
      CollectionFuture<Boolean> f =
        cli.next_ac.asyncSetAttr(key_list.get(0), attr);
      boolean ok = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
      if (!ok) {
        System.out.printf("Collection_List: SetAttr failed." +
                          " id=%d key=%s: %s\n", cli.id, key_list.get(0),
                          f.getOperationStatus().getResponse());
      }
      if (!cli.after_request(ok))
        return false;
    }

    // LopDelete
    {
      int index = 0;
      int index_to = (int) (Math.random() * 20);
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
        }
        if (!cli.after_request(ok))
          return false;
      }
    }

    return true;
  }
}
