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

package android.orm.playground;

import android.content.Context;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.model.Mappers;
import android.orm.model.Plan;
import android.orm.model.Reading;
import android.orm.sql.Value;
import android.orm.sql.statement.Select;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Validations;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.text.Editable;
import android.widget.EditText;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static android.orm.model.Plans.EmptyWrite;
import static android.orm.model.Plans.compose;
import static android.orm.model.Plans.write;
import static android.orm.model.Reading.Item.action;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.orm.util.Validations.safeCast;
import static android.orm.util.Validations.valid;
import static android.text.Html.fromHtml;
import static android.text.TextUtils.isEmpty;

public class Form implements Instance.ReadWrite {

    private static final Object VALID_SOMETHING_NULL = Validations.<Object>valid(something(null));
    private static final Object VALID_NOTHING = valid(nothing());
    private static final Validation.Result<Plan.Write> EMPTY = valid(EmptyWrite);

    @NonNull
    private final Context mContext;
    @NonNls
    @NonNull
    private final String mName;
    @NonNull
    private final List<Entry.Read<?>> mReads;
    @NonNull
    private final List<Entry.Write> mWrites;

    private Form(@NonNull final Context context,
                 @NonNls @NonNull final String name,
                 @NonNull final List<Entry.Read<?>> reads,
                 @NonNull final List<Entry.Write> writes) {
        super();

        mContext = context;
        mName = name;
        mReads = new ArrayList<>(reads);
        mWrites = new ArrayList<>(writes);
    }

    public final boolean isValid() {
        return prepare().isValid();
    }

    @SuppressWarnings("unchecked")
    public final void clear() {
        final Maybe<Object> value = something(null);
        for (final Entry.Read<?> entry : mReads) {
            ((Instance.Setter<Object>) entry).set(value);
        }
    }

    @NonNull
    @Override
    public final String name() {
        return mName;
    }

    @NonNull
    @Override
    public final Select.Projection projection() {
        Select.Projection projection = Select.Projection.Nothing;
        for (final Entry.Read<?> entry : mReads) {
            projection = projection.and(entry.getProjection());
        }
        return projection;
    }

    @NonNull
    @Override
    public final Reading.Item.Action prepareRead() {
        final Collection<Reading.Item.Action> actions = new ArrayList<>(mReads.size());
        for (final Entry.Read<?> entry : mReads) {
            actions.add(entry.prepareRead(mContext));
        }
        return Reading.Item.compose(actions);
    }

    @NonNull
    @Override
    public final Plan.Write prepareWrite() {
        return prepare().get();
    }

    @NonNull
    private Validation.Result<Plan.Write> prepare() {
        final Collection<Plan.Write> plans = new ArrayList<>(mWrites.size());
        Validation.Result<Plan.Write> last = EMPTY;

        for (int i = 0; (i < mWrites.size()) && last.isValid(); i++) {
            last = mWrites.get(i).prepareWrite(mContext);
            if (last.isValid()) {
                plans.add(last.get());
            }
        }

        return last.isValid() ? valid(compose(plans)) : last;
    }

    public static Builder builder(@NonNls @NonNull final String name) {
        return new Builder(name);
    }

    public static class Builder {

        @NonNls
        @NonNull
        private final String mName;

        private final List<Entry.Read<?>> mReads = new ArrayList<>();
        private final List<Entry.Write> mWrites = new ArrayList<>();

        public Builder(@NonNls @NonNull final String name) {
            super();

            mName = name;
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.Readable<V> binding,
                                      @NonNull final Value.Write<V> value) {
            return bind(binding, Mappers.write(value));
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.Readable<V> binding,
                                      @NonNull final Mapper.Write<V> mapper) {
            return with(entry(binding, mapper));
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.ReadWrite<V> binding,
                                      @NonNull final Value.ReadWrite<V> value) {
            return bind(binding, Mappers.mapper(value));
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Binding.ReadWrite<V> binding,
                                      @NonNull final Mapper.ReadWrite<V> mapper) {
            return with(entry(binding, mapper));
        }

