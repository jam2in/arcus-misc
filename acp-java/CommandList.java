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

public class CommandList {

  client cli;
  String key;
  boolean ok;
  List<byte[]> vals;
  int idx;

  ElementValueType vtype = ElementValueType.BYTEARRAY;
  CollectionAttributes attr;
  CollectionResponse response;
  CollectionFuture<Boolean> fb;
  CollectionFuture<List<byte[]>> f;
  CollectionFuture<List<Object>> fo;

  public CommandList(client cli) {
    this.cli = cli;
  }

  public boolean create(String key) throws Exception {
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(4000),
                                    CollectionOverflowAction.tail_trim);
    System.out.printf("[LOP CREATE] request. key=%s.\n", key);
    fb = cli.next_ac.asyncLopCreate(key, vtype, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean create(String key, long maxcount) throws Exception {
    attr = new CollectionAttributes(cli.conf.client_exptime,
                                    new Long(maxcount),
                                    CollectionOverflowAction.tail_trim);
    System.out.printf("[LOP CREATE] request. key=%s maxcount=%s\n", key, String.valueOf(maxcount));
    fb = cli.next_ac.asyncLopCreate(key, vtype, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean create(String key, CollectionAttributes attr) throws Exception {
    assert(attr != null);
    System.out.printf("[LOP CREATE] request. key=%s maxcount=%s overflowaction=%s\n", key, attr.getMaxCount(), attr.getOverflowAction());
    fb = cli.next_ac.asyncLopCreate(key, vtype, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }
  
  public boolean insert(String key, int idx, String val, CollectionAttributes attr) throws Exception {
    System.out.printf("[LOP INSERT] request. key=%s, index=%d, val=%s\n", key, idx, val);
    fb = cli.next_ac.asyncLopInsert(key, idx, val, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean delete(String key, int idx, boolean dropIfEmpty) throws Exception {
    System.out.printf("[LOP DELETE] request. key=%s index=%d, dropIfEmpty=%s\n", key, idx, dropIfEmpty);
    fb = cli.next_ac.asyncLopDelete(key, idx, dropIfEmpty);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean exist(String key) throws Exception {
    create(key);
    response = fb.getOperationStatus().getResponse();
    return TestUtil.checkResponse(key, response, CollectionResponse.EXISTS);
  }

  public byte[] get(String key, int idx, boolean withDelete, boolean dropIfEmpty) throws Exception {
    System.out.printf("[LOP GET] request. key=%s, idx=%d, withDelete=%s, dropIfEmpty=%s\n", key, idx, withDelete, dropIfEmpty);
    f = cli.next_ac.asyncLopGet(key, idx, withDelete, dropIfEmpty, raw_transcoder.raw_tc);
    vals = f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = f.getOperationStatus().getResponse();
    return getValue(vals, 0);
  }

  public boolean confirm(String key, int idx, String target) throws Exception {
    return TestUtil.checkValue(key, get(key, idx, false, false), target);
  }

  public CollectionResponse getResponse() {
    return response;
  }

  private byte[] getValue(List<byte[]> vals, int idx) {
    if (vals.isEmpty()) return null;
    else return vals.get(idx);
  }
}

