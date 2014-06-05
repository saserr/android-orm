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

import android.app.Activity;
import android.orm.DAO;
import android.orm.access.ErrorHandler;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.NonNls;

public abstract class ListFragment<M> extends android.app.ListFragment {

    @NonNull
    private final ArrayAdapter.Factory<M> mAdapterFactory;
    @Nullable
    private final ErrorHandler.Factory<Activity> mErrorHandlerFactory;
    @Nullable
    private DAO mDAO;
    @Nullable
    private ArrayAdapter<M> mAdapter;

    protected ListFragment(@NonNull final ArrayAdapter.Factory<M> adapterFactory) {
        this(adapterFactory, null);
    }

    protected ListFragment(@NonNull final ArrayAdapter.Factory<M> adapterFactory,
                           @Nullable final ErrorHandler.Factory<Activity> errorHandlerFactory) {
        super();

        mErrorHandlerFactory = errorHandlerFactory;
        mAdapterFactory = adapterFactory;
    }

    @Override
    public final void onAttach(@NonNull final Activity activity) {
        super.onAttach(activity);

        mDAO = new DAO(activity.getBaseContext());
        if (mErrorHandlerFactory != null) {
            mDAO.setErrorHandler(mErrorHandlerFactory.create(activity));
        }

        mAdapter = mAdapterFactory.create(activity, mDAO);
        afterAttach(activity);
    }

    @Override
    public final void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setListAdapter(mAdapter);
        afterCreate(savedInstanceState);
    }

    @Override
    public final void onResume() {
        super.onResume();
        assert mDAO != null;
        mDAO.start();
        afterResume();
    }

    @Override
    public final void onPause() {
        beforePause();
        assert mDAO != null;
        mDAO.pause();
        super.onPause();
    }

    @Override
    public final void onDestroy() {
        beforeDestroy();
        assert mDAO != null;
        mDAO.stop();
        mAdapter = null;
        mDAO = null;
        super.onDestroy();
    }

    @NonNull
    protected final M getItem(final int position) {
        return getAdapter().getItem(position);
    }

    @NonNull
    protected final ArrayAdapter<M> getAdapter() {
        if (mAdapter == null) {
            throw new UnsupportedOperationException("You are accessing ArrayAdapter too early or to late");
        }
        return mAdapter;
    }

    @NonNull
    protected final DAO getDAO() {
        if (mDAO == null) {
            throw new UnsupportedOperationException("You are accessing DAO too early or to late");
        }
        return mDAO;
    }

    @NonNull
    protected final Form.Builder form(@NonNls @NonNull final String name) {
        return Form.builder(name);
    }

    protected void afterAttach(@NonNull final Activity activity) {
    }

    protected void afterCreate(@Nullable final Bundle savedInstanceState) {
    }

    protected void afterResume() {
    }

    protected void beforePause() {
    }

    protected void beforeDestroy() {
    }
}
