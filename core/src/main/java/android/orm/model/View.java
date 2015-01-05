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

package android.orm.model;

import android.orm.sql.Reader;
import android.orm.sql.Value;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public abstract class View<V> extends Instance.Readable.Base implements Observer.Read {

    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Observer.Read mObserver;

    private final Instance.Setter<V> mSetter = new Instance.Setter<V>() {
        @Override
        public void set(@Nullable final V value) {
            mValue = something(value);
        }
    };

    @NonNull
    private Maybe<V> mValue = nothing();

    protected View(@NonNls @NonNull final String name,
                   @Nullable final Observer.Read observer) {
        super();

        mName = name;
        mObserver = (observer == null) ? DUMMY : observer;
    }

    @NonNull
    protected abstract Reader.Element<V> prepareReader(@NonNull final Maybe<V> v);

    public final boolean isSomething() {
        return mValue.isSomething();
    }

    public final boolean isNothing() {
        return mValue.isNothing();
    }

    public final boolean isNull() {
        return mValue.isSomething() && (mValue.get() == null);
    }

    @Nullable
    public final V get() {
        return mValue.getOrElse(null);
    }

    @NonNls
    @NonNull
    @Override
    public final String getName() {
        return mName;
    }

    @NonNull
    @Override
    public final Instance.Readable.Action prepareRead() {
        return Instances.action(prepareReader(mValue), mSetter);
    }

    @Override
    public final void beforeRead() {
        mObserver.beforeRead();
    }

    @Override
    public final void afterRead() {
        mObserver.afterRead();
    }

    @NonNull
    public static <V> View<V> create(@NonNull final Value.Read<V> value,
                                     @Nullable final Observer.Read observer) {
        return new View<V>(value.getName(), observer) {
            @NonNull
            @Override
            protected Reader.Element<V> prepareReader(@NonNull final Maybe<V> ignored) {
                return Plan.Read.from(value);
            }
        };
    }

    @NonNull
    public static <M> View<M> create(@NonNull final Mapper.Read<M> mapper,
                                     @Nullable final Observer.Read observer) {
        return new View<M>(mapper.getName(), observer) {
            @NonNull
            @Override
            protected Reader.Element<M> prepareReader(@NonNull final Maybe<M> value) {
                final M model = value.getOrElse(null);
                return (model == null) ? mapper.prepareReader() : mapper.prepareReader(model);
            }
        };
    }
}
