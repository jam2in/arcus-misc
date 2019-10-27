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

import net.spy.memcached.CASValue;
import net.spy.memcached.CASResponse;

public class CommandSimple {

  client cli;
  String key;
  boolean ok;

  Future<Boolean> fb;
  Future<byte[]> f;

  public CommandSimple(client cli) {
    this.cli = cli;
  }

  public boolean set(String key, String val) throws Exception {
    System.out.printf("[SET] request. key=%s val=%s\n", key, val);
    fb = cli.next_ac.set(key, cli.conf.client_exptime, val);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean add(String key, String val) throws Exception {
    System.out.printf("[ADD] request. key=%s val=%s\n", key, val);
    fb = cli.next_ac.add(key, cli.conf.client_exptime, val);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean cas(String key, String val) throws Exception {
    Future<CASValue<byte[]>> fcas = cli.next_ac.asyncGets(key, raw_transcoder.raw_tc);
    CASValue<byte[]> casvalue = fcas.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    long cas = casvalue.getCas();

    System.out.printf("[CAS] request. key=%s, cas=%s, val=%s\n", key, String.valueOf(cas), val);
    Future<CASResponse> fcasr = cli.next_ac.asyncCAS(key, cas, val);
    CASResponse response = fcasr.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    TestUtil.checkResponse(key, response, CASResponse.OK);
    return true;
  }

  public boolean append(String key, String val) throws Exception {
    System.out.printf("[APPEND] request. key=%s val=%s\n", key, val);
    fb = cli.next_ac.append(100L /* not_used */, key, val);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean prepend(String key, String val) throws Exception {
    System.out.printf("[PREPEND] request. key=%s val=%s\n", key, val);
    fb = cli.next_ac.prepend(100L /* not_used */, key, val);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean replace(String key, String val) throws Exception {
    System.out.printf("[REPLACE] request. key=%s val=%s\n", key, val);
    fb = cli.next_ac.replace(key, cli.conf.client_exptime, val);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean delete(String[] keys) throws Exception {
    for (int i = 0; i < keys.length; i++) {
      ok = delete(keys[i]);
      if (!ok) return ok;
    }
    return ok;
  }

  public boolean delete(String key) throws Exception {
    fb = cli.next_ac.delete(key);
    System.out.printf("[DELETE] request. key=%s\n", key);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public long increment(String key, int val) throws Exception {
    System.out.printf("[INCREMENT] request. key=%s val=%s\n", key, String.valueOf(val));
    return cli.next_ac.incr(key, val);
  }

  public long decrement(String key, int val) throws Exception {
    System.out.printf("[DECREMENT] request. key=%s val=%s\n", key, String.valueOf(val));
    return cli.next_ac.decr(key, val);
  }

  public byte[] get(String key) throws Exception {
    System.out.printf("[GET] request. key=%s\n", key);
    f = cli.next_ac.asyncGet(key, raw_transcoder.raw_tc);
    return f.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }

  public boolean confirm(String key, String target) throws Exception {
    return TestUtil.checkValue(key, get(key), target);
  }
}

