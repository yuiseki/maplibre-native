package org.maplibre.android.maps;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Pair;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.maplibre.android.MapLibre;
import org.maplibre.android.constants.MapLibreConstants;
import org.maplibre.android.style.layers.Layer;
import org.maplibre.android.style.layers.TransitionOptions;
import org.maplibre.android.style.light.Light;
import org.maplibre.android.style.sources.Source;
import org.maplibre.android.util.DefaultStyle;
import org.maplibre.android.utils.BitmapUtils;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The proxy object for current map style.
 * <p>
 * To create new instances of this object, create a new instance using a {@link Builder} and load the style with
 * MapLibreMap. This object is returned from {@link MapLibreMap#getStyle()} once the style
 * has been loaded by underlying map.
 * </p>
 */
@SuppressWarnings("unchecked")
public class Style {

  static final String EMPTY_JSON = "{\"version\": 8,\"sources\": {},\"layers\": []}";

  private final NativeMap nativeMap;
  private final HashMap<String, Source> sources = new HashMap<>();
  private final HashMap<String, Layer> layers = new HashMap<>();
  private final HashMap<String, Bitmap> images = new HashMap<>();
  private final Builder builder;
  private boolean fullyLoaded;

  /**
   * Private constructor to build a style object.
   *
   * @param builder   the builder used for creating this style
   * @param nativeMap the map object used to load this style
   */
  private Style(@NonNull Builder builder, @NonNull NativeMap nativeMap) {
    this.builder = builder;
    this.nativeMap = nativeMap;
  }

  /**
   * Returns the current style url.
   *
   * @return the style url
   * @deprecated use {@link #getUri()} instead
   */
  @NonNull
  @Deprecated
  public String getUrl() {
    validateState("getUrl");
    return nativeMap.getStyleUri();
  }

  /**
   * Returns the current style uri.
   *
   * @return the style uri
   */
  @NonNull
  public String getUri() {
    validateState("getUri");
    return nativeMap.getStyleUri();
  }

  /**
   * Returns the current style json.
   *
   * @return the style json
   */
  @NonNull
  public String getJson() {
    validateState("getJson");
    return nativeMap.getStyleJson();
  }

  //
  // Source
  //

  /**
   * Retrieve all the sources in the style
   *
   * @return all the sources in the current style
   */
  @NonNull
  public List<Source> getSources() {
    validateState("getSources");
    return nativeMap.getSources();
  }

  /**
   * Adds the source to the map. The source must be newly created and not added to the map before
   *
   * @param source the source to add
   */
  public void addSource(@NonNull Source source) {
    validateState("addSource");
    nativeMap.addSource(source);
    sources.put(source.getId(), source);
  }

  /**
   * Retrieve a source by id
   *
   * @param id the source's id
   * @return the source if present in the current style
   */
  @Nullable
  public Source getSource(String id) {
    validateState("getSource");
    Source source = sources.get(id);
    if (source == null) {
      source = nativeMap.getSource(id);
    }
    return source;
  }

  /**
   * Tries to cast the Source to T, throws ClassCastException if it's another type.
   *
   * @param sourceId the id used to look up a layer
   * @param <T>      the generic type of a Source
   * @return the casted Source, null if another type
   */
  @Nullable
  public <T extends Source> T getSourceAs(@NonNull String sourceId) {
    validateState("getSourceAs");
    // noinspection unchecked
    if (sources.containsKey(sourceId)) {
      return (T) sources.get(sourceId);
    }
    return (T) nativeMap.getSource(sourceId);
  }

  /**
   * Removes the source from the style.
   *
   * @param sourceId the source to remove
   * @return true if the source was removed, false otherwise
   */
  public boolean removeSource(@NonNull String sourceId) {
    validateState("removeSource");
    boolean successful = nativeMap.removeSource(sourceId);
    if (successful) {
      sources.remove(sourceId);
    }
    return successful;
  }

  /**
   * Removes the source, preserving the reference for re-use
   *
   * @param source the source to remove
   * @return true if the source was removed, false otherwise
   */
  public boolean removeSource(@NonNull Source source) {
    validateState("removeSource");
    boolean successful = nativeMap.removeSource(source);
    if (successful) {
      sources.remove(source.getId());
    }
    return successful;
  }

  //
  // Layer
  //

  /**
   * Adds the layer to the map. The layer must be newly created and not added to the map before
   *
   * @param layer the layer to add
   */
  public void addLayer(@NonNull Layer layer) {
    validateState("addLayer");
    nativeMap.addLayer(layer);
    layers.put(layer.getId(), layer);
  }

  /**
   * Adds the layer to the map. The layer must be newly created and not added to the map before
   *
   * @param layer the layer to add
   * @param below the layer id to add this layer before
   */
  public void addLayerBelow(@NonNull Layer layer, @NonNull String below) {
    validateState("addLayerBelow");
    nativeMap.addLayerBelow(layer, below);
    layers.put(layer.getId(), layer);
  }

  /**
   * Adds the layer to the map. The layer must be newly created and not added to the map before
   *
   * @param layer the layer to add
   * @param above the layer id to add this layer above
   */
  public void addLayerAbove(@NonNull Layer layer, @NonNull String above) {
    validateState("addLayerAbove");
    nativeMap.addLayerAbove(layer, above);
    layers.put(layer.getId(), layer);
  }

  /**
   * Adds the layer to the map at the specified index. The layer must be newly
   * created and not added to the map before
   *
   * @param layer the layer to add
   * @param index the index to insert the layer at
   */
  public void addLayerAt(@NonNull Layer layer, @IntRange(from = 0) int index) {
    validateState("addLayerAbove");
    nativeMap.addLayerAt(layer, index);
    layers.put(layer.getId(), layer);
  }

  /**
   * Get the layer by id
   *
   * @param id the layer's id
   * @return the layer, if present in the style
   */
  @Nullable
  public Layer getLayer(@NonNull String id) {
    validateState("getLayer");
    Layer layer = layers.get(id);
    if (layer == null) {
      layer = nativeMap.getLayer(id);
    }
    return layer;
  }

  /**
   * Tries to cast the Layer to T, throws ClassCastException if it's another type.
   *
   * @param layerId the layer id used to look up a layer
   * @param <T>     the generic attribute of a Layer
   * @return the casted Layer, null if another type
   */
  @Nullable
  public <T extends Layer> T getLayerAs(@NonNull String layerId) {
    validateState("getLayerAs");
    // noinspection unchecked
    return (T) nativeMap.getLayer(layerId);
  }

  /**
   * Retrieve all the layers in the style
   *
   * @return all the layers in the current style
   */
  @NonNull
  public List<Layer> getLayers() {
    validateState("getLayers");
    return nativeMap.getLayers();
  }

  /**
   * Removes the layer. Any references to the layer become invalid and should not be used anymore
   *
   * @param layerId the layer to remove
   * @return true if the layer was removed, false otherwise
   */
  public boolean removeLayer(@NonNull String layerId) {
    validateState("removeLayer");
    layers.remove(layerId);
    return nativeMap.removeLayer(layerId);
  }

  /**
   * Removes the layer. The reference is re-usable after this and can be re-added
   *
   * @param layer the layer to remove
   * @return true if the layer was removed, false otherwise
   */
  public boolean removeLayer(@NonNull Layer layer) {
    validateState("removeLayer");
    layers.remove(layer.getId());
    return nativeMap.removeLayer(layer);
  }

  /**
   * Removes the layer. Any other references to the layer become invalid and should not be used anymore
   *
   * @param index the layer index
   * @return true if the layer was removed, false otherwise
   */
  public boolean removeLayerAt(@IntRange(from = 0) int index) {
    validateState("removeLayerAt");
    return nativeMap.removeLayerAt(index);
  }

  //
  // Image
  //

  /**
   * Adds an image to be used in the map's style
   *
   * @param name  the name of the image
   * @param image the pre-multiplied Bitmap
   */
  public void addImage(@NonNull String name, @NonNull Bitmap image) {
    addImage(name, image, false);
  }

  /**
   * Adds an image to be used in the map's style
   *
   * @param name     the name of the image
   * @param image    the pre-multiplied Bitmap
   * @param stretchX image stretch areas for x axix
   * @param stretchY image stretch areas for y axix
   * @param content  image content for text to fit
   */
  public void addImage(@NonNull String name, @NonNull Bitmap image, @NonNull List<ImageStretches> stretchX,
                       @NonNull List<ImageStretches> stretchY, @Nullable ImageContent content) {
    addImage(name, image, false, stretchX, stretchY, content);
  }

  /**
   * Adds an drawable to be converted into a bitmap to be used in the map's style
   *
   * @param name     the name of the image
   * @param drawable the drawable instance to convert
   */
  public void addImage(@NonNull String name, @NonNull Drawable drawable) {
    Bitmap bitmap = BitmapUtils.getBitmapFromDrawable(drawable);
    if (bitmap == null) {
      throw new IllegalArgumentException("Provided drawable couldn't be converted to a Bitmap.");
    }
    addImage(name, bitmap, false);
  }

  /**
   * Adds an drawable to be converted into a bitmap to be used in the map's style
   *
   * @param name     the name of the image
   * @param drawable the drawable instance to convert
   * @param stretchX image stretch areas for x axix
   * @param stretchY image stretch areas for y axix
   * @param content  image content for text to fit
   */
  public void addImage(@NonNull String name, @NonNull Drawable drawable,
                       @NonNull List<ImageStretches> stretchX,
                       @NonNull List<ImageStretches> stretchY,
                       @Nullable ImageContent content) {
    Bitmap bitmap = BitmapUtils.getBitmapFromDrawable(drawable);
    if (bitmap == null) {
      throw new IllegalArgumentException("Provided drawable couldn't be converted to a Bitmap.");
    }
    addImage(name, bitmap, false, stretchX, stretchY, content);
  }

  /**
   * Adds an image to be used in the map's style
   *
   * @param name   the name of the image
   * @param bitmap the pre-multiplied Bitmap
   * @param sdf    the flag indicating image is an SDF or template image
   */
  public void addImage(@NonNull final String name, @NonNull Bitmap bitmap, boolean sdf) {
    validateState("addImage");
    nativeMap.addImages(new Image[] {toImage(new Builder.ImageWrapper(name, bitmap, sdf))});
  }

  /**
   * Adds an image to be used in the map's style
   *
   * @param name     the name of the image
   * @param bitmap   the pre-multiplied Bitmap
   * @param sdf      the flag indicating image is an SDF or template image
   * @param stretchX image stretch areas for x axix
   * @param stretchY image stretch areas for y axix
   * @param content  image content for text to fit
   */
  public void addImage(@NonNull final String name, @NonNull Bitmap bitmap, boolean sdf,
                       @NonNull List<ImageStretches> stretchX,
                       @NonNull List<ImageStretches> stretchY,
                       @Nullable ImageContent content) {
    validateState("addImage");
    nativeMap.addImages(new Image[] {
      toImage(new Builder.ImageWrapper(name, bitmap, sdf, stretchX, stretchY, content))});
  }

  /**
   * Adds an image asynchronously, to be used in the map's style.
   *
   * @param name  the name of the image
   * @param image the pre-multiplied Bitmap
   */
  public void addImageAsync(@NonNull String name, @NonNull Bitmap image) {
    addImageAsync(name, image, false);
  }

  /**
   * Adds an image asynchronously, to be used in the map's style.
   *
   * @param name     the name of the image
   * @param image    the pre-multiplied Bitmap
   * @param stretchX image stretch areas for x axix
   * @param stretchY image stretch areas for y axix
   * @param content  image content for text to fit
   */
  public void addImageAsync(@NonNull String name, @NonNull Bitmap image,
                            @NonNull List<ImageStretches> stretchX,
                            @NonNull List<ImageStretches> stretchY,
                            @Nullable ImageContent content) {
    addImageAsync(name, image, false, stretchX, stretchY, content);
  }

  /**
   * Adds an drawable asynchronously, to be converted into a bitmap to be used in the map's style.
   *
   * @param name     the name of the image
   * @param drawable the drawable instance to convert
   */
  public void addImageAsync(@NonNull String name, @NonNull Drawable drawable) {
    Bitmap bitmap = BitmapUtils.getBitmapFromDrawable(drawable);
    if (bitmap == null) {
      throw new IllegalArgumentException("Provided drawable couldn't be converted to a Bitmap.");
    }
    addImageAsync(name, bitmap, false);
  }


  /**
   * Adds an drawable asynchronously, to be converted into a bitmap to be used in the map's style.
   *
   * @param name     the name of the image
   * @param drawable the drawable instance to convert
   * @param stretchX image stretch areas for x axix
   * @param stretchY image stretch areas for y axix
   * @param content  image content for text to fit
   */
  public void addImageAsync(@NonNull String name, @NonNull Drawable drawable,
                            @NonNull List<ImageStretches> stretchX,
                            @NonNull List<ImageStretches> stretchY,
                            @Nullable ImageContent content) {
    Bitmap bitmap = BitmapUtils.getBitmapFromDrawable(drawable);
    if (bitmap == null) {
      throw new IllegalArgumentException("Provided drawable couldn't be converted to a Bitmap.");
    }
    addImageAsync(name, bitmap, false, stretchX, stretchY, content);
  }

  /**
   * Adds an image asynchronously, to be used in the map's style.
   *
   * @param name   the name of the image
   * @param bitmap the pre-multiplied Bitmap
   * @param sdf    the flag indicating image is an SDF or template image
   */
  public void addImageAsync(@NonNull final String name, @NonNull Bitmap bitmap, boolean sdf) {
    validateState("addImage");
    new BitmapImageConversionTask(nativeMap).execute(new Builder.ImageWrapper(name, bitmap, sdf));
  }

  /**
   * Adds an image asynchronously, to be used in the map's style.
   *
   * @param name     the name of the image
   * @param bitmap   the pre-multiplied Bitmap
   * @param sdf      the flag indicating image is an SDF or template image
   * @param stretchX image stretch areas for x axix
   * @param stretchY image stretch areas for y axix
   * @param content  image content for text to fit
   */
  public void addImageAsync(@NonNull final String name, @NonNull Bitmap bitmap, boolean sdf,
                            @NonNull List<ImageStretches> stretchX,
                            @NonNull List<ImageStretches> stretchY,
                            @Nullable ImageContent content) {
    validateState("addImage");
    new BitmapImageConversionTask(nativeMap)
      .execute(new Builder.ImageWrapper(name, bitmap, sdf, stretchX, stretchY, content));
  }

  /**
   * Adds images to be used in the map's style.
   *
   * @param images the map of images to add
   */
  public void addImages(@NonNull HashMap<String, Bitmap> images) {
    addImages(images, false);
  }

  /**
   * Adds images to be used in the map's style.
   *
   * @param images   the map of images to add
   * @param stretchX image stretch areas for x axix
   * @param stretchY image stretch areas for y axix
   * @param content  image content for text to fit
   */
  public void addImages(@NonNull HashMap<String, Bitmap> images, @NonNull List<ImageStretches> stretchX,
                        @NonNull List<ImageStretches> stretchY, @Nullable ImageContent content) {
    addImages(images, false, stretchX, stretchY, content);
  }

  /**
   * Adds images to be used in the map's style.
   *
   * @param images the map of images to add
   * @param sdf    the flag indicating image is an SDF or template image
   */
  public void addImages(@NonNull HashMap<String, Bitmap> images, boolean sdf) {
    validateState("addImage");
    Image[] convertedImages = new Image[images.size()];
    int index = 0;
    for (Builder.ImageWrapper imageWrapper : Builder.ImageWrapper.convertToImageArray(images, sdf)) {
      convertedImages[index] = toImage(imageWrapper);
      index++;
    }

    nativeMap.addImages(convertedImages);
  }

  /**
   * Adds images to be used in the map's style.
   *
   * @param images   the map of images to add
   * @param sdf      the flag indicating image is an SDF or template image
   * @param stretchX image stretch areas for x axix
   * @param stretchY image stretch areas for y axix
   * @param content  image content for text to fit
   */
  public void addImages(@NonNull HashMap<String, Bitmap> images, boolean sdf,
                        @NonNull List<ImageStretches> stretchX,
                        @NonNull List<ImageStretches> stretchY,
                        @Nullable ImageContent content) {
    validateState("addImage");
    Image[] convertedImages = new Image[images.size()];
    int index = 0;
    for (Builder.ImageWrapper imageWrapper
      : Builder.ImageWrapper.convertToImageArray(images, sdf, stretchX, stretchY, content)) {
      convertedImages[index] = toImage(imageWrapper);
      index++;
    }

    nativeMap.addImages(convertedImages);
  }

  /**
   * Adds images asynchronously, to be used in the map's style.
   *
   * @param images the map of images to add
   */
  public void addImagesAsync(@NonNull HashMap<String, Bitmap> images) {
    addImagesAsync(images, false);
  }

  /**
   * Adds images asynchronously, to be used in the map's style.
   *
   * @param images   the map of images to add
   * @param stretchX image stretch areas for x axix
   * @param stretchY image stretch areas for y axix
   * @param content  image content for text to fit
   */
  public void addImagesAsync(@NonNull HashMap<String, Bitmap> images, @NonNull List<ImageStretches> stretchX,
                             @NonNull List<ImageStretches> stretchY, @Nullable ImageContent content) {
    addImagesAsync(images, false, stretchX, stretchY, content);
  }

  /**
   * Adds images asynchronously, to be used in the map's style.
   *
   * @param images the map of images to add
   * @param sdf    the flag indicating image is an SDF or template image
   */
  public void addImagesAsync(@NonNull HashMap<String, Bitmap> images, boolean sdf) {
    validateState("addImages");
    new BitmapImageConversionTask(nativeMap).execute(Builder.ImageWrapper.convertToImageArray(images, sdf));
  }

  /**
   * Adds images asynchronously, to be used in the map's style.
   *
   * @param images   the map of images to add
   * @param sdf      the flag indicating image is an SDF or template image
   * @param stretchX image stretch areas for x axix
   * @param stretchY image stretch areas for y axix
   * @param content  image content for text to fit
   */
  public void addImagesAsync(@NonNull HashMap<String, Bitmap> images, boolean sdf,
                             @NonNull List<ImageStretches> stretchX,
                             @NonNull List<ImageStretches> stretchY,
                             @Nullable ImageContent content) {
    validateState("addImages");
    new BitmapImageConversionTask(nativeMap)
      .execute(Builder.ImageWrapper.convertToImageArray(images, sdf, stretchX, stretchY, content));
  }

  /**
   * Removes an image from the map's style.
   *
   * @param name the name of the image to remove
   */
  public void removeImage(@NonNull String name) {
    validateState("removeImage");
    nativeMap.removeImage(name);
  }

  /**
   * Get an image from the map's style using an id.
   *
   * @param id the id of the image
   * @return the image bitmap
   */
  @Nullable
  public Bitmap getImage(@NonNull String id) {
    validateState("getImage");
    return nativeMap.getImage(id);
  }

  //
  // Transition
  //

  /**
   * <p>
   * Set the transition options for style changes.
   * </p>
   * If not set, any changes take effect without animation, besides symbols,
   * which will fade in/out with a default duration after symbol collision detection.
   * <p>
   * To disable symbols fade in/out animation,
   * pass transition options with {@link TransitionOptions#enablePlacementTransitions} equal to false.
   * <p>
   * Both {@link TransitionOptions#duration} and {@link TransitionOptions#delay}
   * will also change the behavior of the symbols fade in/out animation if the placement transition is enabled.
   *
   * @param transitionOptions the transition options
   */
  public void setTransition(@NonNull TransitionOptions transitionOptions) {
    validateState("setTransition");
    nativeMap.setTransitionOptions(transitionOptions);
  }

  /**
   * <p>
   * Get the transition options for style changes.
   * </p>
   * By default, any changes take effect without animation, besides symbols,
   * which will fade in/out with a default duration after symbol collision detection.
   * <p>
   * To disable symbols fade in/out animation,
   * pass transition options with {@link TransitionOptions#enablePlacementTransitions} equal to false
   * into {@link #setTransition(TransitionOptions)}.
   * <p>
   * Both {@link TransitionOptions#duration} and {@link TransitionOptions#delay}
   * will also change the behavior of the symbols fade in/out animation if the placement transition is enabled.
   *
   * @return TransitionOptions the transition options
   */
  @NonNull
  public TransitionOptions getTransition() {
    validateState("getTransition");
    return nativeMap.getTransitionOptions();
  }

  //
  // Light
  //

  /**
   * Get the light source used to change lighting conditions on extruded fill layers.
   *
   * @return the global light source
   */
  @Nullable
  public Light getLight() {
    validateState("getLight");
    return nativeMap.getLight();
  }

  //
  // State
  //

  /**
   * Called when the underlying map will start loading a new style or the map is destroyed.
   * This method will clean up this style by setting the java sources and layers
   * in a detached state and removing them from core.
   */
  void clear() {
    fullyLoaded = false;
    for (Layer layer : layers.values()) {
      if (layer != null) {
        layer.setDetached();
      }
    }

    for (Source source : sources.values()) {
      if (source != null) {
        source.setDetached();
      }
    }

    for (Map.Entry<String, Bitmap> bitmapEntry : images.entrySet()) {
      nativeMap.removeImage(bitmapEntry.getKey());
      bitmapEntry.getValue().recycle();
    }

    sources.clear();
    layers.clear();
    images.clear();
  }

  /**
   * Called when the underlying map has finished loading this style.
   * This method will add all components added to the builder that were defined with the 'with' prefix.
   */
  void onDidFinishLoadingStyle() {
    if (!fullyLoaded) {
      fullyLoaded = true;
      for (Source source : builder.sources) {
        addSource(source);
      }

      for (Builder.LayerWrapper layerWrapper : builder.layers) {
        if (layerWrapper instanceof Builder.LayerAtWrapper) {
          addLayerAt(layerWrapper.layer, ((Builder.LayerAtWrapper) layerWrapper).index);
        } else if (layerWrapper instanceof Builder.LayerAboveWrapper) {
          addLayerAbove(layerWrapper.layer, ((Builder.LayerAboveWrapper) layerWrapper).aboveLayer);
        } else if (layerWrapper instanceof Builder.LayerBelowWrapper) {
          addLayerBelow(layerWrapper.layer, ((Builder.LayerBelowWrapper) layerWrapper).belowLayer);
        } else {
          // just add layer to map, but below annotations
          addLayerBelow(layerWrapper.layer, MapLibreConstants.LAYER_ID_ANNOTATIONS);
        }
      }

      for (Builder.ImageWrapper image : builder.images) {
        addImage(image.id, image.bitmap, image.sdf);
      }

      if (builder.transitionOptions != null) {
        setTransition(builder.transitionOptions);
      }
    }
  }

  /**
   * Returns true if the style is fully loaded. Returns false if style hasn't been fully loaded or a new style is
   * underway of being loaded.
   *
   * @return True if fully loaded, false otherwise
   */
  public boolean isFullyLoaded() {
    return fullyLoaded;
  }

  /**
   * Validates the style state, throw an IllegalArgumentException on invalid state.
   *
   * @param methodCall the calling method name
   */
  private void validateState(String methodCall) {
    if (!fullyLoaded) {
      throw new IllegalStateException(
        String.format("Calling %s when a newer style is loading/has loaded.", methodCall)
      );
    }
  }

  //
  // Builder
  //

  /**
   * Builder for composing a style object.
   */
  public static class Builder {

    private final List<Source> sources = new ArrayList<>();
    private final List<LayerWrapper> layers = new ArrayList<>();
    private final List<ImageWrapper> images = new ArrayList<>();

    private TransitionOptions transitionOptions;
    private String styleUri;
    private String styleJson;

    /**
     * <p>
     * Will loads a new map style asynchronous from the specified URL.
     * </p>
     * {@code url} can take the following forms:
     * <ul>
     * <li>{@code http://...} or {@code https://...}:
     * loads the style over the Internet from any web server.</li>
     * <li>{@code asset://...}:
     * loads the style from the APK {@code assets/} directory.
     * This is used to load a style bundled with your app.</li>
     * <li>{@code file://...}:
     * loads the style from a file path. This is used to load a style from disk.
     * </li>
     * </li>
     * </ul>
     * <p>
     * This method is asynchronous and will return before the style finishes loading.
     * If you wish to wait for the map to finish loading, listen to the {@link MapView.OnDidFinishLoadingStyleListener}
     * callback or provide an {@link OnStyleLoaded} callback when setting the style on MapLibreMap.
     * </p>
     * If the style fails to load or an invalid style URL is set, the map view will become blank.
     * An error message will be logged in the Android logcat and {@link MapView.OnDidFailLoadingMapListener} callback
     * will be triggered.
     *
     * @param url The URL of the map style
     * @return this
     * @see Style
     * @deprecated use {@link #fromUri(String)} instead
     */
    @Deprecated
    @NonNull
    public Builder fromUrl(@NonNull String url) {
      this.styleUri = url;
      return this;
    }

    /**
     * <p>
     * Will loads a new map style asynchronous from the specified URI.
     * </p>
     * {@code uri} can take the following forms:
     * <ul>
     * <li>{@code http://...} or {@code https://...}:
     * loads the style over the Internet from any web server.</li>
     * <li>{@code asset://...}:
     * loads the style from the APK {@code assets/} directory.
     * This is used to load a style bundled with your app.</li>
     * <li>{@code file://...}:
     * loads the style from a file path. This is used to load a style from disk.
     * </li>
     * </li>
     * </ul>
     * <p>
     * This method is asynchronous and will return before the style finishes loading.
     * If you wish to wait for the map to finish loading, listen to the {@link MapView.OnDidFinishLoadingStyleListener}
     * callback or use {@link MapLibreMap#setStyle(String, OnStyleLoaded)} instead.
     * </p>
     * If the style fails to load or an invalid style URI is set, the map view will become blank.
     * An error message will be logged in the Android logcat and {@link MapView.OnDidFailLoadingMapListener} callback
     * will be triggered.
     *
     * @param uri The URI of the map style
     * @return this
     * @see Style
     */
    @NonNull
    public Builder fromUri(@NonNull String uri) {
      this.styleUri = uri;
      return this;
    }

    /**
     * Will load a new map style from a json string.
     * <p>
     * If the style fails to load or an invalid style URI is set, the map view will become blank.
     * An error message will be logged in the Android logcat and {@link MapView.OnDidFailLoadingMapListener} callback
     * will be triggered.
     * </p>
     *
     * @return this
     */
    @NonNull
    public Builder fromJson(@NonNull String styleJson) {
      this.styleJson = styleJson;
      return this;
    }

    /**
     * Will add the source when map style has loaded.
     *
     * @param source the source to add
     * @return this
     */
    @NonNull
    public Builder withSource(@NonNull Source source) {
      sources.add(source);
      return this;
    }

    /**
     * Will add the sources when map style has loaded.
     *
     * @param sources the sources to add
     * @return this
     */
    @NonNull
    public Builder withSources(@NonNull Source... sources) {
      this.sources.addAll(Arrays.asList(sources));
      return this;
    }

    /**
     * Will add the layer when the style has loaded.
     *
     * @param layer the layer to be added
     * @return this
     */
    @NonNull
    public Builder withLayer(@NonNull Layer layer) {
      layers.add(new LayerWrapper(layer));
      return this;
    }

    /**
     * Will add the layers when the style has loaded.
     *
     * @param layers the layers to be added
     * @return this
     */
    @NonNull
    public Builder withLayers(@NonNull Layer... layers) {
      for (Layer layer : layers) {
        this.layers.add(new LayerWrapper(layer));
      }
      return this;
    }

    /**
     * Will add the layer when the style has loaded at a specified index.
     *
     * @param layer the layer to be added
     * @return this
     */
    @NonNull
    public Builder withLayerAt(@NonNull Layer layer, int index) {
      layers.add(new LayerAtWrapper(layer, index));
      return this;
    }

    /**
     * Will add the layer when the style has loaded above a specified layer id.
     *
     * @param layer the layer to be added
     * @return this
     */
    @NonNull
    public Builder withLayerAbove(@NonNull Layer layer, @NonNull String aboveLayerId) {
      layers.add(new LayerAboveWrapper(layer, aboveLayerId));
      return this;
    }

    /**
     * Will add the layer when the style has loaded below a specified layer id.
     *
     * @param layer the layer to be added
     * @return this
     */
    @NonNull
    public Builder withLayerBelow(@NonNull Layer layer, @NonNull String belowLayerId) {
      layers.add(new LayerBelowWrapper(layer, belowLayerId));
      return this;
    }

    /**
     * Will add the transition when the map style has loaded.
     *
     * @param transition the transition to be added
     * @return this
     */
    @NonNull
    public Builder withTransition(@NonNull TransitionOptions transition) {
      this.transitionOptions = transition;
      return this;
    }

    /**
     * Will add the drawable as image when the map style has loaded.
     *
     * @param id       the id for the image
     * @param drawable the drawable to be converted and added
     * @return this
     */
    @NonNull
    public Builder withImage(@NonNull String id, @NonNull Drawable drawable) {
      Bitmap bitmap = BitmapUtils.getBitmapFromDrawable(drawable);
      if (bitmap == null) {
        throw new IllegalArgumentException("Provided drawable couldn't be converted to a Bitmap.");
      }
      return this.withImage(id, bitmap, false);
    }

    /**
     * Will add the drawable as image when the map style has loaded.
     *
     * @param id       the id for the image
     * @param drawable the drawable to be converted and added
     * @param stretchX image stretch areas for x axix
     * @param stretchY image stretch areas for y axix
     * @param content  image content for text to fit
     * @return this
     */
    @NonNull
    public Builder withImage(@NonNull String id, @NonNull Drawable drawable, @NonNull List<ImageStretches> stretchX,
                             @NonNull List<ImageStretches> stretchY, @Nullable ImageContent content) {
      Bitmap bitmap = BitmapUtils.getBitmapFromDrawable(drawable);
      if (bitmap == null) {
        throw new IllegalArgumentException("Provided drawable couldn't be converted to a Bitmap.");
      }
      return this.withImage(id, bitmap, false, stretchX, stretchY, content);
    }

    /**
     * Will add the drawables as images when the map style has loaded.
     *
     * @param values pairs, where first is the id for te image and second is the drawable
     * @return this
     */
    @NonNull
    public Builder withDrawableImages(@NonNull Pair<String, Drawable>... values) {
      return this.withDrawableImages(false, values);
    }

    /**
     * Will add the image when the map style has loaded.
     *
     * @param id    the id for the image
     * @param image the image to be added
     * @return this
     */
    @NonNull
    public Builder withImage(@NonNull String id, @NonNull Bitmap image) {
      return this.withImage(id, image, false);
    }

    /**
     * Will add the image when the map style has loaded.
     *
     * @param id       the id for the image
     * @param image    the image to be added
     * @param stretchX image stretch areas for x axix
     * @param stretchY image stretch areas for y axix
     * @param content  image content for text to fit
     * @return this
     */
    @NonNull
    public Builder withImage(@NonNull String id, @NonNull Bitmap image, @NonNull List<ImageStretches> stretchX,
                             @NonNull List<ImageStretches> stretchY, @Nullable ImageContent content) {
      return this.withImage(id, image, false, stretchX, stretchY, content);
    }

    /**
     * Will add the images when the map style has loaded.
     *
     * @param values pairs, where first is the id for te image and second is the bitmap
     * @return this
     */
    @NonNull
    public Builder withBitmapImages(@NonNull Pair<String, Bitmap>... values) {
      for (Pair<String, Bitmap> value : values) {
        this.withImage(value.first, value.second, false);
      }
      return this;
    }

    /**
     * Will add the drawable as image when the map style has loaded.
     *
     * @param id       the id for the image
     * @param drawable the drawable to be converted and added
     * @param sdf      the flag indicating image is an SDF or template image
     * @return this
     */
    @NonNull
    public Builder withImage(@NonNull String id, @NonNull Drawable drawable, boolean sdf) {
      Bitmap bitmap = BitmapUtils.getBitmapFromDrawable(drawable);
      if (bitmap == null) {
        throw new IllegalArgumentException("Provided drawable couldn't be converted to a Bitmap.");
      }
      return this.withImage(id, bitmap, sdf);
    }

    /**
     * Will add the drawable as image when the map style has loaded.
     *
     * @param id       the id for the image
     * @param drawable the drawable to be converted and added
     * @param sdf      the flag indicating image is an SDF or template image
     * @param stretchX image stretch areas for x axix
     * @param stretchY image stretch areas for y axix
     * @param content  image content for text to fit
     * @return this
     */
    @NonNull
    public Builder withImage(@NonNull String id, @NonNull Drawable drawable, boolean sdf,
                             @NonNull List<ImageStretches> stretchX,
                             @NonNull List<ImageStretches> stretchY,
                             @Nullable ImageContent content) {
      Bitmap bitmap = BitmapUtils.getBitmapFromDrawable(drawable);
      if (bitmap == null) {
        throw new IllegalArgumentException("Provided drawable couldn't be converted to a Bitmap.");
      }
      return this.withImage(id, bitmap, sdf, stretchX, stretchY, content);
    }

    /**
     * Will add the drawables as images when the map style has loaded.
     *
     * @param sdf    the flag indicating image is an SDF or template image
     * @param values pairs, where first is the id for te image and second is the drawable
     * @return this
     */
    @NonNull
    public Builder withDrawableImages(boolean sdf, @NonNull Pair<String, Drawable>... values) {
      for (Pair<String, Drawable> value : values) {
        Bitmap bitmap = BitmapUtils.getBitmapFromDrawable(value.second);
        if (bitmap == null) {
          throw new IllegalArgumentException("Provided drawable couldn't be converted to a Bitmap.");
        }
        this.withImage(value.first, bitmap, sdf);
      }
      return this;
    }

    /**
     * Will add the image when the map style has loaded.
     *
     * @param id    the id for the image
     * @param image the image to be added
     * @param sdf   the flag indicating image is an SDF or template image
     * @return this
     */
    @NonNull
    public Builder withImage(@NonNull String id, @NonNull Bitmap image, boolean sdf) {
      images.add(new ImageWrapper(id, image, sdf));
      return this;
    }

    /**
     * Will add the image when the map style has loaded.
     *
     * @param id       the id for the image
     * @param image    the image to be added
     * @param sdf      the flag indicating image is an SDF or template image
     * @param stretchX image stretch areas for x axix
     * @param stretchY image stretch areas for y axix
     * @param content  image content for text to fit
     * @return this
     */
    @NonNull
    public Builder withImage(@NonNull String id, @NonNull Bitmap image, boolean sdf,
                             @NonNull List<ImageStretches> stretchX,
                             @NonNull List<ImageStretches> stretchY, @Nullable ImageContent content) {
      images.add(new ImageWrapper(id, image, sdf, stretchX, stretchY, content));
      return this;
    }

    /**
     * Will add the images when the map style has loaded.
     *
     * @param sdf    the flag indicating image is an SDF or template image
     * @param values pairs, where first is the id for te image and second is the bitmap
     * @return this
     */
    @NonNull
    public Builder withBitmapImages(boolean sdf, @NonNull Pair<String, Bitmap>... values) {
      for (Pair<String, Bitmap> value : values) {
        this.withImage(value.first, value.second, sdf);
      }
      return this;
    }

    public String getUri() {
      return styleUri;
    }

    public String getJson() {
      return styleJson;
    }

    public List<Source> getSources() {
      return sources;
    }

    public List<LayerWrapper> getLayers() {
      return layers;
    }

    public List<ImageWrapper> getImages() {
      return images;
    }

    TransitionOptions getTransitionOptions() {
      return transitionOptions;
    }

    /**
     * Build the composed style.
     */
    Style build(@NonNull NativeMap nativeMap) {
      return new Style(this, nativeMap);
    }

    public static class ImageWrapper {
      Bitmap bitmap;
      String id;
      boolean sdf;
      List<ImageStretches> stretchX;
      List<ImageStretches> stretchY;
      ImageContent content;

      public ImageWrapper(String id, Bitmap bitmap, boolean sdf) {
        this(id, bitmap, sdf, null, null, null);
      }

      public ImageWrapper(String id, Bitmap bitmap, boolean sdf, List<ImageStretches> stretchX,
                          List<ImageStretches> stretchY, ImageContent content) {
        this.id = id;
        this.bitmap = bitmap;
        this.sdf = sdf;
        this.stretchX = stretchX;
        this.stretchY = stretchY;
        this.content = content;
      }

      public Bitmap getBitmap() {
        return bitmap;
      }

      public String getId() {
        return id;
      }

      public boolean isSdf() {
        return sdf;
      }

      public List<ImageStretches> getStretchX() {
        return stretchX;
      }

      public List<ImageStretches> getStretchY() {
        return stretchY;
      }

      public ImageContent getContent() {
        return content;
      }

      public static ImageWrapper[] convertToImageArray(HashMap<String, Bitmap> bitmapHashMap, boolean sdf) {
        ImageWrapper[] images = new ImageWrapper[bitmapHashMap.size()];
        List<String> keyList = new ArrayList<>(bitmapHashMap.keySet());
        for (int i = 0; i < bitmapHashMap.size(); i++) {
          String id = keyList.get(i);
          images[i] = new ImageWrapper(id, bitmapHashMap.get(id), sdf);
        }
        return images;
      }

      public static ImageWrapper[] convertToImageArray(HashMap<String, Bitmap> bitmapHashMap, boolean sdf,
                                                       List<ImageStretches> stretchX,
                                                       List<ImageStretches> stretchY,
                                                       ImageContent content) {
        ImageWrapper[] images = new ImageWrapper[bitmapHashMap.size()];
        List<String> keyList = new ArrayList<>(bitmapHashMap.keySet());
        for (int i = 0; i < bitmapHashMap.size(); i++) {
          String id = keyList.get(i);
          images[i] = new ImageWrapper(id, bitmapHashMap.get(id), sdf, stretchX, stretchY, content);
        }
        return images;
      }
    }

    public class LayerWrapper {
      Layer layer;

      LayerWrapper(Layer layer) {
        this.layer = layer;
      }

      public Layer getLayer() {
        return layer;
      }
    }

    public class LayerAboveWrapper extends LayerWrapper {
      String aboveLayer;

      LayerAboveWrapper(Layer layer, String aboveLayer) {
        super(layer);
        this.aboveLayer = aboveLayer;
      }

      public String getAboveLayer() {
        return aboveLayer;
      }
    }

    public class LayerBelowWrapper extends LayerWrapper {
      String belowLayer;

      LayerBelowWrapper(Layer layer, String belowLayer) {
        super(layer);
        this.belowLayer = belowLayer;
      }

      public String getBelowLayer() {
        return belowLayer;
      }
    }

    public class LayerAtWrapper extends LayerWrapper {
      int index;

      LayerAtWrapper(Layer layer, int index) {
        super(layer);
        this.index = index;
      }

      public int getIndex() {
        return index;
      }
    }
  }

  public static Image toImage(Builder.ImageWrapper imageWrapper) {
    Bitmap bitmap = imageWrapper.bitmap;
    if (bitmap.getConfig() != Bitmap.Config.ARGB_8888) {
      bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);
    }

    ByteBuffer buffer = ByteBuffer.allocate(bitmap.getByteCount());
    bitmap.copyPixelsToBuffer(buffer);
    float pixelRatio = (float) bitmap.getDensity() / DisplayMetrics.DENSITY_DEFAULT;

    if (imageWrapper.getStretchX() != null && imageWrapper.getStretchY() != null) {
      float[] arrayX = new float[imageWrapper.getStretchX().size() * 2];
      for (int i = 0; i < imageWrapper.getStretchX().size(); i++) {
        arrayX[i * 2] = imageWrapper.getStretchX().get(i).getFirst();
        arrayX[i * 2 + 1] = imageWrapper.getStretchX().get(i).getSecond();
      }

      float[] arrayY = new float[imageWrapper.getStretchY().size() * 2];
      for (int i = 0; i < imageWrapper.getStretchY().size(); i++) {
        arrayY[i * 2] = imageWrapper.getStretchY().get(i).getFirst();
        arrayY[i * 2 + 1] = imageWrapper.getStretchY().get(i).getSecond();
      }
      return new Image(buffer.array(), pixelRatio, imageWrapper.id,
        bitmap.getWidth(), bitmap.getHeight(), imageWrapper.sdf, arrayX, arrayY,
        imageWrapper.getContent() == null ? null : imageWrapper.getContent().getContentArray()
      );
    }

    return new Image(buffer.array(), pixelRatio, imageWrapper.id,
      bitmap.getWidth(), bitmap.getHeight(), imageWrapper.sdf
    );
  }

  private static class BitmapImageConversionTask extends AsyncTask<Builder.ImageWrapper, Void, Image[]> {

    private WeakReference<NativeMap> nativeMap;

    BitmapImageConversionTask(NativeMap nativeMap) {
      this.nativeMap = new WeakReference<>(nativeMap);
    }

    @NonNull
    @Override
    protected Image[] doInBackground(Builder.ImageWrapper... params) {
      List<Image> images = new ArrayList<>();
      for (Builder.ImageWrapper param : params) {
        images.add(toImage(param));
      }
      return images.toArray(new Image[images.size()]);
    }

    @Override
    protected void onPostExecute(@NonNull Image[] images) {
      super.onPostExecute(images);
      NativeMap nativeMap = this.nativeMap.get();
      if (nativeMap != null && !nativeMap.isDestroyed()) {
        nativeMap.addImages(images);
      }
    }
  }

  /**
   * Callback to be invoked when a style has finished loading.
   */
  public interface OnStyleLoaded {
    /**
     * Invoked when a style has finished loading.
     *
     * @param style the style that has finished loading
     */
    void onStyleLoaded(@NonNull Style style);
  }

  //
  // Style URL constants
  //

  /**
   * Get predefined styles
   *
   *  @return The list of predefined styles
   */
  public static DefaultStyle[] getPredefinedStyles() {
    return MapLibre.getPredefinedStyles();
  }

  /**
   * Get predefined style by name
   *
   *  @return The predefined style definition
   */
  @NonNull
  public static String getPredefinedStyle(String name) {
    DefaultStyle style = MapLibre.getPredefinedStyle(name);
    if (style != null) {
      return style.getUrl();
    }
    throw new IllegalArgumentException("Could not find layer " + name);
  }
}
