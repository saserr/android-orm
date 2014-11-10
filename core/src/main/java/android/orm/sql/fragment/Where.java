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

package android.orm.sql.fragment;

import android.content.ContentValues;
import android.orm.Model;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.sql.Column;
import android.orm.sql.Fragment;
import android.orm.sql.Helper;
import android.orm.sql.Type;
import android.orm.sql.Types;
import android.orm.sql.Value;
import android.orm.sql.Writable;
import android.orm.sql.Writer;
import android.orm.util.Function;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.text.MessageFormat;
import java.util.Map;

import static android.orm.sql.Types.Integer;
import static android.orm.sql.Types.Real;
import static android.orm.sql.Types.Text;
import static android.orm.sql.Value.Write.Operation.Visit;
import static android.orm.util.Maybes.something;

public class Where implements Fragment {

    @NonNls
    private static final MessageFormat NOT = new MessageFormat("not ({0})");
    @NonNls
    private static final MessageFormat AND = new MessageFormat("({0}) and ({1})");
    @NonNls
    private static final MessageFormat OR = new MessageFormat("({0}) or ({1})");

    public static final Where None = new Where(null);

    @NonNls
    @Nullable
    private final String mSQL;

    public Where(@NonNls @Nullable final String selection) {
        super();

        mSQL = selection;
    }

    public final boolean isEmpty() {
        return mSQL == null;
    }

    @NonNull
    public final Where not() {
        return (mSQL == null) ? None : new Where(NOT.format(new String[]{mSQL}));
    }

    @NonNull
    public final Where and(@NonNull final Where other) {
        final Where result;

        if (mSQL == null) {
            result = (other.mSQL == null) ? None : other;
        } else {
            result = (other.mSQL == null) ?
                    this :
                    new Where(AND.format(new String[]{mSQL, other.mSQL}));
        }

        return result;
    }

    @NonNull
    public final Where or(@NonNull final Where other) {
        final Where result;

        if (mSQL == null) {
            result = (other.mSQL == null) ? None : other;
        } else {
            result = (other.mSQL == null) ?
                    this :
                    new Where(OR.format(new String[]{mSQL, other.mSQL}));
        }

        return result;
    }

    @NonNls
    @Nullable
    @Override
    public final String toSQL() {
        return mSQL;
    }

    @NonNull
    public static <V> SimplePart<V> where(@NonNull final Column<V> column) {
        return where(column.getName(), column.getType());
    }

    @NonNull
    public static <V> SimplePart<V> where(@NonNls @NonNull final String name,
                                          @NonNull final Type<V> type) {
        return new SimplePart<>(name, type);
    }

    @NonNull
    public static TextPart text(@NonNull final Column<String> column) {
        return text(column.getName());
    }

    @NonNull
    public static TextPart text(@NonNls @NonNull final String name) {
        return new TextPart(name);
    }

    @NonNull
    public static <M extends Model> ComplexPart<M> model() {
        return new ComplexPart<M>() {
            @Override
            protected void write(@NonNull final M model, @NonNull final Writable writable) {
                Model.toInstance(model).prepareWrite().write(Visit, writable);
            }
        };
    }

    @NonNull
    public static <M extends Instance.Writable> ComplexPart<M> instance() {
        return new ComplexPart<M>() {
            @Override
            protected void write(@NonNull final M model, @NonNull final Writable writable) {
                model.prepareWrite().write(Visit, writable);
            }
        };
    }

    @NonNull
    public static ComplexPart<Writer> writer() {
        return new ComplexPart<Writer>() {
            @Override
            protected void write(@NonNull final Writer writer, @NonNull final Writable writable) {
                writer.write(Visit, writable);
            }
        };
    }

    @NonNull
    public static <M> ComplexPart.WithNull<M> where(@NonNull final Value.Write<M> value) {
        return new ComplexPart.WithNull<M>() {
            @Override
            protected void write(@Nullable final M model, @NonNull final Writable writable) {
                value.write(Visit, something(model), writable);
            }
        };
    }

    @NonNull
    public static <M> ComplexPart<M> where(@NonNull final Mapper.Write<M> mapper) {
        return new ComplexPart<M>() {
            @Override
            protected void write(@NonNull final M model, @NonNull final Writable writable) {
                mapper.prepareWrite(model).write(Visit, writable);
            }
        };
    }

    public static class SimplePart<V> {

        @NonNull
        private final Type<V> mType;
        @NonNls
        @NonNull
        private final String mEscapedName;

        public SimplePart(@NonNls @NonNull final String name, @NonNull final Type<V> type) {
            super();

            mType = type;
            mEscapedName = Helper.escape(name);
        }

        @NonNull
        public final Where isNull() {
            return new Where(mEscapedName + " is null");
        }

        @NonNull
        public final Where isNotNull() {
            return new Where(mEscapedName + " is not null");
        }

        @NonNull
        public final Where isEqualTo(@NonNull final V value) {
            return new Where(mEscapedName + " = " + escape(value));
        }

        @NonNull
        public final Where isNotEqualTo(@NonNull final V value) {
            return new Where(mEscapedName + " <> " + escape(value));
        }

