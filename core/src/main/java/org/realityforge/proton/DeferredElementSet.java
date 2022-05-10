package org.realityforge.proton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * A collection of TypeElement instances that have been deferred until later processing rounds as their types could not be fully resolved.
 */
public final class DeferredElementSet
{
  /**
   * The list of TypeElement instances deferred since the last call to extractDeferred.
   */
  @Nonnull
  private final Set<TypeElement> _deferred = new HashSet<>();

  /**
   * Extract type elements that were deferred in previous rounds and clear deferred list.
   *
   * @param processingEnv the processing environment.
   * @return a list of TypeElement instances deferred in previous round.
   */
  public List<TypeElement> extractDeferred( @Nonnull final ProcessingEnvironment processingEnv )
  {
    final List<TypeElement> deferred =
      _deferred
        .stream()
        .map( e -> processingEnv.getElementUtils().getTypeElement( e.getQualifiedName() ) )
        .collect( Collectors.toList() );
    clear();
    return deferred;
  }

  /**
   * Clear the set of deferred types.
   * This should be explicitly invoked in the annotation processing when processing rounds have completed.
   */
  public void clear()
  {
    _deferred.clear();
  }

  /**
   * Return the underlying set of deferred types.
   *
   * @return the underlying set of deferred types.
   */
  @Nonnull
  public Set<TypeElement> getDeferred()
  {
    return _deferred;
  }

  public void deferElement( @Nonnull final TypeElement element )
  {
    _deferred.add( element );
  }
}
