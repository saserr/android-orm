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

package android.orm.dao.local;

import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.orm.Access;
import android.orm.Route;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Reading;
import android.orm.sql.AggregateFunction;
import android.orm.sql.Value;
import android.orm.sql.statement.Select;
import android.orm.util.Maybe;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static java.lang.System.arraycopy;

public interface Executable<V> {

    @NonNull
    Maybe<V> execute(@NonNull final DAO dao, @NonNull final SQLiteDatabase database);

    class SomeAccess implements Access.Exists<Executable<Boolean>>, Access.Write<Executable<Uri>, Executable<Integer>, Executable<Integer>> {

        @NonNull
        private final Route mRoute;
        @NonNull
        private final Object[] mArguments;

        public SomeAccess(@NonNull final Route route, @NonNull final Object... arguments) {
            super();

            mRoute = route;
            mArguments = new Object[arguments.length];
            arraycopy(arguments, 0, mArguments, 0, mArguments.length);
        }

        @NonNull
        @Override
        public final Executable<Boolean> exists() {
            return new OnAccess<Boolean>(mRoute, mArguments) {
                @Override
                protected Statement<Boolean> execute(@NonNull final DAO.Access.Some access) {
                    return access.exists();
                }
            };
        }

        @NonNull
        @Override
        public final Executable<Boolean> exists(@NonNull final Select.Where where) {
            return new OnAccess<Boolean>(mRoute, mArguments) {
                @Override
                protected Statement<Boolean> execute(@NonNull final DAO.Access.Some access) {
                    return access.exists();
                }
            };
        }

        @NonNull
        @Override
        public final <M extends Instance.Writable> Executable<Uri> insert(@NonNull final M model) {
            return new OnAccess<Uri>(mRoute, mArguments) {
                @Override
                protected Statement<Uri> execute(@NonNull final DAO.Access.Some access) {
                    return access.insert(model);
                }
            };
        }

        @NonNull
        @Override
        public final <M> Executable<Uri> insert(@NonNull final M model,
                                                @NonNull final Value.Write<M> value) {
            return new OnAccess<Uri>(mRoute, mArguments) {
                @Override
                protected Statement<Uri> execute(@NonNull final DAO.Access.Some access) {
                    return access.insert(model, value);
                }
            };
        }

        @NonNull
        @Override
        public final <M> Executable<Uri> insert(@NonNull final M model,
                                                @NonNull final Mapper.Write<M> mapper) {
            return new OnAccess<Uri>(mRoute, mArguments) {
                @Override
                protected Statement<Uri> execute(@NonNull final DAO.Access.Some access) {
                    return access.insert(model, mapper);
                }
            };
        }

        @NonNull
        @Override
        public final <M extends Instance.Writable> Executable<Integer> update(@NonNull final M model) {
            return new OnAccess<Integer>(mRoute, mArguments) {
                @Override
                protected Statement<Integer> execute(@NonNull final DAO.Access.Some access) {
                    return access.update(model);
                }
            };
        }

        @NonNull
        @Override
        public final <M extends Instance.Writable> Executable<Integer> update(@NonNull final Select.Where where,
                                                                              @NonNull final M model) {
            return new OnAccess<Integer>(mRoute, mArguments) {
                @Override
                protected Statement<Integer> execute(@NonNull final DAO.Access.Some access) {
                    return access.update(where, model);
                }
            };
        }

        @NonNull
        @Override
        public final <M> Executable<Integer> update(@NonNull final M model,
                                                    @NonNull final Value.Write<M> value) {
            return new OnAccess<Integer>(mRoute, mArguments) {
                @Override
                protected Statement<Integer> execute(@NonNull final DAO.Access.Some access) {
                    return access.update(model, value);
                }
            };
        }

        @NonNull
        @Override
        public final <M> Executable<Integer> update(@NonNull final Select.Where where,
                                                    @NonNull final M model,
                                                    @NonNull final Value.Write<M> value) {
            return new OnAccess<Integer>(mRoute, mArguments) {
                @Override
                protected Statement<Integer> execute(@NonNull final DAO.Access.Some access) {
                    return access.update(where, model, value);
                }
            };
        }

        @NonNull
        @Override
        public final <M> Executable<Integer> update(@NonNull final M model,
                                                    @NonNull final Mapper.Write<M> mapper) {
            return new OnAccess<Integer>(mRoute, mArguments) {
                @Override
                protected Statement<Integer> execute(@NonNull final DAO.Access.Some access) {
                    return access.update(model, mapper);
                }
            };
        }

        @NonNull
        @Override
        public final <M> Executable<Integer> update(@NonNull final Select.Where where,
                                                    @NonNull final M model,
                                                    @NonNull final Mapper.Write<M> mapper) {
            return new OnAccess<Integer>(mRoute, mArguments) {
                @Override
                protected Statement<Integer> execute(@NonNull final DAO.Access.Some access) {
                    return access.update(where, model, mapper);
                }
            };
        }

