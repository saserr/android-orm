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

package android.orm.sql;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.orm.sql.fragment.Limit;
import android.orm.sql.fragment.Offset;
import android.orm.util.Legacy;
import android.orm.util.Maybe;
import android.orm.util.Maybes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.HashSet;
import java.util.Set;

import static android.orm.sql.Helper.escape;
import static android.orm.util.Maybes.something;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

public final class Readables {

    @NonNull
    public static Readable readable(@NonNull final Cursor cursor) {
        return new CursorReadable(cursor);
    }

    @NonNull
    public static Readable readable(@NonNull final ContentValues values) {
        return new ContentValuesReadable(values);
    }

    @NonNull
    public static Readable limit(@NonNull final Readable readable,
                                 @Nullable final Limit limit,
                                 @Nullable final Offset offset) {
        return new LimitedReadable(readable, limit, offset);
    }

    @NonNull
    public static Readable combine(@NonNull final Readable first,
                                   @NonNull final Readable second) {
        return new Composition(first, second);
    }

    private static class CursorReadable implements Readable {

        @NonNull
        private final Cursor mCursor;

        private CursorReadable(@NonNull final Cursor cursor) {
            super();

            mCursor = cursor;
        }

        @NonNull
        @Override
        public final Maybe<String> getAsString(@NonNull final String key) {
            final int index = mCursor.getColumnIndex(key);
            return (index < 0) ?
                    Maybes.<String>nothing() :
                    something(mCursor.isNull(index) ? null : mCursor.getString(index));
        }

        @NonNull
        @Override
        public final Maybe<Long> getAsLong(@NonNull final String key) {
            final int index = mCursor.getColumnIndex(key);
            return (index < 0) ?
                    Maybes.<Long>nothing() :
                    something(mCursor.isNull(index) ? null : mCursor.getLong(index));
        }

        @NonNull
        @Override
        public final Maybe<Double> getAsDouble(@NonNull final String key) {
            final int index = mCursor.getColumnIndex(key);
            return (index < 0) ?
                    Maybes.<Double>nothing() :
                    something(mCursor.isNull(index) ? null : mCursor.getDouble(index));
        }

        @NonNull
        @Override
        public final Set<String> getKeys() {
            return new HashSet<>(asList(mCursor.getColumnNames()));
        }

        @Override
        public final int position() {
            return mCursor.getPosition();
        }

        @Override
        public final int size() {
            return mCursor.getCount();
        }

        @Override
        public final boolean start() {
            return mCursor.moveToFirst();
        }

        @Override
        public final boolean start(final int position) {
            return (position >= 0) &&
                    (position < mCursor.getCount()) &&
                    mCursor.moveToPosition(position);
        }

        @Override
        public final boolean next() {
            return mCursor.moveToNext();
        }

        @Override
        public final void close() {
            mCursor.close();
        }
    }

    private static class ContentValuesReadable implements Readable {

        @NonNull
        private final ContentValues mValues;

        private ContentValuesReadable(@NonNull final ContentValues values) {
            super();

            mValues = values;
        }

        @NonNull
        @Override
        public final Maybe<String> getAsString(@NonNull final String key) {
            final String escaped = escape(key);
            final boolean contains = mValues.containsKey(escaped);
            return contains ?
                    something(mValues.getAsString(escaped)) :
                    Maybes.<String>nothing();
        }

        @NonNull
        @Override
        public final Maybe<Long> getAsLong(@NonNull final String key) {
            final String escaped = escape(key);
            final boolean contains = mValues.containsKey(escaped);
            return contains ?
                    something(mValues.getAsLong(escaped)) :
                    Maybes.<Long>nothing();
        }

        @NonNull
        @Override
        public final Maybe<Double> getAsDouble(@NonNull final String key) {
            final String escaped = escape(key);
            final boolean contains = mValues.containsKey(escaped);
            return contains ?
                    something(mValues.getAsDouble(escaped)) :
                    Maybes.<Double>nothing();
        }

        @NonNull
        @Override
        public final Set<String> getKeys() {
            return Legacy.getKeys(mValues);
        }

        @Override
        public final int position() {
            return 0;
        }

        @Override
        public final int size() {
            return 1;
        }

        @Override
        public final boolean start() {
            return true;
        }

        @Override
        public final boolean start(final int position) {
            return position == 0;
        }

