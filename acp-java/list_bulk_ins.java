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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

public class list_bulk_ins implements client_profile {

  String DEFAULT_PREFIX = "arcustest-";
  int KeyLen = 20;
  char[] dummystring = 
    ("1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
     "abcdefghijlmnopqrstuvwxyz").toCharArray();
  Random random = new Random(); // repeatable is okay

  String gen_key(String name) {
    if (name == null)
	  name = "unknown";
      String prefix = DEFAULT_PREFIX;
	  String key = generateData(KeyLen);
	  return prefix + name + ":" + key;
  }

  String generateData(int length) {
    String ret = "";
	for (int loop = 0; loop < length; loop++) {
	  int randomInt = random.nextInt(60);
	  char tempchar = dummystring[randomInt];
	  ret = ret + tempchar;
	}
	return ret;
  }

  public boolean do_test(client cli) {
    try {
	  if (!do_list_test(cli))
	    return false;
	} catch (Exception e) {
	  System.out.printf("client_profile exception. id=%d exception=%s\n", 
												cli.id, e.toString());
      e.printStackTrace();
    }
	return true;
  }

  public boolean do_list_test(client cli) throws Exception {
	int loop_cnt = 100;

    // Prepare Key list
	String key = gen_key("Collection_List");

	List<String> key_list = new LinkedList<String>();	
    for (int i = 0; i < loop_cnt; i++)
	  key_list.add(key + i);

	// Create a list item
	if (!cli.before_request())
	  return false;
	ElementValueType vtype = ElementValueType.BYTEARRAY;
	CollectionAttributes attr = 
	  new CollectionAttributes(cli.conf.client_exptime,
							   CollectionAttributes.DEFAULT_MAXCOUNT,
							   CollectionOverflowAction.tail_trim);

	// Overflow should be error, head_trim, or tail_trim
	for (int i = 0; i < loop_cnt; i++) { 
	  CollectionFuture<Boolean> fb = 
	    cli.next_ac.asyncLopCreate(key_list.get(i), vtype, attr);
		
	  boolean ok = fb.get(1000L, TimeUnit.MILLISECONDS);
	  if (!ok) {
	    System.out.printf("lop create failed. id=%d key=%s\n", cli.id,
							  key, fb.getOperationStatus().getResponse());
	  }
	  if (!cli.after_request(ok))
	    return false;
	}

	byte[] val = cli.vset.get_value();
	assert(val.length <= 4096);
		
	// repeat 4000
	for (int i = 0; i < 4000; i++) {
	  // CollectionFuture<Boolean> fbs = cli.next_ac.asyncLopInsertBulk(key_list, -1, val, new CollectionAttributes());
	  Future<Map<String, CollectionOperationStatus>> fbs = 
	    cli.next_ac.asyncLopInsertBulk(key_list, -1, val, new CollectionAttributes());
	  Map<String, CollectionOperationStatus> result = fbs.get(1000L, TimeUnit.MILLISECONDS);
		
	  if (!cli.after_request(true))
	    return false;
	}

	return true;
  }
}
