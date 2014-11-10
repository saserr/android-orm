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

package android.orm.tasks.view;

import android.app.Activity;
import android.orm.tasks.R;
import android.orm.tasks.model.Task;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Checkable;
import android.widget.ListView;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static android.widget.AbsListView.CHOICE_MODE_NONE;

public class List extends ListFragment {

    private Controller mController = DUMMY_CONTROLLER;

    private ArrayAdapter<Task> mAdapter;

    private final AdapterView.OnItemLongClickListener mEdit = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(final AdapterView<?> adapterView,
                                       final View view,
                                       final int position,
                                       final long id) {
            mController.edit(mAdapter.getItem(position));
            return true;
        }
    };

    public static List create() {
        return new List();
    }

    public List() {
        super();
    }

    @Override
    public final void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ListView list = getListView();
        if (list != null) {
            list.setChoiceMode(CHOICE_MODE_NONE);
            list.setOnItemLongClickListener(mEdit);
        }

        setListShown(false);
    }

    @Override
    public final void onListItemClick(final ListView list,
                                      final View view,
                                      final int position,
                                      final long id) {
        super.onListItemClick(list, view, position, id);

        final Task task = mAdapter.getItem(position);
        if (((Checkable) view).isChecked()) {
            mController.open(task);
        } else {
            mController.close(task);
        }
    }

    public final void show(final Collection<Task> tasks) {
        mAdapter.setNotifyOnChange(false);
        mAdapter.clear();
        for (final Task task : tasks) {
            mAdapter.add(task);
        }
        mAdapter.notifyDataSetChanged();

        setListShown(true);
    }

    @Override
    public final void onAttach(final Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Controller)) {
            throw new IllegalStateException("Activity must implement TasksList's controller.");
        }
        mController = (Controller) activity;
        mAdapter = new Adapter(activity);
        setListAdapter(mAdapter);
    }

    @Override
    public final void onDetach() {
        setListAdapter(null);
        mAdapter = null;
        mController = DUMMY_CONTROLLER;
        super.onDetach();
    }

    public interface Controller {

        void edit(final Task task);

        void open(final Task task);

        void close(final Task task);
    }

    private static final Controller DUMMY_CONTROLLER = new Controller() {

        @Override
        public void edit(final Task task) {/* do nothing */}

        @Override
        public void open(final Task task) {/* do nothing */}

        @Override
        public void close(final Task task) {/* do nothing */}
    };

    private static class Adapter extends ArrayAdapter<Task> {

        private Adapter(final Activity activity) {
            super(activity, R.layout.list_item);
        }

        @Override
        public final View getView(final int position,
                                  final View convertView,
                                  @NotNull final ViewGroup parent) {
            final ListItem view = (ListItem) super.getView(position, convertView, parent);

            if (view != null) {
                final Task task = getItem(position);
                view.setText(task.getTitle());
                view.setChecked(task.isFinished());
            }

            return view;
        }
    }
}
