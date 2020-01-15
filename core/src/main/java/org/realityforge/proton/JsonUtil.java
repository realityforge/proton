package org.realityforge.proton;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.processing.ProcessingEnvironment;
import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.lang.model.element.Element;

public final class JsonUtil
{
  private JsonUtil()
  {
  }

  public static void writeJsonResource( @Nonnull final ProcessingEnvironment processingEnv,
                                        @Nonnull final Element element,
                                        @Nonnull final String filename,
                                        @Nonnull final Consumer<JsonGenerator> action )
    throws IOException
  {
    final Map<String, Object> properties = new HashMap<>();
    properties.put( JsonGenerator.PRETTY_PRINTING, true );
    final JsonGeneratorFactory generatorFactory = Json.createGeneratorFactory( properties );

    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    final JsonGenerator g = generatorFactory.createGenerator( baos );
    action.accept( g );
    g.close();

    ResourceUtil.writeResource( processingEnv, filename, formatJson( baos.toString() ), element );
  }

  /**
   * Format the json file.
   * This is horribly inefficient but it is not called very often or with big files so ... meh.
   */
  @Nonnull
  public static String formatJson( @Nonnull final String input )
  {
    return
      input
        .replaceAll( "(?m)^ {4}([^ ])", "  $1" )
        .replaceAll( "(?m)^ {8}([^ ])", "    $1" )
        .replaceAll( "(?m)^ {12}([^ ])", "      $1" )
        .replaceAll( "(?m)^ {16}([^ ])", "        $1" )
        .replaceAll( "(?m)^ {20}([^ ])", "          $1" )
        .replaceAll( "(?m)^ {24}([^ ])", "            $1" )
        .replaceAll( "(?m)^ {28}([^ ])", "              $1" )
        .replaceAll( "(?m)^ {32}([^ ])", "                $1" )
        .replaceAll( "(?m)^\n\\[\n", "[\n" )
        .replaceAll( "(?m)^\n\\{\n", "{\n" ) +
      "\n";
  }
}
