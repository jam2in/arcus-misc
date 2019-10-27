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

import net.spy.memcached.collection.CollectionResponse;
import net.spy.memcached.CASResponse;

public class TestUtil {

  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_RED = "\u001B[31m";
  private static final String ANSI_GREEN = "\u001B[32m";

  public static void printRequestStart(String command) {
    System.out.printf(ANSI_GREEN + "\n########## %s REQUEST TEST START ##########\n" + ANSI_RESET, command.toUpperCase());
  }
  public static void printRequestSuccess(String command) {
    System.out.printf(ANSI_GREEN + "########## %s REQUEST TEST SUCCESS ##########\n" + ANSI_RESET, command.toUpperCase());
  }
  public static void printConfirmStart(String command) {
    System.out.printf(ANSI_GREEN + "\n########## %s CONFIRM TEST START ##########\n" + ANSI_RESET, command.toUpperCase());
  }
  public static void printConfirmSuccess(String command) {
    System.out.printf(ANSI_GREEN + "########## %s CONFIRM TEST SUCCESS ##########\n" + ANSI_RESET, command.toUpperCase());
  }
  public static void printRequestError(String command, String key) {
    System.out.printf(ANSI_RED + "%s REQUEST TEST FAILED!! key=%s \n" + ANSI_RESET, command.toUpperCase(), key);
    assert(false);
  }
  public static void printConfirmError(String command, String key) {
    System.out.printf(ANSI_RED + "%s CONFIRM TEST FAILED!! key=%s \n" + ANSI_RESET, command.toUpperCase(), key);
    assert(false);
  }

  public static String getTestKey(String key, String ext) {
//    return key + ext;
    return ext;
  }

  public static boolean checkValue(String key, byte[] val, String confirm) {
    String _val = (val == null) ? "null" : new String(val);
    System.out.printf("key=%s, response=%s, expected=%s\n", key, _val, confirm);
    if (confirm == null && val == null) {
      return true;
    } else if (_val.equals(confirm)) {
      return true;
    }
    return false;
  }

  public static boolean checkResponse(String key, CollectionResponse res, CollectionResponse confirm) {
    System.out.printf("key=%s, response=%s, expected=%s\n", key, res.toString(), confirm.toString());
    return res.equals(confirm);
  }

  public static void checkResponse(String key, CASResponse res, CASResponse confirm) {
    System.out.printf("key=%s, response=%s, expected=%s\n", key, res.toString(), confirm.toString());
    assert res.equals(confirm);
  }
}