        @NonNull
        public final <V> Builder bind(@NonNull final Field<V> field,
                                      @NonNull final Value.ReadWrite<V> value) {
            return bind(field, Mappers.mapper(value));
        }

        @NonNull
        public final <M> Builder bind(@NonNull final Field<M> field,
                                      @NonNull final Mapper.ReadWrite<M> mapper) {
            return with(entry(field, mapper));
        }

        @NonNull
        public final Form build(@NonNull final Context context) {
            return new Form(context, mName, mReads, mWrites);
        }

        private Builder with(@NonNull final Entry.Write entry) {
            mWrites.add(entry);
            return this;
        }

        private <V> Builder with(@NonNull final Entry.ReadWrite<V> entry) {
            mReads.add(entry);
            mWrites.add(entry);
            return this;
        }
    }

    public interface Field<V> extends Instance.Setter<V> {

        @NonNull
        Validation.Result<Maybe<V>> get(@NonNull final Context context);

        void set(@Nullable final V v);

        @NonNull
        String getName(@NonNull final Context context);

        void setErrors(@NonNull final List<String> errors);

        @NonNull
        Field<V> checkThat(@NonNull final Validation.Localized<? super V> validation);

        @NonNull
        Field<V> withDefault(@Nullable final V v);

        @NonNull
        <T> Field<T> map(@NonNull final Converter<V, T> converter);

        interface Converter<V, T> {

            @NonNull
            Validation.Result<T> to(@NonNull final V v);

            @NonNull
            V from(@NonNull final T t);

            @NonNull
            List<String> getErrorMessages(@NonNull final String name,
                                          @NonNull final Context context);
        }

        abstract class Base<V> implements Field<V> {

            @Override
            public final void set(@Nullable final V value) {
                set(something(value));
            }

            @NonNull
            @Override
            public final Field<V> checkThat(@NonNull final Validation.Localized<? super V> validation) {
                return new Validated<>(this, validation);
            }

            @NonNull
            @Override
            public final Field<V> withDefault(@Nullable final V value) {
                return new WithDefault<>(this, value);
            }

            @NonNull
            @Override
            public final <T> Field<T> map(@NonNull final Converter<V, T> converter) {
                return new Converted<>(this, converter);
            }

            private static class Validated<V, T extends V> extends Base<T> {

                @NonNull
                private final Field<T> mField;
                @NonNull
                private final Validation.Localized<V> mValidation;

                private Validated(@NonNull final Field<T> field,
                                  @NonNull final Validation.Localized<V> validation) {
                    super();

                    mField = field;
                    mValidation = validation;
                }

                @NonNull
                @Override
                public final Validation.Result<Maybe<T>> get(@NonNull final Context context) {
                    final Validation.Result<Maybe<T>> value = mField.get(context);
                    final Validation.Result<Maybe<T>> result;

                    if (value.isValid()) {
                        result = mValidation.validate(value.get());
                        if (result.isInvalid()) {
                            setErrors(mValidation.name(getName(context)).getErrorMessages(context));
                        }
                    } else {
                        result = value;
                    }

                    return result;
                }

                @Override
                public final void set(@NonNull final Maybe<T> value) {
                    mField.set(value);
                }

                @NonNull
                @Override
                public final String getName(@NonNull final Context context) {
                    return mField.getName(context);
                }

                @Override
                public final void setErrors(@NonNull final List<String> errors) {
                    mField.setErrors(errors);
                }
            }

            private static class WithDefault<V> extends Base<V> {

                @NonNull
                private final Field<V> mField;
                @NonNull
                private final Maybe<V> mDefault;

                private WithDefault(@NonNull final Field<V> field,
                                    @Nullable final V value) {
                    super();

                    mField = field;
                    mDefault = something(value);
                }

