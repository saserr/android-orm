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

package android.orm.model;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.orm.util.Function;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import static android.orm.util.Maybes.something;
import static android.text.TextUtils.isEmpty;
import static java.lang.Math.min;

public final class Bindings {

    @NonNull
    public static <V> Binding.ReadWrite<V> value(@NonNull final AdapterView<? extends Adapter> adapter) {
        return new AdapterBinding<>(adapter);
    }

    @NonNull
    public static Binding.ReadWrite<String> text(@NonNull final TextView text) {
        return new TextBinding(text);
    }

    @NonNull
    public static Binding.Write<Uri> uri(@NonNull final ImageView image) {
        return new ImageUriBinding(image);
    }

    @NonNull
    public static Binding.Write<Bitmap> bitmap(@NonNull final ImageView image) {
        return new ImageBitmapBinding(image);
    }

    @NonNull
    public static Binding.ReadWrite<Drawable> drawable(@NonNull final ImageView image) {
        return new ImageDrawableBinding(image);
    }

    @NonNull
    public static <V, T> Binding.Read<Pair<V, T>> compose(@NonNull final Binding.Read<V> first,
                                                          @NonNull final Binding.Read<T> second) {
        return new ReadComposition<>(first, second);
    }

    @NonNull
    public static <V, T> Binding.Write<Pair<V, T>> compose(@NonNull final Binding.Write<V> first,
                                                           @NonNull final Binding.Write<T> second) {
        return new WriteComposition<>(first, second);
    }

    @NonNull
    public static <V, T> Binding.Read<T> convert(@NonNull final Binding.Read<V> binding,
                                                 @NonNull final Function<Maybe<V>, Maybe<T>> converter) {
        return new ReadConversion<>(binding, converter);
    }

    @NonNull
    public static <V, T> Binding.Write<T> convert(@NonNull final Binding.Write<V> binding,
                                                  @NonNull final Function<Maybe<T>, Maybe<V>> converter) {
        return new WriteConversion<>(binding, converter);
    }

    @NonNull
    public static <V> Binding.ReadWrite<V> combine(@NonNull final Binding.Read<V> read,
                                                   @NonNull final Binding.Write<V> write) {
        return new Combination<>(read, write);
    }

    private static class AdapterBinding<V> extends Binding.ReadWrite.Base<V> {

        @NonNull
        private final AdapterView<? extends Adapter> mAdapter;

        private AdapterBinding(@NonNull final AdapterView<? extends Adapter> adapter) {
            super();

            mAdapter = adapter;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public final Maybe<V> get() {
            final V item = (V) mAdapter.getSelectedItem();
            return (item == null) ? Maybes.<V>nothing() : something(item);
        }

        @Override
        public final void set(@NonNull final Maybe<V> value) {
            int position = -1;

            if (value.isSomething()) {
                final V v = value.get();
                if (v != null) {
                    final int count = mAdapter.getCount();
                    for (int i = 0; (i < count) && (position < 0); i++) {
                        if (v.equals(mAdapter.getItemAtPosition(i))) {
                            position = i;
                        }
                    }
                }
            }

            mAdapter.setSelection(min(position, 0));
        }
    }

    private static class TextBinding extends Binding.ReadWrite.Base<String> {

        @NonNull
        private final TextView mText;

        private TextBinding(@NonNull final TextView text) {
            super();

            mText = text;
        }

        @NonNull
        @Override
        public final Maybe<String> get() {
            final CharSequence chars = mText.getText();
            return isEmpty(chars) ? Maybes.<String>nothing() : something(chars.toString());
        }

        @Override
        public final void set(@NonNull final Maybe<String> value) {
            if (value.isSomething()) {
                mText.setText(value.get());
            }
        }
    }

    private static class ImageUriBinding extends Binding.Write.Base<Uri> {

        @NonNull
        private final ImageView mImage;

        private ImageUriBinding(@NonNull final ImageView image) {
            super();

            mImage = image;
        }

