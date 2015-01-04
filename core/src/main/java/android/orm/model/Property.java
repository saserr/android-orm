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

import android.orm.sql.Value;
import android.orm.sql.Values;
import android.orm.sql.Writer;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import static android.orm.model.Reading.Item.action;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public abstract class Property<V> extends Instance.ReadWrite.Base implements Observer.ReadWrite {

    // TODO logging

    private static final String TAG = Property.class.getSimpleName();

    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Observer.ReadWrite mObserver;

    private final Instance.Setter<V> mSetter = new Instance.Setter<V>() {
        @Override
        public void set(@Nullable final V v) {
            final Maybe<V> value = something(v);
            if (((mSaved == null) && mValue.isNothing()) || mValue.equals(mSaved)) {
                mValue = value;
            } else {
                Log.w(TAG, mName + " is dirty and will not be overwritten"); //NON-NLS
            }
            mSaved = value;
        }
    };

    @NonNull
    private Maybe<V> mValue = nothing();
    @Nullable
    private Maybe<V> mSaved;
    @Nullable
    private Maybe<V> mSaving;

    protected Property(@NonNls @NonNull final String name,
                       @Nullable final Observer.ReadWrite observer) {
        super();

        mName = name;
        mObserver = (observer == null) ? DUMMY : observer;
    }

    @NonNull
    protected abstract Reading.Item<V> prepareRead(@NonNull final Maybe<V> v);

    @NonNull
    protected abstract Writer prepareWrite(@NonNull final Maybe<V> v);

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

    public final void set(@Nullable final V value) {
        mValue = something(value);
    }

    @NonNls
    @NonNull
    @Override
    public final String getName() {
        return mName;
    }

    @NonNull
    @Override
    public final Reading.Item.Action prepareRead() {
        return action(prepareRead(mValue), mSetter);
    }

    @NonNull
    @Override
    public final Writer prepareWrite() {
        final Writer result;

        if (mValue.equals(mSaved)) {
            result = Writer.Empty;
        } else {
            if (mSaving == null) {
                mSaving = mValue;
                result = prepareWrite(mValue);
            } else {
                Log.w(TAG, mName + " is being already saved! This call creates a race condition which value will actually be saved in the database and thus will be ignored.", new Throwable()); //NON-NLS
                result = Writer.Empty;
            }
        }

        return result;
    }

    @Override
    public final void beforeRead() {
        mObserver.beforeRead();
    }

    @Override
    public final void afterRead() {
        mObserver.afterRead();
    }

    @Override
    public final void beforeCreate() {
        mObserver.beforeCreate();
    }

    @Override
    public final void afterCreate() {
        mObserver.afterCreate();
    }

    @Override
    public final void beforeUpdate() {
        mObserver.beforeUpdate();
    }

    @Override
    public final void afterUpdate() {
        mObserver.afterUpdate();
    }

    @Override
    public final void beforeSave() {
        mObserver.beforeSave();
    }

    @Override
    public final void afterSave() {
        mSaved = mSaving;
        mSaving = null;
        mObserver.afterSave();
    }

    @NonNull
    public static <V> Property<V> create(@NonNull final Value.ReadWrite<V> value,
                                         @Nullable final Observer.ReadWrite observer) {
        return new Property<V>(value.getName(), observer) {

            @NonNull
            @Override
            protected Reading.Item<V> prepareRead(@NonNull final Maybe<V> ignored) {
                return Reading.Item.Create.from(value);
            }

            @NonNull
            @Override
            protected Writer prepareWrite(@NonNull final Maybe<V> model) {
                return Values.value(value, model);
            }
        };
    }

    @NonNull
    public static <M> Property<M> create(@NonNull final Mapper.ReadWrite<M> mapper,
                                         @Nullable final Observer.ReadWrite observer) {
        return new Property<M>(mapper.getName(), observer) {

            @NonNull
            @Override
            protected Reading.Item<M> prepareRead(@NonNull final Maybe<M> value) {
                final M model = value.getOrElse(null);
                return (model == null) ? mapper.prepareRead() : mapper.prepareRead(model);
            }

            @NonNull
            @Override
            protected Writer prepareWrite(@NonNull final Maybe<M> value) {
                return mapper.prepareWrite(value);
            }
        };
    }
}
