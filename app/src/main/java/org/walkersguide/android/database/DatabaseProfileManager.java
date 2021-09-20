package org.walkersguide.android.database;

import org.walkersguide.android.database.DatabaseProfileRequest;
import org.walkersguide.android.database.util.AccessDatabase;

import org.walkersguide.android.data.ObjectWithId;

import android.os.AsyncTask;



import java.util.ArrayList;



import org.walkersguide.android.util.Constants;


public class DatabaseProfileManager {


    /**
     * singleton
     */

    private static DatabaseProfileManager managerInstance;

    public static DatabaseProfileManager getInstance() {
        if (managerInstance == null){
            managerInstance = getInstanceSynchronized();
        }
        return managerInstance;
    }

    private static synchronized DatabaseProfileManager getInstanceSynchronized() {
        if (managerInstance == null){
            managerInstance = new DatabaseProfileManager();
        }
        return managerInstance;
    }

    private DatabaseProfileManager() {
    }


    /**
     * profile request
     */

    public interface DatabaseProfileRequestListener {
        public void databaseProfileRequestSuccessful(ArrayList<ObjectWithId> objectList);
        public void databaseProfileRequestFailed(int returnCode);
    }

    private DatabaseProfileTask databaseTask = null;

    public void startDatabaseProfileRequest(
            DatabaseProfileRequestListener listener, DatabaseProfileRequest request) {
        if (databaseProfileRequestInProgress()) {
            if (listener == null) {
                return;
            } else if (databaseTask.getDatabaseProfileRequest().equals(request)) {
                databaseTask.addListener(listener);
                return;
            } else {
                cancelDatabaseProfileRequest();
            }
        }
        databaseTask = new DatabaseProfileTask(listener, request);
        databaseTask.execute();
    }

    public void invalidateDatabaseProfileRequest(DatabaseProfileRequestListener listener) {
        if (databaseProfileRequestInProgress()) {
            databaseTask.removeListener(listener);
        }
    }

    public boolean databaseProfileRequestInProgress() {
        if (databaseTask != null
                && databaseTask.getStatus() != AsyncTask.Status.FINISHED) {
            return true;
        }
        return false;
    }

    public void cancelDatabaseProfileRequest() {
        if (databaseProfileRequestInProgress()) {
            databaseTask.cancel();
        }
    }


    private static class DatabaseProfileTask extends AsyncTask<Void, Void, ArrayList<ObjectWithId>> {

        private ArrayList<DatabaseProfileRequestListener> listenerList;
        private DatabaseProfileRequest request;

        public DatabaseProfileTask(
                DatabaseProfileRequestListener listener, DatabaseProfileRequest request) {
            this.listenerList = new ArrayList<DatabaseProfileRequestListener>();
            if (listener != null) {
                this.listenerList.add(listener);
            }
            this.request = request;
        }

        @Override protected ArrayList<ObjectWithId> doInBackground(Void... params) {
            return AccessDatabase.getInstance().getObjectWithIdListFor(request);
        }

        @Override protected void onPostExecute(ArrayList<ObjectWithId> objectList) {
            for (DatabaseProfileRequestListener listener : this.listenerList) {
                listener.databaseProfileRequestSuccessful(objectList);
            }
        }

        @Override protected void onCancelled(ArrayList<ObjectWithId> objectList) {
            for (DatabaseProfileRequestListener listener : this.listenerList) {
                listener.databaseProfileRequestFailed(Constants.RC.CANCELLED);
            }
        }

        public DatabaseProfileRequest getDatabaseProfileRequest() {
            return this.request;
        }

        public void addListener(DatabaseProfileRequestListener newListener) {
            if (newListener != null
                    && ! this.listenerList.contains(newListener)) {
                this.listenerList.add(newListener);
            }
        }

        public void removeListener(DatabaseProfileRequestListener newListener) {
            if (newListener != null
                    && this.listenerList.contains(newListener)) {
                this.listenerList.remove(newListener);
            }
        }

        public void cancel() {
            this.cancel(true);
        }
    }

}