        @Override
        public final void set(@NonNull final Maybe<Uri> value) {
            if (value.isSomething()) {
                final Uri uri = value.get();
                if (uri == null) {
                    mImage.setImageDrawable(null);
                } else {
                    mImage.setImageURI(uri);
                }
            }
        }
    }

    private static class ImageBitmapBinding extends Binding.Write.Base<Bitmap> {

        @NonNull
        private final ImageView mImage;

        private ImageBitmapBinding(@NonNull final ImageView image) {
            super();

            mImage = image;
        }

        @Override
        public final void set(@NonNull final Maybe<Bitmap> value) {
            if (value.isSomething()) {
                final Bitmap bitmap = value.get();
                if (bitmap == null) {
                    mImage.setImageDrawable(null);
                } else {
                    mImage.setImageBitmap(bitmap);
                }
            }
        }
    }

    private static class ImageDrawableBinding extends Binding.ReadWrite.Base<Drawable> {

        @NonNull
        private final ImageView mImage;

        private ImageDrawableBinding(@NonNull final ImageView image) {
            super();

            mImage = image;
        }

        @NonNull
        @Override
        public final Maybe<Drawable> get() {
            final Drawable drawable = mImage.getDrawable();
            return (drawable == null) ? Maybes.<Drawable>nothing() : something(drawable);
        }

        @Override
        public final void set(@NonNull final Maybe<Drawable> value) {
            if (value.isSomething()) {
                mImage.setImageDrawable(value.get());
            }
        }
    }

    private static class ReadComposition<V, T> extends Binding.Read.Base<Pair<V, T>> {

        @NonNull
        private final Binding.Read<V> mFirst;
        @NonNull
        private final Binding.Read<T> mSecond;
        @NonNull
        private final Function<Pair<Maybe<V>, Maybe<T>>, Maybe<Pair<V, T>>> mLift;

        private ReadComposition(@NonNull final Binding.Read<V> first,
                                @NonNull final Binding.Read<T> second) {
            super();

            mFirst = first;
            mSecond = second;
            mLift = Maybes.liftPair();
        }

        @NonNull
        @Override
        public final Maybe<Pair<V, T>> get() {
            return mLift.invoke(Pair.create(mFirst.get(), mSecond.get()));
        }
    }

    private static class WriteComposition<V, T> extends Binding.Write.Base<Pair<V, T>> {

        @NonNull
        private final Binding.Write<V> mFirst;
        @NonNull
        private final Binding.Write<T> mSecond;

        private WriteComposition(@NonNull final Binding.Write<V> first,
                                 @NonNull final Binding.Write<T> second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @Override
        public final void set(@NonNull final Maybe<Pair<V, T>> value) {
            if (value.isSomething()) {
                final Pair<V, T> pair = value.get();
                mFirst.set(something((pair == null) ? null : pair.first));
                mSecond.set(something((pair == null) ? null : pair.second));
            } else {
                mFirst.set(Maybes.<V>nothing());
                mSecond.set(Maybes.<T>nothing());
            }
        }
    }

    private static class ReadConversion<V, T> extends Binding.Read.Base<T> {

        @NonNull
        private final Binding.Read<V> mBinding;
        @NonNull
        private final Function<Maybe<V>, Maybe<T>> mConverter;

        private ReadConversion(@NonNull final Binding.Read<V> binding,
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

    private static class WriteConversion<V, T> extends Binding.Write.Base<T> {

        @NonNull
        private final Binding.Write<V> mBinding;
        @NonNull
        private final Function<Maybe<T>, Maybe<V>> mConverter;

        private WriteConversion(@NonNull final Binding.Write<V> binding,
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

    private static class Combination<V> extends Binding.ReadWrite.Base<V> {

        @NonNull
        private final Binding.Read<V> mRead;
        @NonNull
        private final Binding.Write<V> mWrite;

        private Combination(@NonNull final Binding.Read<V> read,
                            @NonNull final Binding.Write<V> write) {
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
