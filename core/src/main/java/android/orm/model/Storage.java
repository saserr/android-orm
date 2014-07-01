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
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import static android.orm.model.Plans.EmptyWrite;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public abstract class Storage<V> extends Instance.Writable.Base implements Observer.Write {

    // TODO logging

    private static final String TAG = Storage.class.getSimpleName();

    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Observer.Write mObserver;

    @NonNull
    private Maybe<V> mValue = nothing();
    @Nullable
    private Maybe<V> mSaving;
    @Nullable
    private Maybe<V> mSaved;

    protected Storage(@NonNls @NonNull final String name, @Nullable final Observer.Write observer) {
        super();

        mName = name;
        mObserver = (observer == null) ? DUMMY : observer;
    }

    public final void set(@Nullable final V value) {
        mValue = something(value);
    }

    @NonNull
    protected abstract Plan.Write prepareWrite(@NonNull final Maybe<V> v);

    @NonNls
    @NonNull
    @Override
    public final String getName() {
        return mName;
    }

    @NonNull
    @Override
    public final Plan.Write prepareWrite() {
        final Plan.Write plan;

        if (mValue.equals(mSaved)) {
            plan = EmptyWrite;
        } else {
            if (mSaving == null) {
                mSaving = mValue;
                plan = prepareWrite(mValue);
            } else {
                Log.w(TAG, mName + " is being already saved! This call creates a race condition which value will actually be saved in the database and thus will be ignored.", new Throwable()); //NON-NLS
                plan = EmptyWrite;
            }
        }

        return plan;
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
    public static <V> Storage<V> create(@NonNull final Value.Write<V> value,
                                        @Nullable final Observer.Write observer) {
        return new Storage<V>(value.getName(), observer) {
            @NonNull
            @Override
            protected Plan.Write prepareWrite(@NonNull final Maybe<V> model) {
                return Plans.write(model, value);
            }
        };
    }

    @NonNull
    public static <M> Storage<M> create(@NonNull final Mapper.Write<M> mapper,
                                        @Nullable final Observer.Write observer) {
        return new Storage<M>(mapper.getName(), observer) {
            @NonNull
            @Override
            protected Plan.Write prepareWrite(@NonNull final Maybe<M> value) {
                final M m = value.getOrElse(null);
                return (m == null) ? EmptyWrite : mapper.prepareWrite(m);
            }
        };
    }
}
