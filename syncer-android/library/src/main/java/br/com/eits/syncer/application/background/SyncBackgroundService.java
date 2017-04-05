package br.com.eits.syncer.application.background;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.eits.syncer.Syncer;
import br.com.eits.syncer.application.ApplicationHolder;
import br.com.eits.syncer.domain.entity.EntityUpdatedId;
import br.com.eits.syncer.domain.entity.Revision;
import br.com.eits.syncer.domain.entity.RevisionType;
import br.com.eits.syncer.domain.entity.SyncData;
import br.com.eits.syncer.infrastructure.dao.RevisionDao;
import br.com.eits.syncer.infrastructure.dao.SQLiteHelper;

/**
 * Define a Service that returns an IBinder for the sync adapter class,
 * allowing the sync adapter framework to call onPerformSync().
 */
public class SyncBackgroundService extends JobService
{
    /**
     *
     */
    private final RevisionDao revisionDao;
    /**
     *
     */
//    private final ORMOpenHelper helper = new ORMOpenHelper( ApplicationHolder.CONTEXT );

    /**
     *
     */
    private final SQLiteHelper helper = new SQLiteHelper(ApplicationHolder.CONTEXT);

    /**
     *
     */
    public SyncBackgroundService()
    {
//        this.revisionDao = new RevisionDao( this.helper.getRuntimeExceptionDao(Revision.class) );
        this.revisionDao = new RevisionDao();
    }

    /**
     * Override this method with the callback logic for your job. Any such logic needs to be
     * performed on a separate thread, as this function is executed on your application's main
     * thread.
     *
     * @param params Parameters specifying info about this job, including the extras bundle you
     *               optionally provided at job-creation time.
     * @return True if your service needs to process the work (on a separate thread). False if
     * there's no more work to be done for this job.
     */
    @Override
    public boolean onStartJob(JobParameters params)
    {
        Log.wtf( SyncBackgroundService.class.getSimpleName(), "onStartJob -> "+ params );

        //Note: this is preformed on the main thread.
        new UpdateAppsAsyncTask().execute(params);
        return true;
    }

    /**
     * This method is called if the system has determined that you must stop execution of your job
     * even before you've had a chance to call {@link #jobFinished(JobParameters, boolean)}.
     *
     * <p>This will happen if the requirements specified at schedule time are no longer met. For
     * example you may have requested WiFi with
     * {@link android.app.job.JobInfo.Builder#setRequiredNetworkType(int)}, yet while your
     * job was executing the user toggled WiFi. Another example is if you had specified
     * {@link android.app.job.JobInfo.Builder#setRequiresDeviceIdle(boolean)}, and the phone left its
     * idle maintenance window. You are solely responsible for the behaviour of your application
     * upon receipt of this message; your app will likely start to misbehave if you ignore it. One
     * immediate repercussion is that the system will cease holding a wakelock for you.</p>
     *
     * @param params Parameters specifying info about this job.
     * @return True to indicate to the JobManager whether you'd like to reschedule this job based
     * on the retry criteria provided at job creation-time. False to drop the job. Regardless of
     * the value returned, your job must stop executing.
     */
    @Override
    public boolean onStopJob(JobParameters params)
    {
        Log.wtf( SyncBackgroundService.class.getSimpleName(), "onStopJob -> "+ params );

        //boolean shouldReschedule = updateTask.cancel(true);
        //return shouldReschedule;

        return false;
    }

    /**
     *
     */
    private class UpdateAppsAsyncTask extends AsyncTask<JobParameters, Void, JobParameters[]>
    {
        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         *
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         *
         * @return A result, defined by the subclass of this task.
         *
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected JobParameters[] doInBackground( JobParameters... params )
        {
            Log.wtf( UpdateAppsAsyncTask.class.getSimpleName(), "doInBackground -> "+ params );

            try
            {
                //-VERIFICAR A DEMORA
                //    -VERIFICAR A TRHEAD DE AGENDAMENTO ??
                //    -VERIFICAR QUANDO REMOVE / ALTERA
//                final ISyncResource syncResource = Syncer.getSyncResource();

                SyncBackgroundService.this.revisionDao.open();
                final List<Object> revisions = SyncBackgroundService.this.revisionDao.queryForEq(SQLiteHelper.COLUMN_SYNCED, 0);

                for( Object entry : revisions )
                {
                    Revision revision = ( Revision ) entry;
                    final Class<?> clazz = Class.forName( revision.getEntityClassName() );
                    final Object entity = Syncer.getMapper().readValue( revision.getEntity().toString(), clazz );
                    revision.setEntity( clazz.cast( entity ) );
                }

                //sync these remotely
                final long lastRevision = ( (Revision) revisions.get(revisions.size()-1) ).getRevision(); // FIXME: pq precisa disto?
                final SyncData localSyncData = new SyncData( lastRevision, null, revisions );
                final SyncData remoteSyncData = Syncer.syncronize( localSyncData );

                //remove all revisions not synced
                SyncBackgroundService.this.revisionDao.removeAllNotSynced();

                //save remote revisions as synced
                final List<Object> remoteRevisions = remoteSyncData.getRevisions();
                for( Object entry : remoteRevisions )
                {
                    final Revision revision = (Revision) entry;
                    final Revision newRevision = new Revision( revision.getEntity(), RevisionType.UPDATE );
                    newRevision.setSynced( true );
                    SyncBackgroundService.this.revisionDao.insertRevision( newRevision );
                }

                SyncBackgroundService.this.revisionDao.close();
                return params;
            }
            catch( Exception e )
            {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        /**
         * <p>Runs on the UI thread after {@link #doInBackground}. The
         * specified result is the value returned by {@link #doInBackground}.</p>
         *
         * <p>This method won't be invoked if the task was cancelled.</p>
         *
         * @param result The result of the operation computed by {@link #doInBackground}.
         *
         * @see #onPreExecute
         * @see #doInBackground
         * @see #onCancelled(Object)
         */
        @Override
        protected void onPostExecute( JobParameters[] result )
        {
            Log.wtf( UpdateAppsAsyncTask.class.getSimpleName(), "onPostExecute -> "+ result );

            for ( JobParameters params : result )
            {
                SyncBackgroundService.this.jobFinished(params, false);
            }
        }
    }
}