        @Override
        public final boolean next() {
            return false;
        }

        @Override
        public final void close() {/* do nothing */}
    }

    private static class LimitedReadable implements Readable {

        @NonNull
        private final Readable mReadable;
        private final int mLimit;
        private final int mOffset;

        private LimitedReadable(@NonNull final Readable readable,
                                @Nullable final Limit limit,
                                @Nullable final Offset offset) {
            super();

            mReadable = readable;
            mLimit = (limit == null) ? Integer.MAX_VALUE : limit.getAmount();
            mOffset = (offset == null) ? 0 : offset.getAmount();
        }

        @NonNull
        @Override
        public final Maybe<String> getAsString(@NonNull final String key) {
            return mReadable.getAsString(key);
        }

        @NonNull
        @Override
        public final Maybe<Long> getAsLong(@NonNull final String key) {
            return mReadable.getAsLong(key);
        }

        @NonNull
        @Override
        public final Maybe<Double> getAsDouble(@NonNull final String key) {
            return mReadable.getAsDouble(key);
        }

        @NonNull
        @Override
        public final Set<String> getKeys() {
            return mReadable.getKeys();
        }

        @Override
        public final int position() {
            final int current = mReadable.position() - mOffset;
            return max(-1, min(current, mLimit));
        }

        @Override
        public final int size() {
            final int size = mReadable.size() - mOffset;
            return max(0, min(size, mLimit));
        }

        @Override
        public final boolean start() {
            return mReadable.start(mOffset);
        }

        @Override
        public final boolean start(final int position) {
            return mReadable.start(mOffset + position);
        }

        @Override
        public final boolean next() {
            final int position = position();
            return (position < 0) ? start() : ((position < mLimit) && mReadable.next());
        }

        @Override
        public final void close() {
            mReadable.close();
        }
    }

    private static class Composition implements Readable {

        @NonNull
        private final Readable mFirst;
        @NonNull
        private final Readable mSecond;

        private Composition(@NonNull final Readable first, @NonNull final Readable second) {
            super();

            mFirst = first;
            mSecond = second;
        }

        @NonNull
        @Override
        public final Maybe<String> getAsString(@NonNull final String key) {
            Maybe<String> result = mFirst.getAsString(key);
            if (result.isNothing()) {
                result = mSecond.getAsString(key);
            }
            return result;
        }

        @NonNull
        @Override
        public final Maybe<Long> getAsLong(@NonNull final String key) {
            Maybe<Long> result = mFirst.getAsLong(key);
            if (result.isNothing()) {
                result = mSecond.getAsLong(key);
            }
            return result;
        }

        @NonNull
        @Override
        public final Maybe<Double> getAsDouble(@NonNull final String key) {
            Maybe<Double> result = mFirst.getAsDouble(key);
            if (result.isNothing()) {
                result = mSecond.getAsDouble(key);
            }
            return result;
        }

        @NonNull
        @Override
        public final Set<String> getKeys() {
            final Set<String> keys1 = mFirst.getKeys();
            final Set<String> keys2 = mSecond.getKeys();
            final Set<String> keys = new HashSet<>(keys1.size() + keys2.size());
            keys.addAll(keys1);
            keys.addAll(keys2);
            return unmodifiableSet(keys);
        }

        @Override
        public final int position() {
            @NonNls final int position1 = mFirst.position();
            @NonNls final int position2 = mSecond.position();
            final int result;

            if ((position1 < 0) || (position2 < 0)) {
                result = -1;
            } else if ((position1 >= mFirst.size()) || (position2 >= mSecond.size())) {
                result = size();
            } else {
                if (position1 != position2) {
                    throw new SQLException("Different positions of two readables: " + position1 + " vs. " + position2);
                }

                result = position1;
            }

            return result;
        }

        @Override
        public final int size() {
            return min(mFirst.size(), mSecond.size());
        }

        @Override
        public final boolean start() {
            return mFirst.start() && mSecond.start();
        }

        @Override
        public final boolean start(final int position) {
            return mFirst.start(position) && mSecond.start(position);
        }

        @Override
        public final boolean next() {
            return mFirst.next() && mSecond.next();
        }

        @Override
        public final void close() {
            mFirst.close();
            mSecond.close();
        }
    }

    private Readables() {
        super();
    }
}
