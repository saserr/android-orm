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

package android.orm.dao.async;

import android.orm.Access;
import android.orm.dao.Executor;
import android.orm.dao.Result;
import android.orm.model.Observer;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.sql.fragment.Order;
import android.orm.sql.fragment.Where;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.orm.dao.Result.something;
import static android.orm.dao.direct.Query.afterRead;
import static android.orm.model.Observer.beforeRead;

public final class Query {

    public static class Builder<M> implements Access.Async.Query.Builder.Many.Refreshable<M> {

        @SuppressWarnings("unchecked")
        private final Function<Producer<Maybe<M>>, Maybe<M>> mAfterRead = afterRead();

        @NonNull
        private final Executor.Async<?, ?> mExecutor;
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

        public Builder(@NonNull final Executor.Async<?, ?> executor,
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
        public final Result<M> execute() {
            beforeRead(mModel);
            final Plan.Read<M> plan = (mModel == null) ?
                    mReading.preparePlan() :
                    mReading.preparePlan(mModel);
            final Result<M> result;

            if (plan.isEmpty()) {
                Observer.afterRead(mModel);
                result = (mModel == null) ? Result.<M>nothing() : something(mModel);
            } else {
                result = mExecutor.query(plan, mWhere, mOrder, mLimit, mOffset).flatMap(mAfterRead);
            }

            return result;
        }
    }

    private Query() {
        super();
    }
}
