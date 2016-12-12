package com.stephen.convinentsnackbar;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Created by honeywell on 9/12/2016.
 */

public class ConvinentSnackbarManager {

    private static final int MSG_TIMEOUT = 0;

    private static final int SHORT_DURATION_MS = 1500;
    private static final int LONG_DURATION_MS = 2750;

    private static ConvinentSnackbarManager sSnackbarManager;

    static ConvinentSnackbarManager getInstance() {
        if (sSnackbarManager == null) {
            sSnackbarManager = new ConvinentSnackbarManager();
        }
        return sSnackbarManager;
    }

    private final Object mLock;
    private final Handler mHandler;

    private ConvinentSnackbarManager.SnackbarRecord mCurrentSnackbar;
    private ConvinentSnackbarManager.SnackbarRecord mNextSnackbar;

    private ConvinentSnackbarManager() {
        mLock = new Object();
        mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case MSG_TIMEOUT:
                        handleTimeout((ConvinentSnackbarManager.SnackbarRecord) message.obj);
                        return true;
                }
                return false;
            }
        });
    }

    interface Callback {
        void show();
        void dismiss(int event);
    }

    public void show(int duration, ConvinentSnackbarManager.Callback callback) {
        synchronized (mLock) {
            if (isCurrentSnackbarLocked(callback)) {
                // Means that the callback is already in the queue. We'll just update the duration
                mCurrentSnackbar.duration = duration;

                // If this is the ConvinentSnackbar currently being shown, call re-schedule it's
                // timeout
                mHandler.removeCallbacksAndMessages(mCurrentSnackbar);
                scheduleTimeoutLocked(mCurrentSnackbar);
                return;
            } else if (isNextSnackbarLocked(callback)) {
                // We'll just update the duration
                mNextSnackbar.duration = duration;
            } else {
                // Else, we need to create a new record and queue it
                mNextSnackbar = new ConvinentSnackbarManager.SnackbarRecord(duration, callback);
            }

            if (mCurrentSnackbar != null && cancelSnackbarLocked(mCurrentSnackbar,
                    ConvinentSnackbar.Callback.DISMISS_EVENT_CONSECUTIVE)) {
                // If we currently have a ConvinentSnackbar, try and cancel it and wait in line
                return;
            } else {
                // Clear out the current snackbar
                mCurrentSnackbar = null;
                // Otherwise, just show it now
                showNextSnackbarLocked();
            }
        }
    }

    public void dismiss(ConvinentSnackbarManager.Callback callback, int event) {
        synchronized (mLock) {
            if (isCurrentSnackbarLocked(callback)) {
                cancelSnackbarLocked(mCurrentSnackbar, event);
            } else if (isNextSnackbarLocked(callback)) {
                cancelSnackbarLocked(mNextSnackbar, event);
            }
        }
    }

    /**
     * Should be called when a ConvinentSnackbar is no longer displayed. This is after any exit
     * animation has finished.
     */
    public void onDismissed(ConvinentSnackbarManager.Callback callback) {
        synchronized (mLock) {
            if (isCurrentSnackbarLocked(callback)) {
                // If the callback is from a ConvinentSnackbar currently show, remove it and show a new one
                mCurrentSnackbar = null;
                if (mNextSnackbar != null) {
                    showNextSnackbarLocked();
                }
            }
        }
    }

    /**
     * Should be called when a ConvinentSnackbar is being shown. This is after any entrance animation has
     * finished.
     */
    public void onShown(ConvinentSnackbarManager.Callback callback) {
        synchronized (mLock) {
            if (isCurrentSnackbarLocked(callback)) {
                scheduleTimeoutLocked(mCurrentSnackbar);
            }
        }
    }

    public void cancelTimeout(ConvinentSnackbarManager.Callback callback) {
        synchronized (mLock) {
            if (isCurrentSnackbarLocked(callback)) {
                mHandler.removeCallbacksAndMessages(mCurrentSnackbar);
            }
        }
    }

    public void restoreTimeout(ConvinentSnackbarManager.Callback callback) {
        synchronized (mLock) {
            if (isCurrentSnackbarLocked(callback)) {
                scheduleTimeoutLocked(mCurrentSnackbar);
            }
        }
    }

    public boolean isCurrent(ConvinentSnackbarManager.Callback callback) {
        synchronized (mLock) {
            return isCurrentSnackbarLocked(callback);
        }
    }

    public boolean isCurrentOrNext(ConvinentSnackbarManager.Callback callback) {
        synchronized (mLock) {
            return isCurrentSnackbarLocked(callback) || isNextSnackbarLocked(callback);
        }
    }

    private static class SnackbarRecord {
        private final WeakReference<Callback> callback;
        private int duration;

        SnackbarRecord(int duration, ConvinentSnackbarManager.Callback callback) {
            this.callback = new WeakReference<>(callback);
            this.duration = duration;
        }

        boolean isSnackbar(ConvinentSnackbarManager.Callback callback) {
            return callback != null && this.callback.get() == callback;
        }
    }

    private void showNextSnackbarLocked() {
        if (mNextSnackbar != null) {
            mCurrentSnackbar = mNextSnackbar;
            mNextSnackbar = null;

            final ConvinentSnackbarManager.Callback callback = mCurrentSnackbar.callback.get();
            if (callback != null) {
                callback.show();
            } else {
                // The callback doesn't exist any more, clear out the ConvinentSnackbar
                mCurrentSnackbar = null;
            }
        }
    }

    private boolean cancelSnackbarLocked(ConvinentSnackbarManager.SnackbarRecord record, int event) {
        final ConvinentSnackbarManager.Callback callback = record.callback.get();
        if (callback != null) {
            // Make sure we remove any timeouts for the SnackbarRecord
            mHandler.removeCallbacksAndMessages(record);
            callback.dismiss(event);
            return true;
        }
        return false;
    }

    private boolean isCurrentSnackbarLocked(ConvinentSnackbarManager.Callback callback) {
        return mCurrentSnackbar != null && mCurrentSnackbar.isSnackbar(callback);
    }

    private boolean isNextSnackbarLocked(ConvinentSnackbarManager.Callback callback) {
        return mNextSnackbar != null && mNextSnackbar.isSnackbar(callback);
    }

    private void scheduleTimeoutLocked(ConvinentSnackbarManager.SnackbarRecord r) {
        if (r.duration == ConvinentSnackbar.LENGTH_INDEFINITE) {
            // If we're set to indefinite, we don't want to set a timeout
            return;
        }

        int durationMs = LONG_DURATION_MS;
        if (r.duration > 0) {
            durationMs = r.duration;
        } else if (r.duration == ConvinentSnackbar.LENGTH_SHORT) {
            durationMs = SHORT_DURATION_MS;
        }
        mHandler.removeCallbacksAndMessages(r);
        mHandler.sendMessageDelayed(Message.obtain(mHandler, MSG_TIMEOUT, r), durationMs);
    }

    private void handleTimeout(ConvinentSnackbarManager.SnackbarRecord record) {
        synchronized (mLock) {
            if (mCurrentSnackbar == record || mNextSnackbar == record) {
                cancelSnackbarLocked(record, ConvinentSnackbar.Callback.DISMISS_EVENT_TIMEOUT);
            }
        }
    }

}
