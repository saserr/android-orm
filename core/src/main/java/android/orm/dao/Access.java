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

package android.orm.dao;

import android.orm.Model;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Plan;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Where;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.orm.model.Observer.beforeCreate;
import static android.orm.model.Observer.beforeUpdate;
import static android.orm.model.Plans.write;
import static android.orm.util.Maybes.something;

public final class Access {

    public abstract static class Single<E, I, U, D> extends Some<E, I, U, D> implements android.orm.Access.Single<E, I, U, D> {
        protected Single(@NonNull final Executor<E, I, U, D> executor) {
            super(executor);
        }
    }

    public abstract static class Many<E, I, U, D> extends Some<E, I, U, D> implements android.orm.Access.Many<E, I, U, D> {
        protected Many(@NonNull final Executor<E, I, U, D> executor) {
            super(executor);
        }
    }

    private abstract static class Some<E, I, U, D> implements android.orm.Access.Some<E, I, D>, android.orm.Access.Update<U> {

        @NonNull
        private final Executor<E, I, U, D> mExecutor;

        protected Some(@NonNull final Executor<E, I, U, D> executor) {
            super();

            mExecutor = executor;
        }

        protected abstract I afterCreate(@Nullable final Object model, @NonNull final I result);

        protected abstract U afterUpdate(@Nullable final Object model, @NonNull final U result);

        @NonNull
        @Override
        public final E exists() {
            return exists(Where.None);
        }

        @NonNull
        @Override
        public final E exists(@NonNull final Where where) {
            return mExecutor.exists(where);
        }

        @NonNull
        @Override
        public final I insert(@NonNull final Model model) {
            return insert(Model.toInstance(model));
        }

        @NonNull
        @Override
        public final I insert(@NonNull final Instance.Writable model) {
            return insert(model, write(model));
        }

        @NonNull
        @Override
        public final I insert(@NonNull final Writer writer) {
            return insert(writer, write(writer));
        }

        @NonNull
        @Override
        public final <M> I insert(@Nullable final M model,
                                  @NonNull final Value.Write<M> value) {
            return insert(model, write(something(model), value));
        }

        @NonNull
        @Override
        public final <M> I insert(@Nullable final M model,
                                  @NonNull final Mapper.Write<M> mapper) {
            return insert(model, mapper.prepareWrite(something(model)));
        }

        @NonNull
        @Override
        public final U update(@NonNull final Model model) {
            return update(Model.toInstance(model));
        }

        @NonNull
        @Override
        public final U update(@NonNull final Where where, @NonNull final Model model) {
            return update(where, Model.toInstance(model));
        }

        @NonNull
        @Override
        public final U update(@NonNull final Instance.Writable model) {
            return update(model, write(model));
        }

        @NonNull
        @Override
        public final U update(@NonNull final Where where, @NonNull final Instance.Writable model) {
            return update(model, where, write(model));
        }

        @NonNull
        @Override
        public final U update(@NonNull final Writer writer) {
            return update(writer, write(writer));
        }

        @NonNull
        @Override
        public final U update(@NonNull final Where where,
                              @NonNull final Writer writer) {
            return update(writer, where, write(writer));
        }

        @NonNull
        @Override
        public final <M> U update(@Nullable final M model,
                                  @NonNull final Value.Write<M> value) {
            return update(model, write(something(model), value));
        }

        @NonNull
        @Override
        public final <M> U update(@NonNull final Where where,
                                  @Nullable final M model,
                                  @NonNull final Value.Write<M> value) {
            return update(model, where, write(something(model), value));
        }

        @NonNull
        @Override
        public final <M> U update(@Nullable final M model,
                                  @NonNull final Mapper.Write<M> mapper) {
            return update(model, mapper.prepareWrite(something(model)));
        }

        @NonNull
        @Override
        public final <M> U update(@NonNull final Where where,
                                  @Nullable final M model,
                                  @NonNull final Mapper.Write<M> mapper) {
            return update(model, where, mapper.prepareWrite(something(model)));
        }

        @NonNull
        @Override
        public final D delete() {
            return delete(Where.None);
        }

        @NonNull
        @Override
        public final D delete(@NonNull final Where where) {
            return mExecutor.delete(where);
        }

        @NonNull
        private <M> I insert(@Nullable final M model, @NonNull final Plan.Write plan) {
            beforeCreate(model);
            return afterCreate(model, mExecutor.insert(plan));
        }

        @NonNull
        private <M> U update(@Nullable final M model, @NonNull final Plan.Write plan) {
            return update(model, Where.None, plan);
        }

        @NonNull
        private <M> U update(@Nullable final M model,
                             @NonNull final Where where,
                             @NonNull final Plan.Write plan) {
            beforeUpdate(model);
            return afterUpdate(model, mExecutor.update(where, plan));
        }
    }

    private Access() {
        super();
    }
}
