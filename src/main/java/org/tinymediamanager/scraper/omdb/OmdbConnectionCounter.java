/*
 * Copyright 2012 - 2019 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tinymediamanager.scraper.omdb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tinymediamanager.scraper.util.RingBuffer;

/**
 * The class OmdbConnectionCounter is a helper class to count the connection and throttle if needed
 *
 * @author Manuel Laggner
 */
public class OmdbConnectionCounter {
  private static final Logger           LOGGER             = LoggerFactory.getLogger(OmdbConnectionCounter.class);
  private static final RingBuffer<Long> CONNECTION_COUNTER = new RingBuffer<>(10);

  public static void trackConnections() {
    Long currentTime = System.currentTimeMillis();
    if (CONNECTION_COUNTER.count() == CONNECTION_COUNTER.maxSize()) {
      Long oldestConnection = CONNECTION_COUNTER.getTailItem();
      if (oldestConnection > (currentTime - 15000)) {
        LOGGER.debug("connection limit reached, throttling " + CONNECTION_COUNTER);
        try {
          Thread.sleep(15000 - (currentTime - oldestConnection));
        }
        catch (InterruptedException e) {
          LOGGER.warn(e.getMessage());
        }
      }
    }

    currentTime = System.currentTimeMillis();
    CONNECTION_COUNTER.add(currentTime);
  }
}
