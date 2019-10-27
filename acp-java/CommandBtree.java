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
import java.util.Map;

import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.Element;
import net.spy.memcached.collection.ElementValueType;
import net.spy.memcached.collection.ElementFlagFilter;
import net.spy.memcached.internal.CollectionFuture;
import net.spy.memcached.ops.CollectionOperationStatus;

public class CommandBtree {

  client cli;
  String operation;
  String key;
  long bkey;
  boolean ok;
  byte[] val;
  Map<Long, Element<byte[]>> vals;

  ElementValueType vtype = ElementValueType.BYTEARRAY;
  ElementFlagFilter filter;
  CollectionAttributes attr;
  CollectionResponse response;
  CollectionFuture<Boolean> fb;
  CollectionFuture<Long> fl;
  CollectionFuture<Map<Long, Element<byte[]>>> f;
  CollectionFuture<Map<Long, Element<Object>>> fo;

  public CommandBtree(client cli) {
    this.cli = cli;
  }

  public CollectionAttributes createAndGetCollectionAttributes(long maxcount) {
    return new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(maxcount),
                                    CollectionOverflowAction.smallest_trim);
  }

  public CollectionAttributes createAndGetCollectionAttributes(long maxcount, CollectionOverflowAction ovflact) {
    return new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(maxcount),
                                    ovflact);
  }

  public boolean create(String key) throws Exception {
    attr = new CollectionAttributes(cli.conf.client_exptime, new Long(4000), CollectionOverflowAction.smallest_trim);
    System.out.printf("[BOP CREATE] request. key=%s.\n", key);
    fb = cli.next_ac.asyncBopCreate(key, vtype, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean create(String key, CollectionAttributes attr) throws Exception {
    assert(attr != null);
    System.out.printf("[BOP CREATE] request. key=%s maxcount=%s overflowaction=%s\n", key, attr.getMaxCount(), attr.getOverflowAction());
    fb = cli.next_ac.asyncBopCreate(key, vtype, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }
  public boolean insert(String key, long bkey, String val, CollectionAttributes attr) throws Exception {
    System.out.printf("[BOP INSERT] request. key=%s, bkey=%s, val=%s\n", key, String.valueOf(bkey), val);
    fb = cli.next_ac.asyncBopInsert(key, bkey, null, val, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean upsert(String key, long bkey, String val, CollectionAttributes attr) throws Exception {
    System.out.printf("[BOP UPSERT] request. key=%s, bkey=%s, val=%s\n", key, String.valueOf(bkey), val);
    fb = cli.next_ac.asyncBopUpsert(key, bkey, null, val, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean delete(String key, long bkey, boolean dropIfEmpty) throws Exception {
    System.out.printf("[BOP DELETE] request. key=%s, bkey=%s, dropIfEmpty=%s\n", key, String.valueOf(bkey), dropIfEmpty);
    fb = cli.next_ac.asyncBopDelete(key, bkey, null, dropIfEmpty);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean update(String key, long bkey, String val) throws Exception {
    System.out.printf("[BOP UPDATE] request. key=%s, bkey=%s, val=%s\n", key, String.valueOf(bkey), val);
    fb = cli.next_ac.asyncBopUpdate(key, bkey, null, val);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public long incr(String key, long bkey, int val) throws Exception {
    System.out.printf("[BOP INCR] request. key=%s, bkey=%s, val=%s\n", key, String.valueOf(bkey), String.valueOf(val));
    fl = cli.next_ac.asyncBopIncr(key, bkey, val);
    return fl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public long decr(String key, long bkey, int val) throws Exception {
    System.out.printf("[BOP DECR] request. key=%s, bkey=%s, val=%s\n", key, String.valueOf(bkey), String.valueOf(val));
    fl = cli.next_ac.asyncBopDecr(key, bkey, val);
    return fl.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }
  
  public boolean exist(String key) throws Exception {
    create(key);
    response = fb.getOperationStatus().getResponse();
    TestUtil.checkResponse(key, response, CollectionResponse.EXISTS);
    return true;
  }

  public byte[] get(String key, long bkey, boolean withDelete, boolean dropIfEmpty) throws Exception {
    System.out.printf("[BOP GET] request. key=%s, bkey=%s withDelete=%s dropIfEmtpy=%s\n", key, String.valueOf(bkey), withDelete, dropIfEmpty);
    f = cli.next_ac.asyncBopGet(key, bkey, filter, withDelete, dropIfEmpty, raw_transcoder.raw_tc);
    vals = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = f.getOperationStatus().getResponse();
    return getValue(vals, bkey);
  }

  public Map<Long, Element<byte[]>> get(String key, long from, long to, boolean withDelete, boolean dropIfEmpty) throws Exception {
    System.out.printf("[BOP GET] request. key=%s, bkey_from=%s, bkey_to=%s withDelete=%s dropIfEmtpy=%s\n", key, String.valueOf(from), String.valueOf(to), withDelete, dropIfEmpty);
    f = cli.next_ac.asyncBopGet(key, from, to, filter, 0 /* offset */, (int)(to - from), withDelete, dropIfEmpty, raw_transcoder.raw_tc);
    vals = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = f.getOperationStatus().getResponse();
    return vals;
  }

  public boolean confirm(String key, long bkey, String target) throws Exception {
    return TestUtil.checkValue(key, get(key, bkey, false, false), target);
  }

  public CollectionResponse getResponse() {
      return response;
  }
  private byte[] getValue(Map<Long, Element<byte[]>> vals, long key) {
    if (vals == null || vals.isEmpty()) return null;
    return (vals.get(key)).getValue();
  }
}

