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
import android.orm.sql.statement.Select;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import org.jetbrains.annotations.NonNls;

import static android.orm.model.Plans.EmptyWrite;
import static android.orm.model.Reading.Item.action;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public class Property<V> implements Instance.ReadWrite, Observer.ReadWrite {

    // TODO logging

    private static final String TAG = Property.class.getSimpleName();

    @NonNull
    private final Mapper.ReadWrite<V> mMapper;
    @NonNull
    private final Observer.ReadWrite mObserver;
    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final Select.Projection mProjection;

    private final Instance.Setter<V> mSetter = new Instance.Setter<V>() {
        @Override
        public void set(@NonNull final Maybe<V> value) {
            if (value.isSomething()) {
                if (((mSaved == null) && mValue.isNothing()) || mValue.equals(mSaved)) {
                    mValue = value;
                } else {
                    Log.w(TAG, mName + " is dirty and will not be overwritten"); //NON-NLS
                }
                mSaved = value;
            } else {
                Log.w(TAG, mName + " is missing"); //NON-NLS
            }
        }
    };

    @NonNull
    private Maybe<V> mValue = nothing();
    @Nullable
    private Maybe<V> mSaved;
    @Nullable
    private Maybe<V> mSaving;

    public Property(@NonNull final Value.ReadWrite<V> value,
                    @Nullable final Observer.ReadWrite observer) {
        this(Mappers.mapper(value), observer);
    }

    public Property(@NonNull final Mapper.ReadWrite<V> mapper,
                    @Nullable final Observer.ReadWrite observer) {
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

    public final void set(@Nullable final V value) {
        mValue = something(value);
    }

    @NonNls
    @NonNull
    @Override
    public final String name() {
        return mName;
    }

    @NonNull
    @Override
    public final Select.Projection projection() {
        return mProjection;
    }

    @NonNull
    @Override
    public final Reading.Item.Action prepareRead() {
        final V value = (mSaved == null) ? null : mSaved.getOrElse(null);
        return action((value == null) ?
                mMapper.prepareRead() :
                mMapper.prepareRead(value), mSetter);
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
                plan = mMapper.prepareWrite(mValue);
            } else {
                Log.w(TAG, mName + " is being already saved! This call creates a race condition which value will actually be saved in the database and thus will be ignored.", new Throwable()); //NON-NLS
                plan = EmptyWrite;
            }
        }

        return plan;
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
        mObserver.afterSave();
    }
}
