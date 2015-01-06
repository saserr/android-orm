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
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Condition;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static android.orm.model.Observer.beforeInsert;
import static android.orm.model.Observer.beforeUpdate;
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

        protected abstract I afterInsert(@Nullable final Object model, @NonNull final I result);

        protected abstract U afterUpdate(@Nullable final Object model, @NonNull final U result);

        @NonNull
        @Override
        public final E exists() {
            return exists(Condition.None);
        }

        @NonNull
        @Override
        public final E exists(@NonNull final Condition condition) {
            return mExecutor.exists(condition);
        }

        @NonNull
        @Override
        public final I insert(@NonNull final Model model) {
            return insert(Model.toInstance(model));
        }

        @NonNull
        @Override
        public final I insert(@NonNull final Instance.Writable model) {
            beforeInsert(model);
            return insert(model, model.prepareWriter());
        }

        @NonNull
        @Override
        public final I insert(@NonNull final Writer writer) {
            beforeInsert(writer);
            return insert(writer, writer);
        }

        @NonNull
        @Override
        public final <M> I insert(@Nullable final M model,
                                  @NonNull final Value.Write<M> value) {
            beforeInsert(model);
            return insert(model, value.write(model));
        }

        @NonNull
        @Override
        public final <M> I insert(@Nullable final M model,
                                  @NonNull final Mapper.Write<M> mapper) {
            beforeInsert(model);
            return insert(model, mapper.prepareWriter(something(model)));
        }

        @NonNull
        @Override
        public final U update(@NonNull final Model model) {
            return update(Model.toInstance(model));
        }

        @NonNull
        @Override
        public final U update(@NonNull final Condition condition, @NonNull final Model model) {
            return update(condition, Model.toInstance(model));
        }

        @NonNull
        @Override
        public final U update(@NonNull final Instance.Writable model) {
            beforeUpdate(model);
            return update(model, model.prepareWriter());
        }

        @NonNull
        @Override
        public final U update(@NonNull final Condition condition, @NonNull final Instance.Writable model) {
            beforeUpdate(model);
            return update(model, condition, model.prepareWriter());
        }

        @NonNull
        @Override
        public final U update(@NonNull final Writer writer) {
            beforeUpdate(writer);
            return update(writer, writer);
        }

        @NonNull
        @Override
        public final U update(@NonNull final Condition condition,
                              @NonNull final Writer writer) {
            beforeUpdate(writer);
            return update(writer, condition, writer);
        }

        @NonNull
        @Override
        public final <M> U update(@Nullable final M model,
                                  @NonNull final Value.Write<M> value) {
            beforeUpdate(model);
            return update(model, value.write(model));
        }

        @NonNull
        @Override
        public final <M> U update(@NonNull final Condition condition,
                                  @Nullable final M model,
                                  @NonNull final Value.Write<M> value) {
            beforeUpdate(model);
            return update(model, condition, value.write(model));
        }

        @NonNull
        @Override
        public final <M> U update(@Nullable final M model,
                                  @NonNull final Mapper.Write<M> mapper) {
            beforeUpdate(model);
            return update(model, mapper.prepareWriter(something(model)));
        }

        @NonNull
        @Override
        public final <M> U update(@NonNull final Condition condition,
                                  @Nullable final M model,
                                  @NonNull final Mapper.Write<M> mapper) {
            beforeUpdate(model);
            return update(model, condition, mapper.prepareWriter(something(model)));
        }

        @NonNull
        @Override
        public final D delete() {
            return delete(Condition.None);
        }

        @NonNull
        @Override
        public final D delete(@NonNull final Condition condition) {
            return mExecutor.delete(condition);
        }

        @NonNull
        private I insert(@Nullable final Object model, @NonNull final Writer writer) {
            return afterInsert(model, mExecutor.insert(writer));
        }

        @NonNull
        private U update(@Nullable final Object model, @NonNull final Writer writer) {
            return update(model, Condition.None, writer);
        }

        @NonNull
        private U update(@Nullable final Object model,
                         @NonNull final Condition condition,
                         @NonNull final Writer writer) {
            return afterUpdate(model, mExecutor.update(condition, writer));
        }
    }

    private Access() {
        super();
    }
}
