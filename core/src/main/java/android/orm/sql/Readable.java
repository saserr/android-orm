/*
 * Copyright 2013 the original author or authors
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

package android.orm.sql;

import android.orm.util.Maybe;
import android.support.annotation.NonNull;

import java.io.Closeable;
import java.util.Set;

public interface Readable extends Closeable {

    @NonNull
    Maybe<String> getAsString(@NonNull final String key);

    @NonNull
    Maybe<Long> getAsLong(@NonNull final String key);

    @NonNull
    Maybe<Double> getAsDouble(@NonNull final String key);

    @NonNull
    Set<String> getKeys();

    int size();

    boolean start();

    boolean next();

    @Override
    void close();
}
