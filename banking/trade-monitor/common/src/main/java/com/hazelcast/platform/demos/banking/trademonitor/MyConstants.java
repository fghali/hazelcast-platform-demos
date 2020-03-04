/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
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

package com.hazelcast.platform.demos.banking.trademonitor;

import java.util.List;

/**
 * <p>Utility constants shared across the modules.
 * </p>
 */
public class MyConstants {

    public static final String KAFKA_TOPIC_NAME_TRADES = "trades";

    public static final String IMAP_NAME_AGGREGATE_QUERY_RESULTS = "AggregateQuery" + "_results";
    public static final String IMAP_NAME_SYMBOLS = "symbols";
    public static final String IMAP_NAME_TRADES = "trades";

    public static final List<String> IMAP_NAMES =
            List.of(IMAP_NAME_AGGREGATE_QUERY_RESULTS, IMAP_NAME_SYMBOLS, IMAP_NAME_TRADES);

    public static final String WEBSOCKET_PATH_TRADES = "/trades";

}