                @NonNull
                @Override
                public final Validation.Result<Maybe<V>> get(@NonNull final Context context) {
                    return mField.get(context);
                }

                @Override
                public final void set(@NonNull final Maybe<V> value) {
                    mField.set((value.getOrElse(null) == null) ? mDefault : value);
                }

                @NonNull
                @Override
                public final String getName(@NonNull final Context context) {
                    return mField.getName(context);
                }

                @Override
                public final void setErrors(@NonNull final List<String> errors) {
                    mField.setErrors(errors);
                }
            }

            private static class Converted<V, T> extends Base<T> {

                @NonNull
                private final Field<V> mField;
                @NonNull
                private final Converter<V, T> mConverter;
                @NonNull
                private final Function<Maybe<V>, Validation.Result<Maybe<T>>> mTo;
                @NonNull
                private final Function<T, V> mFrom;

                private Converted(@NonNull final Field<V> field,
                                  @NonNull final Converter<V, T> converter) {
                    super();

                    mField = field;
                    mConverter = converter;
                    mTo = to(converter);
                    mFrom = from(converter);
                }

                @NonNull
                @Override
                public final Validation.Result<Maybe<T>> get(@NonNull final Context context) {
                    final Validation.Result<Maybe<V>> value = mField.get(context);
                    final Validation.Result<Maybe<T>> result;

                    if (value.isValid()) {
                        result = value.flatMap(mTo);
                        if (result.isInvalid()) {
                            setErrors(mConverter.getErrorMessages(getName(context), context));
                        }
                    } else {
                        result = safeCast((Validation.Result.Invalid<Maybe<V>>) value);
                    }

                    return result;
                }

                @Override
                public final void set(@NonNull final Maybe<T> value) {
                    mField.set(value.map(mFrom));
                }

                @NonNull
                @Override
                public final String getName(@NonNull final Context context) {
                    return mField.getName(context);
                }

                @Override
                public final void setErrors(@NonNull final List<String> errors) {
                    mField.setErrors(errors);
                }

                @NonNull
                private static <V, T> Function.Base<Maybe<V>, Validation.Result<Maybe<T>>> to(@NonNull final Converter<V, T> converter) {
                    return new Function.Base<Maybe<V>, Validation.Result<Maybe<T>>>() {
                        @NonNull
                        @Override
                        @SuppressWarnings("unchecked")
                        public Validation.Result<Maybe<T>> invoke(@NonNull final Maybe<V> value) {
                            final Validation.Result<Maybe<T>> result;

                            if (value.isSomething()) {
                                final V v = value.get();
                                if (v == null) {
                                    result = (Validation.Result<Maybe<T>>) VALID_SOMETHING_NULL;
                                } else {
                                    final Validation.Result<T> mapped = converter.to(v);
                                    result = mapped.isValid() ?
                                            valid(something(mapped.get())) :
                                            Validations.<Maybe<T>>safeCast((Validation.Result.Invalid<T>) mapped);
                                }
                            } else {
                                result = (Validation.Result<Maybe<T>>) VALID_NOTHING;
                            }

                            return result;
                        }
                    };
                }

                @NonNull
                private static <V, T> Function<T, V> from(@NonNull final Converter<V, T> converter) {
                    return new Function.Base<T, V>() {
                        @NonNull
                        @Override
                        public V invoke(@NonNull final T value) {
                            return converter.from(value);
                        }
                    };
                }
            }
        }
    }

