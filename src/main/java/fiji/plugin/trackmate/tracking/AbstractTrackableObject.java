package fiji.plugin.trackmate.tracking;

import java.util.concurrent.atomic.AtomicInteger;

import net.imglib2.AbstractRealLocalizable;

public abstract class AbstractTrackableObject extends AbstractRealLocalizable implements TrackableObject
{

	public static AtomicInteger IDcounter = new AtomicInteger( -1 );

	/** A user-supplied name for this object. */
	protected String name;

	protected final int id;

	private int frame;

	private boolean isVisible;

	public AbstractTrackableObject( final double[] position, final int frame )
	{
		this( position, null, frame );
	}

	public AbstractTrackableObject( final double[] position, final String name, final int frame )
	{
		super( position );
		this.id = IDcounter.incrementAndGet();
		this.frame = frame;
		this.isVisible = true;
		if ( name == null )
		{
			this.name = "ID" + id;
		}
		else
		{
			this.name = name;
		}
	}

	/**
	 * A blank, protected constructor that only sets the dimensionality of this
	 * {@link AbstractTrackableObject} and its ID. All other fields are left
	 * uninitialized.
	 * <p>
	 * It is meant to be used when loading from XML, ensuring the loaded object
	 * has the desired ID. It also ensures that the next
	 * {@link AbstractTrackableObject}s created via other constructors will have
	 * a different ID that all other IDs created with this constructor.
	 * 
	 * @param id
	 *            the desired ID.
	 * @param n
	 *            the dimensionality of the {@link TrackableObject}.
	 */
	protected AbstractTrackableObject( final int id, final int n )
	{
		super( n );
		this.id = id;
		synchronized ( IDcounter )
		{
			if ( IDcounter.get() < id )
			{
				IDcounter.set( id );
			}
		}
	}

	/**
	 * @return the name for this Object.
	 */
	@Override
	public String getName()
	{
		return this.name;
	}

	/**
	 * Set the name of this Object.
	 */
	@Override
	public void setName( final String name )
	{
		this.name = name;
	}

	@Override
	public int hashCode()
	{
		return ID();
	}

	@Override
	public boolean equals( final Object other )
	{
		if ( other == null )
			return false;
		if ( other == this )
			return true;
		if ( !( other instanceof TrackableObject ) )
			return false;
		final TrackableObject os = ( TrackableObject ) other;
		return os.ID() == this.id;
	}

	@Override
	public String toString()
	{
		String str;
		if ( null == name || name.equals( "" ) )
			str = "ID" + ID();
		else
			str = name;
		return str;
	}

	@Override
	public int ID()
	{
		return id;
	}

	@Override
	public int frame()
	{
		return frame;
	}

	@Override
	public void setFrame( final int frame )
	{
		this.frame = frame;
	}

	@Override
	public void setVisible( final boolean visibility )
	{
		this.isVisible = visibility;
	}

	@Override
	public boolean isVisible()
	{
		return isVisible;
	}
}
