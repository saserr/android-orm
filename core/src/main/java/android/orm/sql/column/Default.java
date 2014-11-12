/*
 * Copyright 2014 the original author or authors
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

package android.orm.sql.column;

import android.orm.sql.Type;
import android.orm.sql.fragment.Constraint;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

public class Default<V> implements Constraint {

    @NonNls
    private static final String DEFAULT_NULL = "default null";

    @Nullable
    private final V mValue;
    @Nullable
    private final Producer<V> mProducer;
    @NonNls
    @Nullable
    private final String mSQL;

    public Default(@NonNull final Type<? super V> type, @Nullable final V value) {
        super();

        mValue = value;
        mProducer = null;
        mSQL = (value == null) ? DEFAULT_NULL : "default " + type.escape(value);
    }

    public Default(@Nullable final Producer<V> producer) {
        super();

        mValue = null;
        mProducer = producer;
        mSQL = (producer == null) ? DEFAULT_NULL : null;
    }

    @Nullable
    public final V get() {
        return (mProducer == null) ? mValue : mProducer.produce();
    }

    @NonNls
    @Nullable
    @Override
    public final String toSQL() {
        return mSQL;
    }
}
