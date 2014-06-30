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

import java.util.Arrays;

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

    @NonNull
    public static Read read(@NonNull final Read... observers) {
        return read(Arrays.asList(observers));
    }

    @NonNull
    public static Read read(@NonNull final Iterable<Read> observers) {
        return new Read() {

            @Override
            public void beforeRead() {
                for (final Read observer : observers) {
                    observer.beforeRead();
                }
            }

            @Override
            public void afterRead() {
                for (final Read observer : observers) {
                    observer.afterRead();
                }
            }
        };
    }

    @NonNull
    public static Write write(@NonNull final Write... observers) {
        return write(Arrays.asList(observers));
    }

    @NonNull
    public static Write write(@NonNull final Iterable<Write> observers) {
        return new Write() {

            @Override
            public void beforeCreate() {
                for (final Write observer : observers) {
                    observer.beforeCreate();
                }
            }

            @Override
            public void afterCreate() {
                for (final Write observer : observers) {
                    observer.afterCreate();
                }
            }

            @Override
            public void beforeUpdate() {
                for (final Write observer : observers) {
                    observer.beforeUpdate();
                }
            }

            @Override
            public void afterUpdate() {
                for (final Write observer : observers) {
                    observer.afterUpdate();
                }
            }

            @Override
            public void beforeSave() {
                for (final Write observer : observers) {
                    observer.beforeSave();
                }
            }

            @Override
            public void afterSave() {
                for (final Write observer : observers) {
                    observer.afterSave();
                }
            }
        };
    }

    @NonNull
    public static ReadWrite combine(@NonNull final Read read, @NonNull final Write write) {
        return new ReadWrite() {

            @Override
            public void beforeRead() {
                read.beforeRead();
            }

            @Override
            public void afterRead() {
                read.afterRead();
            }

            @Override
            public void beforeCreate() {
                write.beforeCreate();
            }

            @Override
            public void afterCreate() {
                write.afterCreate();
            }

            @Override
            public void beforeUpdate() {
                write.beforeUpdate();
            }

            @Override
            public void afterUpdate() {
                write.afterUpdate();
            }

            @Override
            public void beforeSave() {
                write.beforeSave();
            }

            @Override
            public void afterSave() {
                write.afterSave();
            }
        };
    }

    private Observer() {
        super();
    }
}
