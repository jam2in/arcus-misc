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
import java.io.*;

import net.spy.memcached.internal.OperationFuture;

public class CommandFlush {

  client cli;

  OperationFuture<Boolean> fb;

  public CommandFlush(client cli) {
    this.cli = cli;
  }

  public boolean flushAll() throws Exception {
    System.out.printf("[FLUSH_ALL] request.\n");
    String[] cmd = {"bash", "-c", "echo flush_all | nc 127.0.0.1 11446"};
    Process p = Runtime.getRuntime().exec(cmd);
    BufferedReader stdInput = new BufferedReader(new 
             InputStreamReader(p.getInputStream()));

    BufferedReader stdError = new BufferedReader(new 
             InputStreamReader(p.getErrorStream()));

    // Read the output from the command
    String s = null;
    while ((s = stdInput.readLine()) != null) {
        System.out.println(s);
    }
    // Read any errors from the attempted command
    if ((s = stdError.readLine()) != null) {
      System.out.println(s);
      while ((s = stdError.readLine()) != null) {
        System.out.println(s);
      }
      return false;
    }
    return true;
  }

  public boolean flushPrefix(String prefix) throws Exception {
    System.out.printf("[FLUSH_PREFIX] request. prefix=%s\n", prefix);
    fb = cli.next_ac.flush(prefix);
    return fb.get(cli.conf.client_timeout, TimeUnit.MILLISECONDS);
  }
}

