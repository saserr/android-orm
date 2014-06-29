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

package android.orm.playground;

import android.content.ContentValues;
import android.orm.Model;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Plan;
import android.orm.sql.Select;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.sql.Writer;
import android.orm.util.Legacy;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import static android.orm.model.Plans.write;
import static android.orm.sql.Types.Integer;
import static android.orm.sql.Types.Real;
import static android.orm.sql.Types.Text;
import static android.orm.util.Maybes.something;

public final class ByExample {

    @NonNull
    public static <M extends Model> Select.Where where(@NonNull final M model) {
        return where(Model.toInstance(model));
    }

    @NonNull
    public static <M extends Instance.Writable> Select.Where where(@NonNull final M model) {
        return where(write(model));
    }

    @NonNull
    public static Select.Where where(@NonNull final Writer writer) {
        return where(write(writer));
    }

    @NonNull
    public static <M> Select.Where where(@Nullable final M model,
                                         @NonNull final Value.Write<M> value) {
        return where(write(something(model), value));
    }

    @NonNull
    public static <M> Select.Where where(@Nullable final M model,
                                         @NonNull final Mapper.Write<M> mapper) {
        return where(mapper.prepareWrite(something(model)));
    }

    @NonNull
    private static Select.Where where(@NonNull final Plan.Write plan) {
        final WhereWritable output = new WhereWritable();
        plan.write(Value.Write.Operation.Update, output);
        return output.getWhere();
    }

    private static class WhereWritable implements Writable {

        private Select.Where mWhere = Select.Where.None;

        private WhereWritable() {
            super();
        }

        @NonNull
        public final Select.Where getWhere() {
            return mWhere;
        }

        @Override
        public final void putNull(@NonNls @NonNull final String key) {
            mWhere = mWhere.and(Select.where(key, Text).isNull());
        }

        @Override
        public final void put(@NonNls @NonNull final String key, @NonNull final String value) {
            mWhere = mWhere.and(Select.where(key, Text).isEqualTo(value));
        }

        @Override
        public final void put(@NonNls @NonNull final String key, @NonNull final Long value) {
            mWhere = mWhere.and(Select.where(key, Integer).isEqualTo(value));
        }

        @Override
        public final void put(@NonNls @NonNull final String key, @NonNull final Double value) {
            mWhere = mWhere.and(Select.where(key, Real).isEqualTo(value));
        }

        @Override
        public final void putAll(@NonNull final ContentValues values) {
            for (final String key : Legacy.getKeys(values)) {
                final Long integer = values.getAsLong(key);
                if (integer == null) {
                    final Double real = values.getAsDouble(key);
                    if (real == null) {
                        put(key, values.getAsString(key));
                    } else {
                        put(key, real);
                    }
                } else {
                    put(key, integer);
                }
            }
        }
    }

    private ByExample() {
        super();
    }
}
