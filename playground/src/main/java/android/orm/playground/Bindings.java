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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.orm.util.Consumer;
import android.orm.util.Converter;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.orm.util.Producer;
import android.orm.util.Validations;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.jetbrains.annotations.NonNls;

import java.util.List;

import static android.orm.util.Converters.from;
import static android.orm.util.Converters.to;
import static android.orm.util.Maybes.nothing;
import static android.orm.util.Maybes.something;
import static android.orm.util.Validations.valid;
import static android.text.Html.fromHtml;
import static android.text.TextUtils.isEmpty;

public final class Bindings {

    private static final Object VALID_SOMETHING_NULL = Validations.<Object>valid(something(null));
    private static final Object VALID_NOTHING = valid(nothing());

    @NonNull
    public static <V> Binding.Readable<V> value(@NonNull final Producer<V> producer) {
        return new Binding.Readable.Base<V>() {
            @NonNull
            @Override
            public Maybe<V> get() {
                return something(producer.produce());
            }
        };
    }

    @NonNull
    public static <V> Binding.Writable<V> value(@NonNull final Consumer<V> consumer) {
        return new Binding.Writable.Base<V>() {
            @Override
            public void set(@NonNull final Maybe<V> value) {
                if (value.isSomething()) {
                    consumer.consume(value.get());
                }
            }
        };
    }

    @NonNull
    public static <V> Binding.ReadWrite<V> value(@NonNull final AdapterView<? extends Adapter> adapter) {
        return new Binding.ReadWrite.Base<V>() {

            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public Maybe<V> get() {
                final V item = (V) adapter.getSelectedItem();
                return (item == null) ? Maybes.<V>nothing() : something(item);
            }

            @Override
            public void set(@NonNull final Maybe<V> value) {
                int position = 0;

                if (value.isSomething()) {
                    final V v = value.get();
                    if (v != null) {
                        final int count = adapter.getCount();
                        for (int i = 0; i < count; i++) {
                            if (v.equals(adapter.getItemAtPosition(i))) {
                                position = i;
                                break;
                            }
                        }
                    }
                }

                adapter.setSelection(position);
            }
        };
    }

    @NonNull
    public static Binding.ReadWrite<String> text(@NonNull final TextView text) {
        return new Binding.ReadWrite.Base<String>() {

            @NonNull
            @Override
            public Maybe<String> get() {
                final CharSequence chars = text.getText();
                return isEmpty(chars) ?
                        Maybes.<String>nothing() :
                        something(chars.toString());
            }

            @Override
            public void set(@NonNull final Maybe<String> value) {
                if (value.isSomething()) {
                    text.setText(value.get());
                }
            }
        };
    }