        @NonNull
        public final Where isLessThan(@NonNull final V value) {
            return new Where(mEscapedName + " < " + escape(value));
        }

        @NonNull
        public final Where isLessOrEqualThan(@NonNull final V value) {
            return new Where(mEscapedName + " <= " + escape(value));
        }

        @NonNull
        public final Where isGreaterThan(@NonNull final V value) {
            return new Where(mEscapedName + " > " + escape(value));
        }

        @NonNull
        public final Where isGreaterOrEqualThan(@NonNull final V value) {
            return new Where(mEscapedName + " >= " + escape(value));
        }

        @NonNull
        public final Where isBetween(@NonNull final V min, @NonNull final V max) {
            return new Where(mEscapedName + " between " + escape(min) + " and " + escape(max));
        }

        @NonNull
        public final Where isNotBetween(@NonNull final V min, @NonNull final V max) {
            return new Where(mEscapedName + " not between " + escape(min) + " and " + escape(max));
        }

        @NonNull
        protected String escape(@NonNull final V value) {
            return mType.escape(value);
        }
    }

    public static class TextPart extends SimplePart<String> {

        @NonNls
        @NonNull
        private final String mEscapedName;

        public TextPart(@NonNls @NonNull final String name) {
            super(name, Text);

            mEscapedName = escape(name);
        }

        @NonNull
        public final Where isLike(@NonNull final String pattern) {
            return new Where(mEscapedName + " like " + escape(pattern));
        }

        @NonNull
        public final Where isNotLike(@NonNull final String pattern) {
            return new Where(mEscapedName + " not like " + escape(pattern));
        }

        @NonNull
        public final Where isLikeGlob(@NonNull final String pattern) {
            return new Where(mEscapedName + " glob " + escape(pattern));
        }

        @NonNull
        public final Where isNotLikeGlob(@NonNull final String pattern) {
            return new Where(mEscapedName + " not glob " + escape(pattern));
        }

        @NonNull
        public final Where isLikeRegexp(@NonNull final String pattern) {
            return new Where(mEscapedName + " regexp " + escape(pattern));
        }

        @NonNull
        public final Where isNotLikeRegexp(@NonNull final String pattern) {
            return new Where(mEscapedName + " not regexp " + escape(pattern));
        }
    }

    public abstract static class ComplexPart<V> {

        protected abstract void write(@NonNull final V v, @NonNull final Writable writable);

        @NonNull
        public final Where isEqualTo(@NonNull final V value) {
            final Builder builder = new Builder.IsEqualTo();
            write(value, builder);
            return builder.result();
        }

        @NonNull
        public final Where isNotEqualTo(@NonNull final V value) {
            final Builder builder = new Builder.IsNotEqualTo();
            write(value, builder);
            return builder.result();
        }

        @NonNull
        public final Where isLessThan(@NonNull final V value) {
            final Builder builder = new Builder.IsLessThan();
            write(value, builder);
            return builder.result();
        }

        @NonNull
        public final Where isLessOrEqualThan(@NonNull final V value) {
            final Builder builder = new Builder.IsLessOrEqualThan();
            write(value, builder);
            return builder.result();
        }

        @NonNull
        public final Where isGreaterThan(@NonNull final V value) {
            final Builder builder = new Builder.IsGreaterThan();
            write(value, builder);
            return builder.result();
        }

        @NonNull
        public final Where isGreaterOrEqualThan(@NonNull final V value) {
            final Builder builder = new Builder.IsGreaterOrEqualThan();
            write(value, builder);
            return builder.result();
        }

        public abstract static class WithNull<V> extends ComplexPart<V> {

            @Override
            protected abstract void write(@Nullable final V v, @NonNull final Writable writable);

            @NonNull
            public final Where isNull() {
                final ComplexPart.Builder builder = new ComplexPart.Builder.IsEqualTo();
                write(null, builder);
                return builder.result();
            }

            @NonNull
            public final Where isNotNull() {
                final ComplexPart.Builder builder = new ComplexPart.Builder.IsNotEqualTo();
                write(null, builder);
                return builder.result();
            }
        }

        private abstract static class Builder implements Writable {

            @NonNull
            private Where mWhere = None;

            @Nullable
            protected abstract <V> Where operation(@NonNull final SimplePart<V> part,
                                                   @Nullable final V value);

            @Override
            public final void putNull(@NonNls @NonNull final String key) {
                add(operation(new TextPart(key), null));
            }

            @Override
            public final void put(@NonNls @NonNull final String key, @NonNull final String value) {
                add(operation(new TextPart(key), value));
            }

            @Override
            public final void put(@NonNls @NonNull final String key, @NonNull final Long value) {
                add(operation(new SimplePart<>(key, Integer), value));
            }

            @Override
            public final void put(@NonNls @NonNull final String key, @NonNull final Double value) {
                add(operation(new SimplePart<>(key, Real), value));
            }

            private void add(@Nullable final Where where) {
                mWhere = (where == null) ? mWhere : mWhere.and(where);
            }

