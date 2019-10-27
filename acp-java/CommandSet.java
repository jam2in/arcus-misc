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

public class CommandSet {

  client cli;
  String key;
  boolean ok;
  Set<byte[]> vals;

  ElementValueType vtype = ElementValueType.BYTEARRAY;
  CollectionAttributes attr;
  CollectionResponse response;
  CollectionFuture<Boolean> fb;
  CollectionFuture<Set<byte[]>> f;
  CollectionFuture<Set<Object>> fo;

  public CommandSet(client cli) {
    this.cli = cli;
  }

  public boolean create(String key) throws Exception {
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(4000),
                                    CollectionOverflowAction.error);
    System.out.printf("[SOP CREATE] request. key=%s, maxcount=4000\n", key);
    fb = cli.next_ac.asyncSopCreate(key, vtype, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean create(String key, long maxcount) throws Exception {
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(maxcount),
                                    CollectionOverflowAction.error);
    System.out.printf("[SOP CREATE] request. key=%s, maxcount=%s\n", key, String.valueOf(maxcount));
    fb = cli.next_ac.asyncSopCreate(key, vtype, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean insert(String key, String val, CollectionAttributes attr) throws Exception {
    System.out.printf("[SOP INSERT] request. key=%s val=%s\n", key, val);
    fb = cli.next_ac.asyncSopInsert(key, val, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean delete(String key, String val, boolean dropIfEmpty) throws Exception {
    System.out.printf("[SOP DELETE] request. key=%s val=%s dropIfEmpty=%s\n", key, val, dropIfEmpty);
    fb = cli.next_ac.asyncSopDelete(key, val, dropIfEmpty);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean exist(String key) throws Exception {
    create(key);
    response = fb.getOperationStatus().getResponse();
    return TestUtil.checkResponse(key, response, CollectionResponse.EXISTS);
  }

  public byte[] get(String key, int count, boolean withDelete, boolean dropIfEmpty) throws Exception {
    System.out.printf("[SOP GET] request. key=%s count=%d withDelete=%s dropIfEmtpy=%s\n", key, count, withDelete, dropIfEmpty);
    f = cli.next_ac.asyncSopGet(key, count, withDelete, dropIfEmpty, raw_transcoder.raw_tc);
    vals = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = f.getOperationStatus().getResponse();
    //FIX: get element by count. 
    return getValue(vals);
  }

  public boolean elemExist(String key, String val, boolean exist) throws Exception {
    System.out.printf("[SOP EXIST] request. key=%s val=%s exist_expected=%s\n", key, val, exist);
    fb = cli.next_ac.asyncSopExist(key, val);
    fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = fb.getOperationStatus().getResponse();
    if (exist)  {
      return TestUtil.checkResponse(key, response, CollectionResponse.EXIST);
    } else {
      return TestUtil.checkResponse(key, response, CollectionResponse.NOT_EXIST);
    }
  }

  private byte[] getValue(Set<byte[]> vals) {
    if (vals.isEmpty()) return null;
    return vals.iterator().next();
  }
}

