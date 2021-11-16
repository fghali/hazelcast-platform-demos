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

package hazelcast.platform.demos.industry.iiot;

import java.util.List;

/**
 * <p>Constants for the app.
 * </p>
 */
public class MyConstants {

    // Map names, for eager creation
    public static final String IMAP_NAME_SERVICE_HISTORY = "service.history";
    public static final String IMAP_NAME_SYS_CONFIG = "sys.config";
    public static final String IMAP_NAME_SYS_LOGGING = "sys.logging";
    public static final List<String> IMAP_NAMES =
            List.of(IMAP_NAME_SERVICE_HISTORY,
                    IMAP_NAME_SYS_CONFIG, IMAP_NAME_SYS_LOGGING);

    public static final String SLF4J_APPENDER_NAME = "IMAP-" + IMAP_NAME_SYS_LOGGING;
    public static final String SLF4J_LOGGER_NAME = "hazelcast.platform.demos";

    public static final String MONGO_PREFIX = "mongo.";
    public static final String MONGO_COLLECTION1 = MONGO_PREFIX + "collection1";
    public static final String MONGO_HOST = MONGO_PREFIX + "host";
    public static final String MONGO_DATABASE = MONGO_PREFIX + "database";
    public static final String MONGO_PASSWORD = MONGO_PREFIX + "password";
    public static final String MONGO_USERNAME = MONGO_PREFIX + "username";
}
