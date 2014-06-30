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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public final class Observer {

    public interface Read {

        void beforeRead();

        void afterRead();

        Read DUMMY = new Base() {
            /* ignore everything */
        };

        abstract class Base implements Read {

            @Override
            public void beforeRead() {/* do nothing */}

            @Override
            public void afterRead() {/* do nothing */}
        }

        class Delegate implements Read {

            @NonNull
            private final Collection<Read> mObservers;

            public Delegate(@NonNull final Read... observers) {
                super();

                mObservers = Arrays.asList(observers);
            }

            public Delegate(@NonNull final Collection<Read> observers) {
                super();

                mObservers = new ArrayList<>(observers);
            }

            @Override
            public final void beforeRead() {
                for (final Read observer : mObservers) {
                    observer.beforeRead();
                }
            }

            @Override
            public final void afterRead() {
                for (final Read observer : mObservers) {
                    observer.afterRead();
                }
            }
        }
    }

    public interface Write {

        void beforeCreate();

        void afterCreate();

        void beforeUpdate();

        void afterUpdate();

        void beforeSave();

        void afterSave();

        Write DUMMY = new Base() {
            /* ignore everything */
        };

        abstract class Base implements Write {

            @Override
            public void beforeCreate() {/* do nothing */}

            @Override
            public void afterCreate() {/* do nothing */}

            @Override
            public void beforeUpdate() {/* do nothing */}

            @Override
            public void afterUpdate() {/* do nothing */}

            @Override
            public void beforeSave() {/* do nothing */}

            @Override
            public void afterSave() {/* do nothing */}
        }

        class Delegate implements Write {

            @NonNull
            private final Collection<Write> mObservers;

            public Delegate(@NonNull final Write... observers) {
                super();

                mObservers = Arrays.asList(observers);
            }

            public Delegate(@NonNull final Collection<Write> observers) {
                super();

                mObservers = new ArrayList<>(observers);
            }

            @Override
            public final void beforeCreate() {
                for (final Write observer : mObservers) {
                    observer.beforeCreate();
                }
            }

            @Override
            public final void afterCreate() {
                for (final Write observer : mObservers) {
                    observer.afterCreate();
                }
            }

            @Override
            public final void beforeUpdate() {
                for (final Write observer : mObservers) {
                    observer.beforeUpdate();
                }
            }

            @Override
            public final void afterUpdate() {
                for (final Write observer : mObservers) {
                    observer.afterUpdate();
                }
            }

            @Override
            public final void beforeSave() {
                for (final Write observer : mObservers) {
                    observer.beforeSave();
                }
            }

            @Override
            public final void afterSave() {
                for (final Write observer : mObservers) {
                    observer.afterSave();
                }
            }
        }
    }

    public interface ReadWrite extends Read, Write {

        ReadWrite DUMMY = new Base() {
            /* ignore everything */
        };

        abstract class Base implements ReadWrite {

            @Override
            public void beforeRead() {/* do nothing */}

            @Override
            public void afterRead() {/* do nothing */}

            @Override
            public void beforeCreate() {/* do nothing */}

            @Override
            public void afterCreate() {/* do nothing */}

            @Override
            public void beforeUpdate() {/* do nothing */}

            @Override
            public void afterUpdate() {/* do nothing */}

            @Override
            public void beforeSave() {/* do nothing */}

            @Override
            public void afterSave() {/* do nothing */}
        }

        abstract class Composition implements ReadWrite {

            @NonNull
            private final Read mRead;
            @NonNull
            private final Write mWrite;

            protected Composition(@NonNull final Read read,
                                  @NonNull final Write write) {
                super();

                mRead = read;
                mWrite = write;
            }

            @Override
            public final void beforeRead() {
                mRead.beforeRead();
            }

            @Override
            public final void afterRead() {
                mRead.afterRead();
            }

            @Override
            public final void beforeCreate() {
                mWrite.beforeCreate();
            }

            @Override
            public final void afterCreate() {
                mWrite.afterCreate();
            }

            @Override
            public final void beforeUpdate() {
                mWrite.beforeUpdate();
            }

            @Override
            public final void afterUpdate() {
                mWrite.afterUpdate();
            }

            @Override
            public final void beforeSave() {
                mWrite.beforeSave();
            }

            @Override
            public final void afterSave() {
                mWrite.afterSave();
            }
        }
    }

    public static <M> void beforeRead(@Nullable final M model) {
        if (model instanceof Read) {
            ((Read) model).beforeRead();
        }
    }

    public static <M> void afterRead(@Nullable final M model) {
        if (model instanceof Read) {
            ((Read) model).afterRead();
        }
    }

    public static <M> void beforeCreate(@Nullable final M model) {
        if (model instanceof Write) {
            ((Write) model).beforeCreate();
            ((Write) model).beforeSave();
        }
    }

    public static <M> void afterCreate(@Nullable final M model) {
        if (model instanceof Write) {
            ((Write) model).afterSave();
            ((Write) model).afterCreate();
        }
    }

    public static <M> void beforeUpdate(@Nullable final M model) {
        if (model instanceof Write) {
            ((Write) model).beforeUpdate();
            ((Write) model).beforeSave();
        }
    }

    public static <M> void afterUpdate(@Nullable final M model) {
        if (model instanceof Write) {
            ((Write) model).afterSave();
            ((Write) model).afterUpdate();
        }
    }

    private Observer() {
        super();
    }
}