    @NonNull
    public static Field<String> field(@StringRes final int id, @NonNull final EditText text) {
        return new Field.Base<String>() {

            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public Validation.Result<Maybe<String>> get(@NonNull final Context context) {
                final Editable editable = text.getText();
                return isEmpty(editable) ?
                        (Validation.Result<Maybe<String>>) VALID_NOTHING :
                        valid(something(editable.toString()));
            }

            @Override
            public void set(@NonNull final Maybe<String> value) {
                if (value.isSomething()) {
                    text.setText(value.get());
                }
            }

            @NonNull
            @Override
            public String getName(@NonNull final Context context) {
                return context.getString(id);
            }

            @Override
            public void setErrors(@NonNull final List<String> errors) {
                if (errors.isEmpty()) {
                    text.setError(null);
                } else {
                    @NonNls final StringBuilder html;
                    if (errors.size() > 1) {
                        html = new StringBuilder();
                        html.append("<ul>");
                        for (final String error : errors) {
                            html.append("<li>").append(error).append('.').append("</li>");
                        }
                        html.append("</ul>");
                    } else {
                        html = new StringBuilder(errors.get(0)).append('.');
                    }
                    text.setError(fromHtml(html.toString()));
                    text.requestFocus();
                }
            }
        };
    }

    private static final class Entry {

        public interface Read<V> extends Instance.Setter<V> {

            @NonNull
            Select.Projection getProjection();

            @NonNull
            Reading.Item.Action prepareRead(@NonNull final Context context);
        }

        public interface Write {
            @NonNull
            Validation.Result<Plan.Write> prepareWrite(@NonNull final Context context);
        }

        public interface ReadWrite<V> extends Read<V>, Write {
        }

        private Entry() {
            super();
        }
    }

    @NonNull
    private static <V> Entry.Write entry(@NonNull final Binding.Readable<V> binding,
                                         @NonNull final Mapper.Write<V> mapper) {
        return new Entry.Write() {
            @NonNull
            @Override
            public Validation.Result<Plan.Write> prepareWrite(@NonNull final Context context) {
                return valid(write(mapper, binding));
            }
        };
    }

    @NonNull
    private static <V> Entry.ReadWrite<V> entry(@NonNull final Binding.ReadWrite<V> binding,
                                                @NonNull final Mapper.ReadWrite<V> mapper) {
        return new Entry.ReadWrite<V>() {

            @NonNull
            @Override
            public Select.Projection getProjection() {
                return mapper.getProjection();
            }

            @NonNull
            @Override
            public Reading.Item.Action prepareRead(@NonNull final Context context) {
                final V value = binding.get().getOrElse(null);
                return action(
                        (value == null) ? mapper.prepareRead() : mapper.prepareRead(value),
                        binding
                );
            }

            @NonNull
            @Override
            public Validation.Result<Plan.Write> prepareWrite(@NonNull final Context context) {
                return valid(write(mapper, binding));
            }

            @Override
            public void set(@NonNull final Maybe<V> value) {
                binding.set(value);
            }
        };
    }

    @NonNull
    private static <V> Entry.ReadWrite<V> entry(@NonNull final Field<V> field,
                                                @NonNull final Mapper.ReadWrite<V> mapper) {
        return new Entry.ReadWrite<V>() {

            @NonNull
            @Override
            public Select.Projection getProjection() {
                return mapper.getProjection();
            }

            @NonNull
            @Override
            public Reading.Item.Action prepareRead(@NonNull final Context context) {
                final Validation.Result<Maybe<V>> value = field.get(context);
                final Reading.Item<V> item;

                if (value.isValid()) {
                    final V v = value.get().getOrElse(null);
                    item = (v == null) ? mapper.prepareRead() : mapper.prepareRead(v);
                } else {
                    item = mapper.prepareRead();
                }

                return action(item, field);
            }

            @NonNull
            @Override
            public Validation.Result<Plan.Write> prepareWrite(@NonNull final Context context) {
                final Validation.Result<Maybe<V>> value = field.get(context);
                final Validation.Result<Plan.Write> result;

                if (value.isValid()) {
                    result = valid(mapper.prepareWrite(value.get()));
                    field.setErrors(Collections.<String>emptyList());
                } else {
                    result = safeCast((Validation.Result.Invalid<Maybe<V>>) value);
                }

                return result;
            }

            @Override
            public void set(@NonNull final Maybe<V> value) {
                field.set(value);
            }
        };
    }
}