    @NonNull
    public static Binding.Validated<String> edit(@StringRes final int id,
                                                 @NonNull final EditText text) {
        return new Named<String>(id, text(text)) {
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

    @NonNull
    public static Binding.Writable<Uri> uri(@NonNull final ImageView view) {
        return new Binding.Writable.Base<Uri>() {
            @Override
            public void set(@NonNull final Maybe<Uri> value) {
                if (value.isSomething()) {
                    final Uri uri = value.get();
                    if (uri == null) {
                        view.setImageDrawable(null);
                    } else {
                        view.setImageURI(uri);
                    }
                }
            }
        };
    }

    @NonNull
    public static Binding.Writable<Bitmap> bitmap(@NonNull final ImageView view) {
        return new Binding.Writable.Base<Bitmap>() {
            @Override
            public void set(@NonNull final Maybe<Bitmap> value) {
                if (value.isSomething()) {
                    final Bitmap bitmap = value.get();
                    if (bitmap == null) {
                        view.setImageDrawable(null);
                    } else {
                        view.setImageBitmap(bitmap);
                    }
                }
            }
        };
    }

    @NonNull
    public static Binding.ReadWrite<Drawable> drawable(@NonNull final ImageView view) {
        return new Binding.ReadWrite.Base<Drawable>() {

            @NonNull
            @Override
            public Maybe<Drawable> get() {
                final Drawable drawable = view.getDrawable();
                return (drawable == null) ? Maybes.<Drawable>nothing() : something(drawable);
            }

            @Override
            public void set(@NonNull final Maybe<Drawable> value) {
                if (value.isSomething()) {
                    view.setImageDrawable(value.get());
                }
            }
        };
    }

    @NonNull
    public static <V, T> Binding.Readable<T> convert(@NonNull final Binding.Readable<V> binding,
                                                     @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
        return new ReadableConversion<>(binding, converter);
    }

    @NonNull
    public static <V, T> Binding.Writable<T> convert(@NonNull final Binding.Writable<V> binding,
                                                     @NonNull final Function<Maybe<T>, Maybe<V>> converter) {
        return new WritableConversion<>(binding, converter);
    }

    @NonNull
    public static <V, T> Binding.ReadWrite<T> convert(@NonNull final Binding.ReadWrite<V> binding,
                                                      @NonNull final Converter<Maybe<V>, Maybe<T>> converter) {
        return combine(
                new ReadableConversion<>(binding, from(converter)),
                new WritableConversion<>(binding, to(converter))
        );
    }

    @NonNull
    public static <V, T> Binding.Validated<T> convert(@NonNull final Binding.Validated<V> binding,
                                                      @NonNull final Binding.Validated.Converter<V, T> converter) {
        return new ValidatedConversion<>(binding, converter);
    }

    @NonNull
    public static <V> Binding.ReadWrite<V> combine(@NonNull final Binding.Readable<V> read,
                                                   @NonNull final Binding.Writable<V> write) {
        return new Combine<>(read, write);
    }

    private abstract static class Named<V> extends Binding.Validated.Base<V> {

        @StringRes
        private final int mId;
        @NonNull
        private final Binding.ReadWrite<V> mBinding;

        protected Named(@StringRes final int id, @NonNull final Binding.ReadWrite<V> binding) {
            super();

            mBinding = binding;
            mId = id;
        }

        @NonNull
        @Override
        public final Validation.Result<Maybe<V>> get(@NonNull final Context context) {
            return valid(mBinding.get());
        }

        @Override
        public final void set(@NonNull final Maybe<V> value) {
            mBinding.set(value);
        }

        @NonNull
        @Override
        public final String getName(@NonNull final Context context) {
            return context.getString(mId);
        }
    }

    private static class ReadableConversion<V, T> extends Binding.Readable.Base<T> {

        @NonNull
        private final Binding.Readable<V> mBinding;
        @NonNull
        private final Function<Maybe<V>, Maybe<T>> mConverter;

        private ReadableConversion(@NonNull final Binding.Readable<V> binding,
                                   @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
            super();

            mBinding = binding;
            mConverter = converter;
        }

        @NonNull
        @Override
        public final Maybe<T> get() {
            return mConverter.invoke(mBinding.get());
        }
    }

    private static class WritableConversion<V, T> extends Binding.Writable.Base<T> {

        @NonNull
        private final Binding.Writable<V> mBinding;
        @NonNull
        private final Function<Maybe<T>, Maybe<V>> mConverter;

        private WritableConversion(@NonNull final Binding.Writable<V> binding,
                                   @NonNull final Function<Maybe<T>, Maybe<V>> converter) {
            super();

            mBinding = binding;
            mConverter = converter;
        }

        @Override
        public final void set(@NonNull final Maybe<T> value) {
            mBinding.set(mConverter.invoke(value));
        }
    }

    private static class ValidatedConversion<V, T> extends Binding.Validated.Base<T> {

        @NonNull
        private final Binding.Validated<V> mBinding;
        @NonNull
        private final Binding.Validated.Converter<V, T> mConverter;
        @NonNull
        private final Function<Maybe<V>, Validation.Result<Maybe<T>>> mTo;
        @NonNull
        private final Function<T, V> mFrom;

        private ValidatedConversion(@NonNull final Binding.Validated<V> binding,
                                    @NonNull final Binding.Validated.Converter<V, T> converter) {
            super();

            mBinding = binding;
            mConverter = converter;
            mTo = new To<>(converter);
            mFrom = new From<>(converter);
        }

        @NonNull
        @Override
        public final Validation.Result<Maybe<T>> get(@NonNull final Context context) {
            final Validation.Result<Maybe<V>> value = mBinding.get(context);
            final Validation.Result<Maybe<T>> result;

            if (value.isValid()) {
                result = value.flatMap(mTo);
                if (result.isInvalid()) {
                    setErrors(mConverter.getErrorMessages(getName(context), context));
                }
            } else {
                result = Validations.<Maybe<T>>safeCast((Validation.Result.Invalid<Maybe<V>>) value);
            }

            return result;
        }

        @Override
        public final void set(@NonNull final Maybe<T> value) {
            mBinding.set(value.map(mFrom));
        }

        @NonNull
        @Override
        public final String getName(@NonNull final Context context) {
            return mBinding.getName(context);
        }

        @Override
        public final void setErrors(@NonNull final List<String> errors) {
            mBinding.setErrors(errors);
        }

        private static class To<V, T> implements Function<Maybe<V>, Validation.Result<Maybe<T>>> {

            @NonNull
            private final Binding.Validated.Converter<V, T> mConverter;

            private To(@NonNull final Binding.Validated.Converter<V, T> converter) {
                super();

                mConverter = converter;
            }

            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public final Validation.Result<Maybe<T>> invoke(@NonNull final Maybe<V> value) {
                final Validation.Result<Maybe<T>> result;

                if (value.isSomething()) {
                    final V v = value.get();
                    if (v == null) {
                        result = (Validation.Result<Maybe<T>>) VALID_SOMETHING_NULL;
                    } else {
                        final Validation.Result<T> mapped = mConverter.to(v);
                        result = mapped.isValid() ?
                                valid(something(mapped.get())) :
                                Validations.<Maybe<T>>safeCast((Validation.Result.Invalid<T>) mapped);
                    }
                } else {
                    result = (Validation.Result<Maybe<T>>) VALID_NOTHING;
                }

                return result;
            }
        }

        private static class From<T, V> implements Function<T, V> {

            @NonNull
            private final Binding.Validated.Converter<V, T> mConverter;

            private From(@NonNull final Binding.Validated.Converter<V, T> converter) {
                super();

                mConverter = converter;
            }

            @NonNull
            @Override
            public final V invoke(@NonNull final T value) {
                return mConverter.from(value);
            }
        }
    }

    private static class Combine<V> extends Binding.ReadWrite.Base<V> {

        @NonNull
        private final Binding.Readable<V> mRead;
        @NonNull
        private final Binding.Writable<V> mWrite;

        private Combine(@NonNull final Binding.Readable<V> read,
                        @NonNull final Binding.Writable<V> write) {
            super();

            mRead = read;
            mWrite = write;
        }

        @NonNull
        @Override
        public final Maybe<V> get() {
            return mRead.get();
        }

        @Override
        public final void set(@NonNull final Maybe<V> value) {
            mWrite.set(value);
        }
    }

    private Bindings() {
        super();
    }
}
