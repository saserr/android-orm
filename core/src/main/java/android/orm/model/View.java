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

import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import static android.orm.model.Reading.Item.action;
import static android.orm.util.Maybes.nothing;

public class View<V> implements Instance.Readable, Observer.Read {

    private static final String TAG = Property.class.getSimpleName();

    @NonNull
    private final Mapper.Read<V> mMapper;
    @NonNull
    private final Observer.Read mObserver;
    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Select.Projection mProjection;

    private final Instance.Setter<V> mSetter = new Instance.Setter<V>() {
        @Override
        public void set(@NonNull final Maybe<V> value) {
            if (value.isSomething()) {
                mValue = value;
            } else {
                Log.w(TAG, mName + " is missing"); //NON-NLS
            }
        }
    };

    @NonNull
    private Maybe<V> mValue = nothing();

    public View(@NonNull final Value.Read<V> value, @Nullable final Observer.Read observer) {
        this(Mappers.read(value), observer);
    }

    public View(@NonNull final Mapper.Read<V> mapper, @Nullable final Observer.Read observer) {
        super();

        mMapper = mapper;
        mObserver = (observer == null) ? DUMMY : observer;
        mName = mapper.getName();
        mProjection = mapper.getProjection();
    }

    public final boolean isSomething() {
        return mValue.isSomething();
    }

    public final boolean isNothing() {
        return mValue.isNothing();
    }

    @NonNull
    public final Maybe<V> get() {
        return mValue;
    }

    @Nullable
    public final V getValue() {
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
    public final Select.Projection getProjection() {
        return mProjection;
    }

    @NonNull
    @Override
    public final Reading.Item.Action prepareRead() {
        final V value = mValue.getOrElse(null);
        return action((value == null) ?
                mMapper.prepareRead() :
                mMapper.prepareRead(value), mSetter);
    }

    @Override
    public final void beforeRead() {
        mObserver.beforeRead();
    }

    @Override
    public final void afterRead() {
        mObserver.afterRead();
    }
}
