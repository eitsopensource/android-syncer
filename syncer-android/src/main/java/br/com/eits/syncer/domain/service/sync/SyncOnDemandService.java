package br.com.eits.syncer.domain.service.sync;

import android.util.Log;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import br.com.eits.syncer.Syncer;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by eduardo on 22/01/2018.
 */

public class SyncOnDemandService
{
	private static final Map<String, PersistentSyncTask> PERSISTENT_TASKS = new ConcurrentHashMap<>();
	private static final String TAG = "[sync]";

	static
	{
		for ( String serviceName : Syncer.syncResourceConfiguration().getServiceNames() )
		{
			PERSISTENT_TASKS.put( serviceName, new PersistentSyncTask( serviceName, Syncer.syncResourceConfiguration().getSyncResource( serviceName ) ) );
		}
	}

	public static Observable<Void> syncNow( String serviceName )
	{
		final PersistentSyncTask task = PERSISTENT_TASKS.get( serviceName );
		if ( task == null )
		{
			throw new IllegalArgumentException( "There is no service defined with the name \"" + serviceName + "\"." );
		}
		return Observable.create( task )
				.subscribeOn( Schedulers.io() ).observeOn( AndroidSchedulers.mainThread() );
	}

	public static void syncNowBackground( final String serviceName )
	{
		syncNow( serviceName ).observeOn( Schedulers.io() )
				.subscribe( new Observer<Void>()
				{
					private final String tag = tagFor( serviceName );

					@Override
					public void onSubscribe( Disposable d )
					{
						Log.i( tag, "Starting background sync" );
					}

					@Override
					public void onNext( Void aVoid )
					{

					}

					@Override
					public void onError( Throwable e )
					{
						Log.i( tag, "Sync failed" );
					}

					@Override
					public void onComplete()
					{
						Log.i( tag, "Sync complete" );
					}
				} );
	}

	public static Observable<String> syncAllNow()
	{
		return Observable.create( new ObservableOnSubscribe<String>()
		{
			@Override
			public void subscribe( final ObservableEmitter<String> serviceDoneEmitter ) throws Exception
			{
				final Set<String> completedServices = new HashSet<>();
				for ( final String serviceName : PERSISTENT_TASKS.keySet() )
				{
					syncNow( serviceName )
							.subscribe( new Observer<Void>()
							{
								private Disposable subscription;

								@Override
								public void onSubscribe( Disposable d )
								{
									subscription = d;
								}

								@Override
								public void onNext( Void aVoid )
								{

								}

								@Override
								public void onError( Throwable e )
								{
									if ( !subscription.isDisposed() )
									{
										serviceDoneEmitter.tryOnError( new RuntimeException( serviceName, e ) );
									}
								}

								@Override
								public void onComplete()
								{
									completedServices.add( serviceName );
									serviceDoneEmitter.onNext( serviceName );
									if ( completedServices.equals( PERSISTENT_TASKS.keySet() ) )
									{
										serviceDoneEmitter.onComplete();
									}
								}
							} );
				}
			}
		} ).subscribeOn( Schedulers.io() ).observeOn( AndroidSchedulers.mainThread() );
	}

	private static String tagFor( String serviceName )
	{
		return "[sync//" + serviceName + "]";
	}
}
