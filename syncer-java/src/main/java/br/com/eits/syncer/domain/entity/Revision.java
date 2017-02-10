package br.com.eits.syncer.domain.entity;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 */
public class Revision<T> implements Serializable
{
	/**
	 *
	 */
	private static final long serialVersionUID = 2518817552731435286L;

	/*-------------------------------------------------------------------
	 *				 		     ATTRIBUTES
	 *-------------------------------------------------------------------*/
	/**
	 *
	 */
	private Long revision;
	/**
	 *
	 */
	private Boolean synced;
	/**
	 *
	 */
	private Boolean mustSync;
	/**
	 *
	 */
	private RevisionType type;
	/**
	 *
	 */
	private T entity;
	/**
	 *
	 */
	private String entityId;
	/**
	 *
	 */
	private String entityClassName;

	/*-------------------------------------------------------------------
	 *				 		     CONSTRUCTORS
	 *-------------------------------------------------------------------*/
	/**
	 * 
	 * @param entity
	 * @param type
	 */
	@JsonCreator
	public Revision( @JsonProperty("entity") T entity, @JsonProperty("type") RevisionType type, @JsonProperty("mustSync") Boolean mustSync )
	{
		this.revision = Calendar.getInstance().getTimeInMillis();
		this.entity = entity;
		this.entityClassName = entity.getClass().getName();
		this.mustSync = mustSync;
		this.type = type;
		this.synced = false;
	}

	/*-------------------------------------------------------------------
	 *				 		     BEHAVIORS
	 *-------------------------------------------------------------------*/

	/**
	 *
	 * @param o
	 * @return
     */
	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final Revision<T> revision = (Revision<T>) o;
		return Objects.equals(revision, revision.revision) &&
				Objects.equals(synced, revision.synced) &&
				Objects.equals(mustSync, revision.mustSync) &&
				type == revision.type &&
				Objects.equals(entity, revision.entity) &&
				Objects.equals(entityClassName, revision.entityClassName);
	}

	/**
	 *
	 * @return
     */
	@Override
	public int hashCode()
	{
		return Objects.hash(revision, synced, mustSync, type, entity, entityClassName);
	}

	/*-------------------------------------------------------------------
    *				 		   GETTERS AND SETTERS
    *-------------------------------------------------------------------*/
	/**
	 *
	 * @return
     */
	public String getEntityClassName()
	{
		return this.entityClassName;
	}

	/**
	 *
	 * @return
     */
	public RevisionType getType()
	{
		return this.type;
	}

	/**
	 *
	 * @return
     */
	public T getEntity()
	{
		return this.entity;
	}

	/**
	 *
	 * @return
	 */
	public Boolean getSynced()
	{
		return this.synced;
	}

	/**
	 *
	 * @param synced
	 */
	public void setSynced(Boolean synced)
	{
		this.synced = synced;
	}

	/**
	 * @return the id
	 */
	public Long getRevision()
	{
		return this.revision;
	}

	/**
	 * @return the mustSync
	 */
	public Boolean getMustSync()
	{
		return this.mustSync;
	}

	/**
	 * @return the entityId
	 */
	public String getEntityId()
	{
		return entityId;
	}

	/**
	 * @param entityId the entityId to set
	 */
	public void setEntityId( String entityId )
	{
		this.entityId = entityId;
	}
}