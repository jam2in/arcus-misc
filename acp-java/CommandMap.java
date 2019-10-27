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
import java.util.ArrayList;
import java.util.Map;

import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionOverflowAction;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.collection.ElementValueType;
import net.spy.memcached.internal.CollectionFuture;
import net.spy.memcached.ops.CollectionOperationStatus;

public class CommandMap {

  client cli;
  String operation;
  String key;
  String mkey;
  boolean ok;
  Map<String, byte[]> vals;

  ElementValueType vtype = ElementValueType.BYTEARRAY;
  CollectionAttributes attr;
  CollectionResponse response;
  CollectionFuture<Boolean> fb;
  CollectionFuture<Map<String, byte[]>> f;
  CollectionFuture<Map<String, Object>> fo;

  public CommandMap(client cli) {
    this.cli = cli;
  }

  public boolean create(String key) throws Exception {
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(4000),
                                    CollectionOverflowAction.error);
    System.out.printf("[MOP CREATE] request. key=%s.\n", key);
    fb = cli.next_ac.asyncMopCreate(key, vtype, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean create(String key, long maxcount) throws Exception {
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(maxcount),
                                    CollectionOverflowAction.error);
    System.out.printf("[MOP CREATE] request. key=%s.\n", key);
    fb = cli.next_ac.asyncMopCreate(key, vtype, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean insert(String key, String mkey, String val, CollectionAttributes attr) throws Exception {
    System.out.printf("[MOP INSERT] request. key=%s, mkey=%s, val=%s\n", key, mkey, val);
    fb = cli.next_ac.asyncMopInsert(key, mkey, val, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean delete(String key, String mkey, boolean dropIfEmpty) throws Exception {
    System.out.printf("[MOP DELETE] request. key=%s, mkey=%s, dropIfEmpty=%s\n", key, mkey, dropIfEmpty);
    fb = cli.next_ac.asyncMopDelete(key, mkey, dropIfEmpty);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean update(String key, String mkey, String val) throws Exception {
    System.out.printf("[MOP UPDATE] request. key=%s, mkey=%s, val=%s\n", key, mkey, val);
    fb = cli.next_ac.asyncMopUpdate(key, mkey, val);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean exist(String key) throws Exception {
    create(key);
    response = fb.getOperationStatus().getResponse();
    TestUtil.checkResponse(key, response, CollectionResponse.EXISTS);
    return true;
  }

  public byte[] get(String key, String field, boolean withDelete, boolean dropIfEmpty) throws Exception {
    System.out.printf("[MOP GET] request. key=%s field=%s, withDelete=%s, dropIfEmpty=%s\n", key, field, withDelete, dropIfEmpty);
    f = cli.next_ac.asyncMopGet(key, field, withDelete, dropIfEmpty, raw_transcoder.raw_tc);
    vals = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = f.getOperationStatus().getResponse();
    return getValue(vals, field);
  }

  public Map<String, byte[]> get(String key, List<String> flist, boolean withDelete, boolean dropIfEmpty) throws Exception {
    System.out.printf("[MOP GET] request. key=%s field list=%s, withDelete=%s, dropIfEmpty=%s\n", key, flist.toString(), withDelete, dropIfEmpty);
    f = cli.next_ac.asyncMopGet(key, flist, withDelete, dropIfEmpty, raw_transcoder.raw_tc);
    vals = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = f.getOperationStatus().getResponse();
    return vals;
  }

  public boolean confirm(String key, String field, String target) throws Exception {
    return TestUtil.checkValue(key, get(key, field, false, false), target);
  }

  private byte[] getValue(Map<String, byte[]> vals, String key) {
    if (vals.isEmpty()) return null;
    return vals.get(key);
  }
}

