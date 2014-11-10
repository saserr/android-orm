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

package android.orm.dao.direct;

import android.database.sqlite.SQLiteDatabase;
import android.orm.Access;
import android.orm.dao.Executor;
import android.orm.model.Observer;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.Expression;
import android.orm.sql.Readable;
import android.orm.sql.Reader;
import android.orm.sql.Select;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Where;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.orm.model.Observer.beforeRead;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;

public class Query<V> implements Expression<Producer<Maybe<V>>> {

    private static final Object AfterRead = new Function<Producer<Maybe<Object>>, Maybe<Object>>() {
        @NonNull
        @Override
        public Maybe<Object> invoke(@NonNull final Producer<Maybe<Object>> producer) {
            final Maybe<Object> result = producer.produce();
            if (result.isSomething()) {
                Observer.afterRead(result.get());
            }
            return result;
        }
    };

    @NonNull
    private final Reader<V> mReader;
    @NonNull
    private final Select mSelect;

    public Query(@NonNull final Reader<V> reader, @NonNull final Select select) {
        super();

        mReader = reader;
        mSelect = select;
    }

    @NonNull
    @Override
    public final Maybe<Producer<Maybe<V>>> execute(@NonNull final SQLiteDatabase database) {
        final Maybe<Producer<Maybe<V>>> result;

        final Readable input = mSelect.execute(mReader.getProjection(), database);
        if (input == null) {
            result = nothing();
        } else {
            try {
                result = something(mReader.read(input));
            } finally {
                input.close();
            }
        }

        return result;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public static <V> Function<Producer<Maybe<V>>, Maybe<V>> afterRead() {
        return (Function<Producer<Maybe<V>>, Maybe<V>>) AfterRead;
    }

    public static class Builder<M> implements Access.Direct.Query.Builder.Many.Refreshable<M> {

        @NonNull
        private final Executor.Direct<?, ?> mExecutor;
        @NonNull
        private final Reading<M> mReading;

        @NonNull
        private Where mWhere = Where.None;
        @Nullable
        private Order mOrder;
        @Nullable
        private Limit mLimit;
        @Nullable
        private Offset mOffset;
        @Nullable
        private M mModel;

        @SuppressWarnings("unchecked")
        private final Function<Producer<Maybe<M>>, Maybe<M>> mAfterRead = afterRead();

        public Builder(@NonNull final Executor.Direct<?, ?> executor,
                       @NonNull final Reading<M> reading) {
            super();

            mExecutor = executor;
            mReading = reading;
        }

        @NonNull
        @Override
        public final Builder<M> with(@Nullable final Where where) {
            mWhere = (where == null) ? Where.None : where;
            return this;
        }

        @NonNull
        @Override
        public final Builder<M> with(@Nullable final Order order) {
            mOrder = order;
            return this;
        }

        @NonNull
        @Override
        public final Builder<M> with(@Nullable final Limit limit) {
            mLimit = limit;
            return this;
        }

        @NonNull
        @Override
        public final Builder<M> with(@Nullable final Offset offset) {
            mOffset = offset;
            return this;
        }

        @NonNull
        @Override
        public final Builder<M> using(@Nullable final M model) {
            mModel = model;
            return this;
        }

        @NonNull
        @Override
        public final Maybe<M> execute() {
            beforeRead(mModel);
            final Plan.Read<M> plan = (mModel == null) ?
                    mReading.preparePlan() :
                    mReading.preparePlan(mModel);
            final Maybe<M> result;

            if (plan.isEmpty()) {
                Observer.afterRead(mModel);
                result = (mModel == null) ? Maybes.<M>nothing() : something(mModel);
            } else {
                result = mExecutor.query(plan, mWhere, mOrder, mLimit, mOffset).flatMap(mAfterRead);
            }

            return result;
        }
    }
}
