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

import android.orm.sql.Reader;
import android.orm.sql.Writer;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Lens;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

import static android.orm.model.Plans.compose;
import static android.orm.util.Maybes.nothing;
import static java.util.Arrays.asList;

public final class Plan {

    public abstract static class Read<M> implements Reader<M> {

        @NonNull
        private final Select.Projection mProjection;

        protected Read(@NonNull final Select.Projection projection) {
            super();

            mProjection = projection;
        }

        @NonNull
        public final Select.Projection getProjection() {
            return mProjection;
        }

        public final boolean isEmpty() {
            return mProjection.isEmpty();
        }
    }

    public abstract static class Write implements Writer {

        protected Write() {
            super();
        }

        public abstract boolean isEmpty();

        @NonNull
        public final Write and(@NonNull final Write other) {
            return other.isEmpty() ? this : (isEmpty() ? other : compose(asList(this, other)));
        }

        @NonNull
        public static <M> Builder<M> builder() {
            return new Builder<>();
        }

        public static class Builder<M> {

            @NonNull
            private final Collection<Function<Maybe<M>, Write>> mEntries = new ArrayList<>();

            public Builder() {
                super();
            }

            @NonNull
            public final <V> Builder<M> put(@NonNull final Mapper.Write<V> mapper,
                                            @NonNull final Lens.Read<M, Maybe<V>> lens) {
                mEntries.add(entry(mapper, lens));
                return this;
            }

            @NonNull
            public final Write build(@NonNull final Maybe<M> result) {
                return build(result, mEntries);
            }

            @NonNull
            private static <M, V> Function<Maybe<M>, Write> entry(@NonNull final Mapper.Write<V> mapper,
                                                                  @NonNull final Lens.Read<M, Maybe<V>> lens) {
                return new Function.Base<Maybe<M>, Write>() {
                    @NonNull
                    @Override
                    public Write invoke(@NonNull final Maybe<M> value) {
                        final Maybe<V> result;

                        if (value.isSomething()) {
                            final M model = value.get();
                            result = (model == null) ? Maybes.<V>something(null) : lens.get(model);
                        } else {
                            result = nothing();
                        }

                        return mapper.prepareWrite(
                                (result == null) ? Maybes.<V>something(null) : result
                        );
                    }
                };
            }

            @NonNull
            private static <M> Write build(@NonNull final Maybe<M> result,
                                           @NonNull final Collection<Function<Maybe<M>, Write>> entries) {
                final Collection<Write> plans = new ArrayList<>(entries.size());

                for (final Function<Maybe<M>, Write> entry : entries) {
                    final Write plan = entry.invoke(result);
                    if (!plan.isEmpty()) {
                        plans.add(plan);
                    }
                }

                return compose(plans);
            }
        }
    }

    private Plan() {
        super();
    }
}
