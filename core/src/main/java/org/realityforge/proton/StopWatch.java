package org.realityforge.proton;

import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * A simple class used to time sections of code and accumulate times for section over multiple executions.
 */
public final class StopWatch
{
  @Nonnull
  private final String _name;
  /**
   * The total time spent in timer since last reset.
   */
  private long _totalDuration;
  /**
   * The time at which timer was started or 0 if no timer is active.
   */
  private long _start;

  public StopWatch( @Nonnull final String name )
  {
    _name = Objects.requireNonNull( name );
  }

  @Nonnull
  public String getName()
  {
    return _name;
  }

  public long getTotalDuration()
  {
    return _totalDuration;
  }

  public void start()
  {
    if ( 0 != _start )
    {
      throw new IllegalStateException( "Attempted to start " + _name + " timer that had already been started" );
    }
    _start = System.nanoTime();
  }

  public void stop()
  {
    if ( 0 == _start )
    {
      throw new IllegalStateException( "Attempted to stop '" + _name + "' timer that had not been started" );
    }
    _totalDuration += System.nanoTime() - _start;
    _start = 0;
  }

  public void reset()
  {
    _totalDuration = 0;
  }

  @Nonnull
  @Override
  public String toString()
  {
    return _name + ": " + _totalDuration;
  }
}
