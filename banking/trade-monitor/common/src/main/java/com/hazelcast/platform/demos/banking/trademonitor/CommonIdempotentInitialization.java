/*
 * Copyright (c) 2008-2021, Hazelcast, Inc. All Rights Reserved.
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

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastJsonValue;
import com.hazelcast.jet.datamodel.Tuple3;
import com.hazelcast.map.IMap;

import hazelcast.platform.demos.banking.trademonitor.MyConstants;
import hazelcast.platform.demos.banking.trademonitor.NasdaqFinancialStatus;
import hazelcast.platform.demos.banking.trademonitor.NasdaqMarketCategory;
import hazelcast.platform.demos.banking.trademonitor.SymbolInfo;
import hazelcast.platform.demos.banking.trademonitor.Trade;

/**
 * <p>May be invoked from clientside or serverside to ensure serverside ready.
 * </p>
 * <p>Has to be idempotent, so a client can call at start-up without
 * having to test if another client has already run it.
 * </p>
 */
public class CommonIdempotentInitialization {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonIdempotentInitialization.class);

    /**
     * <p>Access the {@link com.hazelcast.map.IMap} and other objects
     * that are used by the example. This will create them on first
     * access, so ensuring all are visible from the outset.
     * </p>
     */
    static boolean createNeededObjects(HazelcastInstance hazelcastInstance) {
        for (String iMapName : MyConstants.IMAP_NAMES) {
            hazelcastInstance.getMap(iMapName);
        }
        // This operation can't fail
        return true;
    }

    /**
     * <p>Kafka properties can be stashed for ad-hoc jobs to use.
     * </p>
     * <p>Stock symbols are needed for trade look-up enrichment,
     * the first member to start loads them from a file into
     * a {@link com.hazelcast.map.IMap}.
     * </p>
     */
    static boolean loadNeededData(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        boolean ok = true;
        try {
            IMap<String, String> kafkaConfigMap =
                    hazelcastInstance.getMap(MyConstants.IMAP_NAME_KAFKA_CONFIG);
            IMap<String, SymbolInfo> symbolsMap =
                    hazelcastInstance.getMap(MyConstants.IMAP_NAME_SYMBOLS);

            if (!kafkaConfigMap.isEmpty()) {
                LOGGER.trace("Skip loading '{}', not empty", kafkaConfigMap.getName());
            } else {
                Properties properties = InitializerConfig.kafkaSourceProperties(bootstrapServers);

                kafkaConfigMap.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                        properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
                kafkaConfigMap.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                        properties.getProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
                kafkaConfigMap.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                        properties.getProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));

                LOGGER.trace("Loaded {} into '{}'", kafkaConfigMap.size(), kafkaConfigMap.getName());
            }

            if (!symbolsMap.isEmpty()) {
                LOGGER.trace("Skip loading '{}', not empty", symbolsMap.getName());
            } else {
                Map<String, SymbolInfo> localMap =
                        MyUtils.nasdaqListed().entrySet().stream()
                        .collect(Collectors.<Entry<String, Tuple3<String, NasdaqMarketCategory, NasdaqFinancialStatus>>,
                                String, SymbolInfo>
                                toUnmodifiableMap(
                                entry -> entry.getKey(),
                                entry -> {
                                    SymbolInfo symbolInfo = new SymbolInfo();
                                    symbolInfo.setSecurityName(entry.getValue().f0());
                                    symbolInfo.setMarketCategory(entry.getValue().f1());
                                    symbolInfo.setFinancialStatus(entry.getValue().f2());
                                    return symbolInfo;
                                }));

                symbolsMap.putAll(localMap);

                LOGGER.trace("Loaded {} into '{}'", localMap.size(), symbolsMap.getName());
            }
        } catch (Exception e) {
            LOGGER.error("loadNeededData()", e);
            ok = false;
        }
        return ok;
    }

    /**
     * <p>Define Hazelcast maps &amp; Kafka topics for later SQL querying.
     * </p>
     */
    static boolean defineQueryableObjects(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        boolean ok = true;
        ok &= defineKafka(hazelcastInstance, bootstrapServers);
        ok &= defineIMap(hazelcastInstance);
        ok &= defineIMap2(hazelcastInstance);
        return ok;
    }


    /**
     * <p>Define Kafka streams so can be directly used as a
     * querying source by SQL.
     * </p>
     *
     * @param bootstrapServers
     */
    static boolean defineKafka(HazelcastInstance hazelcastInstance, String bootstrapServers) {
        String definition1 = "CREATE EXTERNAL MAPPING IF NOT EXISTS "
                // Name for our SQL
                + MyConstants.KAFKA_TOPIC_MAPPING_PREFIX + MyConstants.KAFKA_TOPIC_NAME_TRADES
                // Name of the remote object
                + " EXTERNAL NAME " + MyConstants.KAFKA_TOPIC_NAME_TRADES
                + " ( "
                + " id             VARCHAR, "
                + " price          BIGINT, "
                + " quantity       BIGINT, "
                + " symbol         VARCHAR, "
                // Timestamp is a reserved word, need to escape. Adjust the mapping name so avoiding clash with IMap
                + " \"timestamp\"  BIGINT "
                + " ) "
                + " TYPE Kafka "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'json-flat',"
                + " 'auto.offset.reset' = 'earliest',"
                + " 'bootstrap.servers' = '" + bootstrapServers + "'"
                + " )";

        return define(definition1, hazelcastInstance);
    }


    /**
     * <p>Without this metadata, cannot query an empty
     * {@link IMap}.
     * </p>
     *
     * @param hazelcastInstance
     */
    static boolean defineIMap(HazelcastInstance hazelcastInstance) {
        String definition1 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_AGGREGATE_QUERY_RESULTS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Tuple3.class.getCanonicalName() + "'"
                + " )";

        String definition2 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_ALERTS_MAX_VOLUME
                + " ("
                + "    __key BIGINT,"
                + "    \"timestamp\" VARCHAR,"
                + "    symbol VARCHAR,"
                + "    volume BIGINT"
                + ")"
                 + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.Long',"
                + " 'valueFormat' = 'json-flat',"
                + " 'valueJavaClass' = '" + HazelcastJsonValue.class.getCanonicalName() + "'"
                + " )";

        String definition3 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_KAFKA_CONFIG
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = '" + String.class.getCanonicalName() + "',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + String.class.getCanonicalName() + "'"
                + " )";

        String definition4 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_SYMBOLS
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + SymbolInfo.class.getCanonicalName() + "'"
                + " )";

        String definition5 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_TRADES
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = '" + Trade.class.getCanonicalName() + "'"
                + " )";

        boolean ok = true;
        ok &= define(definition1, hazelcastInstance);
        ok &= define(definition2, hazelcastInstance);
        ok &= define(definition3, hazelcastInstance);
        ok &= define(definition4, hazelcastInstance);
        ok &= define(definition5, hazelcastInstance);
        return ok;
    }

    /**
     * <p>More map definitions
     * </p>
     * @param hazelcastInstance
     */
     static boolean defineIMap2(HazelcastInstance hazelcastInstance) {
        String definition6 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_PYTHON_SENTIMENT
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = 'java.lang.String'"
                + " )";

        String definition7 = "CREATE MAPPING IF NOT EXISTS "
                + MyConstants.IMAP_NAME_JOB_CONTROL
                + " TYPE IMap "
                + " OPTIONS ( "
                + " 'keyFormat' = 'java',"
                + " 'keyJavaClass' = 'java.lang.String',"
                + " 'valueFormat' = 'java',"
                + " 'valueJavaClass' = 'java.lang.String'"
                + " )";

        boolean ok = true;
        ok &= define(definition6, hazelcastInstance);
        ok &= define(definition7, hazelcastInstance);
        return ok;
    }


    /**
     * <p>Generic handler to loading definitions
     * </p>
     *
     * @param definition
     * @param hazelcastInstance
     */
    static boolean define(String definition, HazelcastInstance hazelcastInstance) {
        LOGGER.info("Definition '{}'", definition);
        try {
            hazelcastInstance.getSql().execute(definition);
            return true;
        } catch (Exception e) {
            LOGGER.error(definition, e);
            return false;
        }
    }

}
