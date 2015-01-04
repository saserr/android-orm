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

package android.orm.model;

import android.orm.sql.Column;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Condition;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import static android.orm.sql.Value.Write.Operation.Update;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public class Version extends Instance.ReadWrite.Base implements Observer.ReadWrite {

    private static final Function<Long, Long> INCREMENT = new Function<Long, Long>() {
        @NonNull
        @Override
        public Long invoke(@NonNull final Long value) {
            return value + 1;
        }
    };

    @NonNull
    private final Mapper mMapper;
    @NonNull
    private final Observer.ReadWrite mObserver;

    private final Instance.Setter<Long> mSetter = new Instance.Setter<Long>() {
        @Override
        public void set(@Nullable final Long current) {
            mCurrent = something(current);
        }
    };

    @NonNull
    private Maybe<Long> mCurrent = nothing();

    public Version(@NonNull final Column<Long> column,
                   @Nullable final Observer.ReadWrite observer) {
        super();

        mMapper = new Mapper(column);
        mObserver = (observer == null) ? DUMMY : observer;
    }

    @Nullable
    public final Long get() {
        return mCurrent.getOrElse(null);
    }

    public final void set(final long version) {
        mCurrent = something(version);
    }

    @NonNull
    @Override
    public final String getName() {
        return mMapper.getName();
    }

    @NonNull
    @Override
    public final Reading.Item.Action prepareRead() {
        return Reading.Item.action(mMapper, mSetter);
    }

    @NonNull
    @Override
    public final Writer prepareWrite() {
        return mMapper.prepareWrite(mCurrent);
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
        mCurrent = mCurrent.map(INCREMENT);
        mObserver.afterUpdate();
    }

    @Override
    public final void beforeSave() {
        mObserver.beforeSave();
    }

    @Override
    public final void afterSave() {
        mObserver.afterSave();
    }

    public static class Mapper extends android.orm.model.Mapper.ReadWrite.Base<Long> {

        @NonNull
        private final Column<Long> mColumn;
        @NonNls
        @NonNull
        private final String mName;
        @NonNull
        private final Reading.Item.Create<Long> mReading;

        public Mapper(@NonNull final Column<Long> column) {
            super();

            mColumn = column;
            mName = column.getName();
            mReading = Reading.Item.Create.from(mColumn);
        }

        @NonNull
        @Override
        public final String getName() {
            return mName;
        }

        @NonNull
        @Override
        public final Reading.Item.Create<Long> prepareRead() {
            return mReading;
        }

        @NonNull
        @Override
        public final Reading.Item.Create<Long> prepareRead(@NonNull final Long current) {
            return mReading;
        }

        @NonNull
        @Override
        public final Writer prepareWrite(@NonNull final Maybe<Long> value) {
            return new Write(mColumn, value);
        }
    }

    private static class Write implements Writer {

        @NonNull
        private final Column<Long> mColumn;
        @NonNull
        private final Maybe<Long> mValue;
        @NonNull
        private final Condition mOnUpdate;

        private Write(@NonNull final Column<Long> column, @NonNull final Maybe<Long> value) {
            super();

            mColumn = column;
            mValue = value;

            final Long current = value.getOrElse(null);
            mOnUpdate = (current == null) ?
                    Condition.Fail :
                    Condition.on(column).isEqualTo(current);
        }

        @NonNull
        @Override
        public final Condition onUpdate() {
            return mOnUpdate;
        }

        @Override
        public final void write(@NonNull final Value.Write.Operation operation,
                                @NonNull final Writable output) {
            final Maybe<Long> value = (operation == Update) ? mValue.map(INCREMENT) : mValue;
            mColumn.write(operation, value, output);
        }
    }
}
