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

import net.spy.memcached.collection.Attributes;
import net.spy.memcached.collection.CollectionAttributes;
import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.internal.CollectionFuture;
public class CommandAttribute {

  client cli;
  String key;
  boolean ok;

  CollectionResponse response;
  CollectionAttributes cattr;
  CollectionFuture<Boolean> fb;
  CollectionFuture<CollectionAttributes> fca;

  public CommandAttribute(client cli) {
    this.cli = cli;
  }

  /*
  public boolean setAttr(String key, String attrKey, String attrVal) throws Exception {
    assert (key != null && attrKey != null && attrVal != null);
    System.out.printf("[SETATTR] request. key=%s val=%s\n", key, val);
    fb = cli.next_ac.set(key, cli.conf.client_exptime, val);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }
  */

  /*
  public boolean setAttr(String key, Attributes attr) throws Exception {
    System.out.printf("[SETATTR] request. key=%s attrs=%s\n", key, attr.toString());
    fb = cli.next_ac.asyncSetAttr(key, attr);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }
  */

  public boolean setAttr(String key, Attributes attr) throws Exception {
    System.out.printf("[SETATTR] request. key=%s attrs=%s\n", key, attr.toString());
    fb = cli.next_ac.asyncSetAttr(key, attr);
    ok = fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = fb.getOperationStatus().getResponse();
    return ok;
  }

  public CollectionAttributes getAttr(String key) throws Exception {
    System.out.printf("[GETATTR] request. key=%s\n", key);
    fca = cli.next_ac.asyncGetAttr(key);
    cattr = fca.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
    response = fca.getOperationStatus().getResponse();
    assert(cattr != null);
    System.out.printf("%s\n", cattr.toString());
    return cattr;
  }
  /*
  public boolean confirm(String key, String target) throws Exception {
    return TestUtil.checkValue(key, get(key), target);
  }
  */
}

