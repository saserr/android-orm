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

package android.orm.remote.dao;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.net.Uri;
import android.orm.Model;
import android.orm.model.Instance;
import android.orm.model.Mapper;
import android.orm.remote.Route;
import android.orm.sql.Value;
import android.orm.sql.Writer;
import android.orm.sql.fragment.Predicate;
import android.orm.util.Producer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import static android.orm.sql.Value.Write.Operation.Insert;
import static android.orm.sql.Value.Write.Operation.Update;
import static android.orm.sql.Writables.writable;
import static android.orm.util.Maybes.something;

public abstract class Transaction<R> {

    @NonNls
    @Nullable
    private String mAuthority;
    @NonNull
    private final Collection<Producer<ContentProviderOperation>> mBatch = new LinkedList<>();

    protected Transaction() {
        super();
    }

    @NonNull
    protected abstract R commit(@NonNls @Nullable final String authority,
                                @NonNull final Collection<Producer<ContentProviderOperation>> batch);

    @NonNull
    public final Access at(@NonNull final Route route, @NonNull final Object... arguments) {
        return new Access(route, arguments);
    }

    @NonNull
    public final R commit() {
        final R result = commit(mAuthority, new ArrayList<>(mBatch));
        mAuthority = null;
        mBatch.clear();
        return result;
    }

    public interface CommitResult {

        @NonNls
        @NonNull
        String getAuthority();

        @NonNull
        ContentProviderResult[] getResults();
    }

    public class Access implements android.orm.Access.Insert<Access>, android.orm.Access.Update<Access>, android.orm.Access.Delete<Access> {

        @NonNull
        private final Uri mUri;

        private Access(@NonNull final Route route, @NonNull final Object... arguments) {
            super();

            mUri = route.createUri(arguments);
            final String authority = mUri.getAuthority();
            if ((mAuthority != null) && !mAuthority.equals(authority)) {
                throw new IllegalArgumentException(
                        "Different authority " + authority + "! Expected " + mAuthority +
                                ". Transaction between different authorities is not supported, because underlying Android API doesn't support it."
                );
            }

            if (mAuthority == null) {
                mAuthority = authority;
            }
        }

        @NonNull
        @Override
        public final Access insert(@NonNull final Model model) {
            return insert(Model.toInstance(model));
        }

        @NonNull
        @Override
        public final Access insert(@NonNull final Instance.Writable model) {
            return insert(model.prepareWriter());
        }

        @NonNull
        @Override
        public final Access insert(@NonNull final Writer writer) {
            mBatch.add(new Insert(mUri, writer));
            return this;
        }

        @NonNull
        @Override
        public final <M> Access insert(@Nullable final M model,
                                       @NonNull final Value.Write<M> value) {
            return insert(value.write(model));
        }

        @NonNull
        @Override
        public final <M> Access insert(@Nullable final M model,
                                       @NonNull final Mapper.Write<M> mapper) {
            return insert(mapper.prepareWriter(something(model)));
        }

        @NonNull
        @Override
        public final Access update(@NonNull final Model model) {
            return update(Model.toInstance(model));
        }

        @NonNull
        @Override
        public final Access update(@NonNull final Predicate predicate, @NonNull final Model model) {
            return update(predicate, Model.toInstance(model));
        }

        @NonNull
        @Override
        public final Access update(@NonNull final Instance.Writable model) {
            return update(model.prepareWriter());
        }

        @NonNull
        @Override
        public final Access update(@NonNull final Predicate predicate,
                                   @NonNull final Instance.Writable model) {
            return update(predicate, model.prepareWriter());
        }

        @NonNull
        @Override
        public final Access update(@NonNull final Writer writer) {
            return update(Predicate.None, writer);
        }

        @NonNull
        @Override
        public final Access update(@NonNull final Predicate predicate,
                                   @NonNull final Writer writer) {
            mBatch.add(new Update(mUri, writer, predicate));
            return this;
        }

        @NonNull
        @Override
        public final <M> Access update(@Nullable final M model,
                                       @NonNull final Value.Write<M> value) {
            return update(value.write(model));
        }

        @NonNull
        @Override
        public final <M> Access update(@NonNull final Predicate predicate,
                                       @Nullable final M model,
                                       @NonNull final Value.Write<M> value) {
            return update(predicate, value.write(model));
        }

        @NonNull
        @Override
        public final <M> Access update(@Nullable final M model,
                                       @NonNull final Mapper.Write<M> mapper) {
            return update(mapper.prepareWriter(something(model)));
        }

        @NonNull
        @Override
        public final <M> Access update(@NonNull final Predicate predicate,
                                       @Nullable final M model,
                                       @NonNull final Mapper.Write<M> mapper) {
            return update(predicate, mapper.prepareWriter(something(model)));
        }

        @NonNull
        @Override
        public final Access delete() {
            return delete(Predicate.None);
        }

        @NonNull
        @Override
        public final Access delete(@NonNull final Predicate predicate) {
            mBatch.add(new Delete(mUri, predicate));
            return this;
        }
    }

    private static class Insert implements Producer<ContentProviderOperation> {

        @NonNull
        private final Uri mUri;
        @NonNull
        private final Writer mWriter;

        private Insert(@NonNull final Uri uri, @NonNull final Writer writer) {
            super();

            mUri = uri;
            mWriter = writer;
        }

        @NonNull
        @Override
        public final ContentProviderOperation produce() {
            final ContentValues values = new ContentValues();
            mWriter.write(Insert, writable(values));
            return ContentProviderOperation.newInsert(mUri).withValues(values).build();
        }
    }

    private static class Update implements Producer<ContentProviderOperation> {

        @NonNull
        private final Uri mUri;
        @NonNull
        private final Writer mWriter;
        @NonNull
        private final Predicate mPredicate;

        private Update(@NonNull final Uri uri,
                       @NonNull final Writer writer,
                       @NonNull final Predicate predicate) {
            super();

            mUri = uri;
            mWriter = writer;
            mPredicate = predicate;
        }

        @NonNull
        @Override
        public final ContentProviderOperation produce() {
            final ContentValues values = new ContentValues();
            mWriter.write(Update, writable(values));
            return ContentProviderOperation.newUpdate(mUri)
                    .withSelection(mPredicate.toSQL(), null)
                    .withValues(values)
                    .build();
        }
    }

    private static class Delete implements Producer<ContentProviderOperation> {

        @NonNull
        private final Uri mUri;
        @NonNull
        private final Predicate mPredicate;

        private Delete(@NonNull final Uri uri, @NonNull final Predicate predicate) {
            super();

            mUri = uri;
            mPredicate = predicate;
        }

        @NonNull
        @Override
        public final ContentProviderOperation produce() {
            return ContentProviderOperation.newDelete(mUri)
                    .withSelection(mPredicate.toSQL(), null)
                    .build();
        }
    }
}