            @Override
            public final void putAll(@NonNull final ContentValues values) {
                for (final Map.Entry<String, Object> entry : values.valueSet()) {
                    final String key = entry.getKey();
                    final Object value = entry.getValue();
                    if (value == null) {
                        putNull(key);
                    } else {
                        final Double asDouble = values.getAsDouble(key);
                        final Long asLong = values.getAsLong(key);
                        final Boolean asBoolean = values.getAsBoolean(key);
                        final String asString = values.getAsString(key);

                        if (asDouble != null) {
                            put(key, asDouble);
                        } else if (asLong != null) {
                            put(key, asLong);
                        } else if (asBoolean != null) {
                            Types.Bool.write(this, key, asBoolean);
                        } else if (asString != null) {
                            put(key, asString);
                        } else {
                            throw new IllegalArgumentException("Unsupported type " + value.getClass());
                        }
                    }
                }
            }

            @NonNull
            public final Where result() {
                return mWhere;
            }

            public static class IsEqualTo extends ComplexPart.Builder {
                @NonNull
                @Override
                protected final <V> Where operation(@NonNull final SimplePart<V> part,
                                                    @Nullable final V value) {
                    return (value == null) ? part.isNull() : part.isEqualTo(value);
                }
            }

            public static class IsNotEqualTo extends ComplexPart.Builder {
                @NonNull
                @Override
                protected final <V> Where operation(@NonNull final SimplePart<V> part,
                                                    @Nullable final V value) {
                    return (value == null) ? part.isNotNull() : part.isNotEqualTo(value);
                }
            }

            public static class IsLessThan extends ComplexPart.Builder {
                @Nullable
                @Override
                protected final <V> Where operation(@NonNull final SimplePart<V> part,
                                                    @Nullable final V value) {
                    return (value == null) ? null : part.isLessThan(value);
                }
            }

            public static class IsLessOrEqualThan extends ComplexPart.Builder {
                @Nullable
                @Override
                protected final <V> Where operation(@NonNull final SimplePart<V> part,
                                                    @Nullable final V value) {
                    return (value == null) ? null : part.isLessOrEqualThan(value);
                }
            }

            public static class IsGreaterThan extends ComplexPart.Builder {
                @Nullable
                @Override
                protected final <V> Where operation(@NonNull final SimplePart<V> part,
                                                    @Nullable final V value) {
                    return (value == null) ? null : part.isGreaterThan(value);
                }
            }

            public static class IsGreaterOrEqualThan extends ComplexPart.Builder {
                @Nullable
                @Override
                protected final <V> Where operation(@NonNull final SimplePart<V> part,
                                                    @Nullable final V value) {
                    return (value == null) ? null : part.isGreaterOrEqualThan(value);
                }
            }
        }
    }

    public interface Builder<V> {

        @NonNull
        Builder<V> not();

        @NonNull
        Builder<V> and(@NonNull final Where other);

        @NonNull
        Builder<V> and(@NonNull final Builder<? super V> other);

        @NonNull
        Builder<V> or(@NonNull final Where other);

        @NonNull
        Builder<V> or(@NonNull final Builder<? super V> other);

        @NonNull
        Where build(@NonNull final V value);

        Builder<Object> None = builder(new Function<Object, Where>() {
            @NonNull
            @Override
            public Where invoke(@NonNull final Object argument) {
                return Where.None;
            }
        });
    }

    @NonNull
    public static <V> Builder<V> builder(@NonNull final Function<V, Where> factory) {
        return new Builder<V>() {

            @NonNull
            @Override
            public Builder<V> not() {
                return builder(new Function<V, Where>() {
                    @NonNull
                    @Override
                    public Where invoke(@NonNull final V value) {
                        return factory.invoke(value).not();
                    }
                });
            }

            @NonNull
            @Override
            public Builder<V> and(@NonNull final Where other) {
                return builder(new Function<V, Where>() {
                    @NonNull
                    @Override
                    public Where invoke(@NonNull final V value) {
                        return factory.invoke(value).and(other);
                    }
                });
            }

            @NonNull
            @Override
            public Builder<V> and(@NonNull final Builder<? super V> other) {
                return builder(new Function<V, Where>() {
                    @NonNull
                    @Override
                    public Where invoke(@NonNull final V value) {
                        return factory.invoke(value).and(other.build(value));
                    }
                });
            }

            @NonNull
            @Override
            public Builder<V> or(@NonNull final Where other) {
                return builder(new Function<V, Where>() {
                    @NonNull
                    @Override
                    public Where invoke(@NonNull final V value) {
                        return factory.invoke(value).or(other);
                    }
                });
            }

            @NonNull
            @Override
            public Builder<V> or(@NonNull final Builder<? super V> other) {
                return builder(new Function<V, Where>() {
                    @NonNull
                    @Override
                    public Where invoke(@NonNull final V value) {
                        return factory.invoke(value).or(other.build(value));
                    }
                });
            }

            @NonNull
            @Override
            public Where build(@NonNull final V value) {
                return factory.invoke(value);
            }
        };
    }
}