        @NonNull
        @Override
        public final Executable<Integer> delete() {
            return new OnAccess<Integer>(mRoute, mArguments) {
                @Override
                protected Statement<Integer> execute(@NonNull final DAO.Access.Some access) {
                    return access.delete();
                }
            };
        }

        @NonNull
        @Override
        public final Executable<Integer> delete(@NonNull final Select.Where where) {
            return new OnAccess<Integer>(mRoute, mArguments) {
                @Override
                protected Statement<Integer> execute(@NonNull final DAO.Access.Some access) {
                    return access.delete(where);
                }
            };
        }

        private abstract static class OnAccess<V> implements Executable<V> {

            @NonNull
            private final Route mRoute;
            @NonNull
            private final Object[] mArguments;

            private OnAccess(@NonNull final Route route, @NonNull final Object... arguments) {
                super();

                mRoute = route;
                mArguments = arguments;
            }

            protected abstract Statement<V> execute(@NonNull final DAO.Access.Some access);

            @NonNull
            @Override
            public final Maybe<V> execute(@NonNull final DAO dao,
                                          @NonNull final SQLiteDatabase database) {
                return execute(dao.at(mRoute, mArguments)).execute(database);
            }
        }
    }

    abstract class Query<V> implements Access.Query.Builder<V, Executable<V>> {

        @NonNull
        private Select.Where mWhere = Select.Where.None;
        @Nullable
        private Select.Order mOrder;
        @Nullable
        private Select.Limit mLimit;
        @Nullable
        private Select.Offset mOffset;

        protected Query() {
            super();
        }

        @NonNull
        protected abstract DAO.Access.Query<V> query(@NonNull final DAO dao);

        @NonNull
        @Override
        public final Query<V> with(@Nullable final Select.Where where) {
            mWhere = (where == null) ? Select.Where.None : where;
            return this;
        }

        @NonNull
        @Override
        public final Query<V> with(@Nullable final Select.Order order) {
            mOrder = order;
            return this;
        }

        @NonNull
        public final Query<V> with(@Nullable final Select.Limit limit) {
            mLimit = limit;
            return this;
        }

        @NonNull
        public final Query<V> with(@Nullable final Select.Offset offset) {
            mOffset = offset;
            return this;
        }

        @NonNull
        @Override
        public final Executable<V> execute() {
            return new Executable<V>() {
                @NonNull
                @Override
                public Maybe<V> execute(@NonNull final DAO dao,
                                        @NonNull final SQLiteDatabase database) {
                    return query(dao).with(mWhere).with(mOrder).with(mLimit).with(mOffset)
                            .execute()
                            .execute(database);
                }
            };
        }

        public static class Single<V> extends Query<V> {

            @NonNull
            private final Reading.Single<V> mReading;
            @NonNull
            private final Route.Item mRoute;
            @NonNull
            private final Object[] mArguments;

            public Single(@NonNull final Reading.Single<V> reading,
                          @NonNull final Route.Item route,
                          @NonNull final Object... arguments) {
                super();

                mReading = reading;
                mRoute = route;
                mArguments = new Object[arguments.length];
                arraycopy(arguments, 0, mArguments, 0, mArguments.length);
            }

            @NonNull
            @Override
            protected final DAO.Access.Query<V> query(@NonNull final DAO dao) {
                return dao.at(mRoute, mArguments).query(mReading);
            }
        }

        public static class Many<V> extends Query<V> {

            @NonNull
            private final Reading.Many<V> mReading;
            @NonNull
            private final Route.Dir mRoute;
            @NonNull
            private final Object[] mArguments;

            public Many(@NonNull final Reading.Many<V> reading,
                        @NonNull final Route.Dir route,
                        @NonNull final Object... arguments) {
                super();

                mReading = reading;
                mRoute = route;
                mArguments = new Object[arguments.length];
                arraycopy(arguments, 0, mArguments, 0, mArguments.length);
            }

            @NonNull
            @Override
            protected final DAO.Access.Query<V> query(@NonNull final DAO dao) {
                return dao.at(mRoute, mArguments).query(mReading);
            }
        }

        public static class Aggregate<V> extends Query<V> {

            @NonNull
            private final AggregateFunction<V> mFunction;
            @NonNull
            private final Route.Dir mRoute;
            @NonNull
            private final Object[] mArguments;

            public Aggregate(@NonNull final AggregateFunction<V> function,
                             @NonNull final Route.Dir route,
                             @NonNull final Object... arguments) {
                super();

                mFunction = function;
                mRoute = route;
                mArguments = new Object[arguments.length];
                arraycopy(arguments, 0, mArguments, 0, mArguments.length);
            }

            @NonNull
            @Override
            protected final DAO.Access.Query<V> query(@NonNull final DAO dao) {
                return dao.at(mRoute, mArguments).query(mFunction);
            }
        }
    }
}
