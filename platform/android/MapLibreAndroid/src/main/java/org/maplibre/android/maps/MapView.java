package org.maplibre.android.maps;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.collection.LongSparseArray;
import timber.log.Timber;

import org.maplibre.android.gestures.AndroidGesturesManager;
import org.maplibre.android.MapStrictMode;
import org.maplibre.android.MapLibre;
import org.maplibre.android.R;
import org.maplibre.android.WellKnownTileServer;
import org.maplibre.android.annotations.Annotation;
import org.maplibre.android.constants.MapLibreConstants;
import org.maplibre.android.exceptions.MapLibreConfigurationException;
import org.maplibre.android.location.LocationComponent;
import org.maplibre.android.maps.renderer.MapRenderer;
import org.maplibre.android.maps.widgets.CompassView;
import org.maplibre.android.net.ConnectivityReceiver;
import org.maplibre.android.storage.FileSource;
import org.maplibre.android.utils.BitmapUtils;
import org.maplibre.android.tile.TileOperation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.maplibre.android.maps.widgets.CompassView.TIME_MAP_NORTH_ANIMATION;
import static org.maplibre.android.maps.widgets.CompassView.TIME_WAIT_IDLE;

/**
 * <p>
 * A {@code MapView} provides an embeddable map interface.
 * You use this class to display map information and to manipulate the map contents from your application.
 * You can center the map on a given coordinate, specify the size of the area you want to display,
 * and style the features of the map to fit your application's use case.
 * </p>
 * <p>
 * Use of {@code MapView} requires a MapLibre API access token.
 * Obtain an access token on the <a href="https://www.mapbox.com/studio/account/tokens/">MapLibre account page</a>.
 * </p>
 * <strong>Warning:</strong> Please note that you are responsible for getting permission to use the map data,
 * and for ensuring your use adheres to the relevant terms of use.
 */
public class MapView extends FrameLayout implements NativeMapView.ViewCallback {

  private final MapChangeReceiver mapChangeReceiver = new MapChangeReceiver();
  private final MapCallback mapCallback = new MapCallback();
  private final InitialRenderCallback initialRenderCallback = new InitialRenderCallback();

  @Nullable
  private NativeMap nativeMapView;
  @Nullable
  private MapLibreMap maplibreMap;
  private View renderView;

  private AttributionClickListener attributionClickListener;
  MapLibreMapOptions maplibreMapOptions;
  private MapRenderer mapRenderer;
  private boolean destroyed;

  @Nullable
  private CompassView compassView;
  private PointF focalPoint;

  // callback for focal point invalidation
  private final FocalPointInvalidator focalInvalidator = new FocalPointInvalidator();
  // callback for registering touch listeners
  private final GesturesManagerInteractionListener registerTouchListener = new GesturesManagerInteractionListener();
  // callback for camera change events
  private final CameraChangeDispatcher cameraDispatcher = new CameraChangeDispatcher();

  @Nullable
  private MapGestureDetector mapGestureDetector;
  @Nullable
  private MapKeyListener mapKeyListener;
  @Nullable
  private Bundle savedInstanceState;
  private boolean isStarted;

  @UiThread
  public MapView(@NonNull Context context) {
    super(context);
    Timber.d("MapView constructed with context");
    initialize(context, MapLibreMapOptions.createFromAttributes(context));
  }

  @UiThread
  public MapView(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    Timber.d("MapView constructed with context and attribute set");
    initialize(context, MapLibreMapOptions.createFromAttributes(context, attrs));
  }

  @UiThread
  public MapView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    Timber.d( "MapView constructed with context, attributeSet and defStyleAttr");
    initialize(context, MapLibreMapOptions.createFromAttributes(context, attrs));
  }

  @UiThread
  public MapView(@NonNull Context context, @Nullable MapLibreMapOptions options) {
    super(context);
    Timber.d("MapView constructed with context and MapLibreMapOptions");
    initialize(context, options == null ? MapLibreMapOptions.createFromAttributes(context) : options);
  }

  @CallSuper
  @UiThread
  protected void initialize(@NonNull final Context context, @NonNull final MapLibreMapOptions options) {
    if (isInEditMode()) {
      // in IDE layout editor, just return
      return;
    }

    if (!MapLibre.hasInstance()) {
      throw new MapLibreConfigurationException();
    }

    // hide surface until map is fully loaded #10990
    setForeground(new ColorDrawable(options.getForegroundLoadColor()));

    maplibreMapOptions = options;

    // add accessibility support
    setContentDescription(context.getString(R.string.maplibre_mapActionDescription));
    setWillNotDraw(false);
    initializeDrawingSurface(options);
  }

  private void initializeMap() {
    Context context = getContext();

    // callback for focal point invalidation
    focalInvalidator.addListener(createFocalPointChangeListener());

    // setup components for MapLibreMap creation
    Projection proj = new Projection(nativeMapView, this);
    UiSettings uiSettings = new UiSettings(proj, focalInvalidator, getPixelRatio(), this);
    LongSparseArray<Annotation> annotationsArray = new LongSparseArray<>();
    IconManager iconManager = new IconManager(nativeMapView);
    Annotations annotations = new AnnotationContainer(nativeMapView, annotationsArray);
    Markers markers = new MarkerContainer(nativeMapView, annotationsArray, iconManager);
    Polygons polygons = new PolygonContainer(nativeMapView, annotationsArray);
    Polylines polylines = new PolylineContainer(nativeMapView, annotationsArray);
    ShapeAnnotations shapeAnnotations = new ShapeAnnotationContainer(nativeMapView, annotationsArray);
    AnnotationManager annotationManager = new AnnotationManager(this, annotationsArray, iconManager,
            annotations, markers, polygons, polylines, shapeAnnotations);
    Transform transform = new Transform(this, nativeMapView, cameraDispatcher);

    // MapLibreMap
    List<MapLibreMap.OnDeveloperAnimationListener> developerAnimationListeners = new ArrayList<>();
    maplibreMap = new MapLibreMap(nativeMapView, transform, uiSettings, proj, registerTouchListener, cameraDispatcher,
            developerAnimationListeners);
    maplibreMap.injectAnnotationManager(annotationManager);

    // user input
    mapGestureDetector = new MapGestureDetector(context, transform, proj, uiSettings,
            annotationManager, cameraDispatcher);
    mapKeyListener = new MapKeyListener(transform, uiSettings, mapGestureDetector);

    // LocationComponent
    maplibreMap.injectLocationComponent(new LocationComponent(maplibreMap, transform, developerAnimationListeners));

    // Ensure this view is interactable
    setClickable(true);
    setLongClickable(true);
    setFocusable(true);
    setFocusableInTouchMode(true);
    requestDisallowInterceptTouchEvent(true);

    // notify Map object about current connectivity state
    nativeMapView.setReachability(MapLibre.isConnected());

    // initialise MapLibreMap
    if (savedInstanceState == null) {
      maplibreMap.initialise(context, maplibreMapOptions);
    } else {
      maplibreMap.onRestoreInstanceState(savedInstanceState);
    }

    mapCallback.initialised();
  }

  protected CompassView initialiseCompassView() {
    compassView = new CompassView(this.getContext());
    addView(compassView);
    compassView.setTag("compassView");
    compassView.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
    compassView.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
    compassView.setContentDescription(getResources().getString(R.string.maplibre_compassContentDescription));
    compassView.injectCompassAnimationListener(createCompassAnimationListener(cameraDispatcher));
    compassView.setOnClickListener(createCompassClickListener(cameraDispatcher));
    return compassView;
  }

  protected ImageView initialiseAttributionView() {
    ImageView attrView = new ImageView(this.getContext());
    addView(attrView);
    attrView.setTag("attrView");
    attrView.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
    attrView.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
    attrView.setAdjustViewBounds(true);
    attrView.setClickable(true);
    attrView.setFocusable(true);
    attrView.setContentDescription(getResources().getString(R.string.maplibre_attributionsIconContentDescription));
    attrView.setImageDrawable(BitmapUtils.getDrawableFromRes(getContext(), R.drawable.maplibre_info_bg_selector));
    // inject widgets with MapLibreMap
    attrView.setOnClickListener(attributionClickListener = new AttributionClickListener(getContext(), maplibreMap));
    return attrView;
  }

  protected ImageView initialiseLogoView() {
    ImageView logoView = new ImageView(this.getContext());
    addView(logoView);
    logoView.setTag("logoView");
    logoView.getLayoutParams().width = LayoutParams.WRAP_CONTENT;
    logoView.getLayoutParams().height = LayoutParams.WRAP_CONTENT;
    logoView.setImageDrawable(BitmapUtils.getDrawableFromRes(getContext(), R.drawable.maplibre_logo_icon));
    return logoView;
  }

  private FocalPointChangeListener createFocalPointChangeListener() {
    return new FocalPointChangeListener() {
      @Override
      public void onFocalPointChanged(PointF pointF) {
        focalPoint = pointF;
      }
    };
  }

  private MapLibreMap.OnCompassAnimationListener createCompassAnimationListener(@NonNull final CameraChangeDispatcher
                                                                                      cameraChangeDispatcher) {
    return new MapLibreMap.OnCompassAnimationListener() {
      @Override
      public void onCompassAnimation() {
        cameraChangeDispatcher.onCameraMove();
      }

      @Override
      public void onCompassAnimationFinished() {
        if (compassView != null) {
          compassView.isAnimating(false);
        }
        cameraChangeDispatcher.onCameraIdle();
      }
    };
  }

  private OnClickListener createCompassClickListener(@NonNull final CameraChangeDispatcher cameraChangeDispatcher) {
    return new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (maplibreMap != null && compassView != null) {
          if (focalPoint != null) {
            maplibreMap.setFocalBearing(0, focalPoint.x, focalPoint.y, TIME_MAP_NORTH_ANIMATION);
          } else {
            maplibreMap.setFocalBearing(
              0, maplibreMap.getWidth() / 2, maplibreMap.getHeight() / 2,
              TIME_MAP_NORTH_ANIMATION);
          }
          cameraChangeDispatcher.onCameraMoveStarted(MapLibreMap.OnCameraMoveStartedListener.REASON_API_ANIMATION);
          compassView.isAnimating(true);
          compassView.postDelayed(compassView, TIME_WAIT_IDLE + TIME_MAP_NORTH_ANIMATION);
        }
      }
    };
  }

  //
  // Lifecycle events
  //

  /**
   * <p>
   * You must call this method from the parent's Activity#onCreate(Bundle)} or
   * Fragment#onViewCreated(View, Bundle).
   * </p>
   * You must set a valid access token with
   * {@link MapLibre#getInstance(Context, String, WellKnownTileServer)}
   * before you call this method or an exception will be thrown.
   *
   * @param savedInstanceState Pass in the parent's savedInstanceState.
   * @see MapLibre#getInstance(Context, String, WellKnownTileServer)
   */
  @UiThread
  public void onCreate(@Nullable Bundle savedInstanceState) {
    if (savedInstanceState != null && savedInstanceState.getBoolean(MapLibreConstants.STATE_HAS_SAVED_STATE)) {
      this.savedInstanceState = savedInstanceState;
    }
  }

  private void initializeDrawingSurface(MapLibreMapOptions options) {
    mapRenderer = MapRenderer.create(options, getContext(), MapView.this::onSurfaceCreated);
    renderView = mapRenderer.getView();

    addView(renderView, 0);

    options.pixelRatio(getPixelRatio());
    nativeMapView = new NativeMapView(getContext(), options, this, mapChangeReceiver, mapRenderer);
  }

  private void onSurfaceCreated() {
    post(new Runnable() {
      @Override
      public void run() {
        // Initialize only when not destroyed and only once
        if (!destroyed && maplibreMap == null) {
          MapView.this.initializeMap();
          maplibreMap.onStart();
        }
      }
    });
  }

  /**
   * You must call this method from the parent's Activity#onSaveInstanceState(Bundle)
   * or Fragment#onSaveInstanceState(Bundle).
   *
   * @param outState Pass in the parent's outState.
   */
  @UiThread
  public void onSaveInstanceState(@NonNull Bundle outState) {
    if (maplibreMap != null) {
      outState.putBoolean(MapLibreConstants.STATE_HAS_SAVED_STATE, true);
      maplibreMap.onSaveInstanceState(outState);
    }
  }

  /**
   * You must call this method from the parent's Activity#onStart() or Fragment#onStart()
   */
  @UiThread
  public void onStart() {
    if (!isStarted) {
      ConnectivityReceiver.instance(getContext()).activate();
      FileSource.getInstance(getContext()).activate();
      isStarted = true;
    }
    if (maplibreMap != null) {
      maplibreMap.onStart();
    }

    if (mapRenderer != null) {
      mapRenderer.onStart();
    }
  }

  /**
   * You must call this method from the parent's Activity#onResume() or Fragment#onResume().
   */
  @UiThread
  public void onResume() {
    if (mapRenderer != null) {
      mapRenderer.onResume();
    }
  }

  /**
   * You must call this method from the parent's Activity#onPause() or Fragment#onPause().
   */
  @UiThread
  public void onPause() {
    if (mapRenderer != null) {
      mapRenderer.onPause();
    }
  }

  /**
   * You must call this method from the parent's Activity#onStop() or Fragment#onStop().
   */
  @UiThread
  public void onStop() {
    if (attributionClickListener != null) {
      attributionClickListener.onStop();
    }

    if (maplibreMap != null) {
      // map was destroyed before it was started
      mapGestureDetector.cancelAnimators();
      maplibreMap.onStop();
    }

    if (mapRenderer != null) {
      mapRenderer.onStop();
    }

    if (isStarted) {
      ConnectivityReceiver.instance(getContext()).deactivate();
      FileSource.getInstance(getContext()).deactivate();
      isStarted = false;
    }
  }

  /**
   * You must call this method from the parent's Activity#onDestroy() or Fragment#onDestroyView().
   */
  @UiThread
  public void onDestroy() {
    destroyed = true;
    mapChangeReceiver.clear();
    mapCallback.onDestroy();
    initialRenderCallback.onDestroy();

    if (compassView != null) {
      // avoid leaking context through animator #13742
      compassView.resetAnimation();
    }

    if (maplibreMap != null) {
      maplibreMap.onDestroy();
    }

    if (nativeMapView != null) {
      // null when destroying an activity programmatically mapbox-navigation-android/issues/503
      nativeMapView.destroy();
      nativeMapView = null;
    }

    if (mapRenderer != null) {
      mapRenderer.onDestroy();
    }
  }

  /**
   * Queue a runnable to be executed on the map renderer thread.
   *
   * @param runnable the runnable to queue
   */
  public void queueEvent(@NonNull Runnable runnable) {
    if (mapRenderer == null) {
      throw new IllegalStateException("Calling MapView#queueEvent before mapRenderer is created.");
    }
    mapRenderer.queueEvent(runnable);
  }

  /**
   * The maximum frame rate at which the map view is rendered,
   * but it can't excess the ability of device hardware.
   *
   * @param maximumFps Can be set to arbitrary integer values.
   */
  public void setMaximumFps(int maximumFps) {
    if (mapRenderer != null) {
      mapRenderer.setMaximumFps(maximumFps);
    } else {
      throw new IllegalStateException("Calling MapView#setMaximumFps before mapRenderer is created.");
    }
  }

  /**
   * Set the rendering refresh mode and wake up the render thread if it is sleeping.
   *
   * @param mode can be:
   * {@link MapRenderer.RenderingRefreshMode#CONTINUOUS} or {@link MapRenderer.RenderingRefreshMode#WHEN_DIRTY}
   * default is {@link MapRenderer.RenderingRefreshMode#WHEN_DIRTY}
   */
  public void setRenderingRefreshMode(MapRenderer.RenderingRefreshMode mode) {
    if (mapRenderer != null) {
      mapRenderer.setRenderingRefreshMode(mode);
    } else {
      throw new IllegalStateException("Calling MapView#setRenderingRefreshMode before mapRenderer is created.");
    }
  }

  /**
   * Get the rendering refresh mode
   *
   * @return one of the MapRenderer.RenderingRefreshMode modes
   * @see #setRenderingRefreshMode
   */
  public MapRenderer.RenderingRefreshMode getRenderingRefreshMode() {
    if (mapRenderer == null) {
      throw new IllegalStateException("Calling MapView#getRenderingRefreshMode before mapRenderer is created.");
    }
    return mapRenderer.getRenderingRefreshMode();
  }

  /**
   * Returns if the map has been destroyed.
   * <p>
   * This method can be used to determine if the result of an asynchronous operation should be set.
   * </p>
   *
   * @return true, if the map has been destroyed
   */
  @UiThread
  public boolean isDestroyed() {
    return destroyed;
  }

  /**
   * Returns the View used for rendering OpenGL.
   * <p>
   * The type of the returned view is either a GLSurfaceView or a TextureView.
   * </p>
   *
   * @return the view used for rendering OpenGL
   */
  @NonNull
  @UiThread
  public View getRenderView() {
    return renderView;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (!isGestureDetectorInitialized()) {
      return super.onTouchEvent(event);
    }

    return mapGestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
  }

  @Override
  public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
    if (!isKeyDetectorInitialized()) {
      return super.onKeyDown(keyCode, event);
    }
    return mapKeyListener.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyLongPress(int keyCode, KeyEvent event) {
    if (!isKeyDetectorInitialized()) {
      return super.onKeyLongPress(keyCode, event);
    }
    return mapKeyListener.onKeyLongPress(keyCode, event) || super.onKeyLongPress(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
    if (!isKeyDetectorInitialized()) {
      return super.onKeyUp(keyCode, event);
    }
    return mapKeyListener.onKeyUp(keyCode, event) || super.onKeyUp(keyCode, event);
  }

  @Override
  public boolean onTrackballEvent(@NonNull MotionEvent event) {
    if (!isKeyDetectorInitialized()) {
      return super.onTrackballEvent(event);
    }
    return mapKeyListener.onTrackballEvent(event) || super.onTrackballEvent(event);
  }

  @Override
  public boolean onGenericMotionEvent(@NonNull MotionEvent event) {
    if (!isGestureDetectorInitialized()) {
      return super.onGenericMotionEvent(event);
    }
    return mapGestureDetector.onGenericMotionEvent(event) || super.onGenericMotionEvent(event);
  }

  /**
   * You must call this method from the parent's Activity#onLowMemory() or Fragment#onLowMemory().
   */
  @UiThread
  public void onLowMemory() {
    if (nativeMapView != null && maplibreMap != null && !destroyed) {
      nativeMapView.onLowMemory();
    }
  }

  //
  // Rendering
  //

  @Override
  protected void onSizeChanged(int width, int height, int oldw, int oldh) {
    if (!isInEditMode() && nativeMapView != null) {
      // null-checking the nativeMapView, see #13277
      nativeMapView.resizeView(width, height);
    }
  }

  /**
   * Returns the map pixel ratio, by default it returns the device pixel ratio.
   * Can be overwritten using {@link MapLibreMapOptions#pixelRatio(float)}.
   *
   * @return the current map pixel ratio
   */
  public float getPixelRatio() {
    // check is user defined his own pixel ratio value
    float pixelRatio = maplibreMapOptions.getPixelRatio();
    if (pixelRatio == 0) {
      // if not, get the one defined by the system
      pixelRatio = getResources().getDisplayMetrics().density;
    }
    return pixelRatio;
  }

  //
  // ViewCallback
  //

  @Nullable
  @Override
  public Bitmap getViewContent() {
    return BitmapUtils.createBitmapFromView(this);
  }

  //
  // Map events
  //

  /**
   * Set a callback that's invoked when the camera region will change.
   *
   * @param listener The callback that's invoked when the camera region will change
   */
  public void addOnCameraWillChangeListener(@NonNull OnCameraWillChangeListener listener) {
    mapChangeReceiver.addOnCameraWillChangeListener(listener);
  }

  /**
   * Remove a callback that's invoked when the camera region will change.
   *
   * @param listener The callback that's invoked when the camera region will change
   */
  public void removeOnCameraWillChangeListener(@NonNull OnCameraWillChangeListener listener) {
    mapChangeReceiver.removeOnCameraWillChangeListener(listener);
  }

  /**
   * Set a callback that's invoked when the camera is changing.
   *
   * @param listener The callback that's invoked when the camera is changing
   */
  public void addOnCameraIsChangingListener(@NonNull OnCameraIsChangingListener listener) {
    mapChangeReceiver.addOnCameraIsChangingListener(listener);
  }

  /**
   * Remove a callback that's invoked when the camera is changing.
   *
   * @param listener The callback that's invoked when the camera is changing
   */
  public void removeOnCameraIsChangingListener(@NonNull OnCameraIsChangingListener listener) {
    mapChangeReceiver.removeOnCameraIsChangingListener(listener);
  }

  /**
   * Set a callback that's invoked when the camera region did change.
   *
   * @param listener The callback that's invoked when the camera region did change
   */
  public void addOnCameraDidChangeListener(@NonNull OnCameraDidChangeListener listener) {
    mapChangeReceiver.addOnCameraDidChangeListener(listener);
  }

  /**
   * Set a callback that's invoked when the camera region did change.
   *
   * @param listener The callback that's invoked when the camera region did change
   */
  public void removeOnCameraDidChangeListener(@NonNull OnCameraDidChangeListener listener) {
    mapChangeReceiver.removeOnCameraDidChangeListener(listener);
  }

  /**
   * Set a callback that's invoked when the map will start loading.
   *
   * @param listener The callback that's invoked when the map will start loading
   */
  public void addOnWillStartLoadingMapListener(@NonNull OnWillStartLoadingMapListener listener) {
    mapChangeReceiver.addOnWillStartLoadingMapListener(listener);
  }

  /**
   * Set a callback that's invoked when the map will start loading.
   *
   * @param listener The callback that's invoked when the map will start loading
   */
  public void removeOnWillStartLoadingMapListener(@NonNull OnWillStartLoadingMapListener listener) {
    mapChangeReceiver.removeOnWillStartLoadingMapListener(listener);
  }

  /**
   * Set a callback that's invoked when the map has finished loading.
   *
   * @param listener The callback that's invoked when the map has finished loading
   */
  public void addOnDidFinishLoadingMapListener(@NonNull OnDidFinishLoadingMapListener listener) {
    mapChangeReceiver.addOnDidFinishLoadingMapListener(listener);
  }

  /**
   * Set a callback that's invoked when the map has finished loading.
   *
   * @param listener The callback that's invoked when the map has finished loading
   */
  public void removeOnDidFinishLoadingMapListener(@NonNull OnDidFinishLoadingMapListener listener) {
    mapChangeReceiver.removeOnDidFinishLoadingMapListener(listener);
  }

  /**
   * Set a callback that's invoked when the map failed to load.
   *
   * @param listener The callback that's invoked when the map failed to load
   */
  public void addOnDidFailLoadingMapListener(@NonNull OnDidFailLoadingMapListener listener) {
    mapChangeReceiver.addOnDidFailLoadingMapListener(listener);
  }

  /**
   * Set a callback that's invoked when the map failed to load.
   *
   * @param listener The callback that's invoked when the map failed to load
   */
  public void removeOnDidFailLoadingMapListener(@NonNull OnDidFailLoadingMapListener listener) {
    mapChangeReceiver.removeOnDidFailLoadingMapListener(listener);
  }

  /**
   * Set a callback that's invoked when the map will start rendering a frame.
   *
   * @param listener The callback that's invoked when the camera will start rendering a frame
   */
  public void addOnWillStartRenderingFrameListener(@NonNull OnWillStartRenderingFrameListener listener) {
    mapChangeReceiver.addOnWillStartRenderingFrameListener(listener);
  }

  /**
   * Set a callback that's invoked when the map will start rendering a frame.
   *
   * @param listener The callback that's invoked when the camera will start rendering a frame
   */
  public void removeOnWillStartRenderingFrameListener(@NonNull OnWillStartRenderingFrameListener listener) {
    mapChangeReceiver.removeOnWillStartRenderingFrameListener(listener);
  }

  /**
   * Set a callback that's invoked when the map has finished rendering a frame.
   *
   * @param listener The callback that's invoked when the map has finished rendering a frame
   */
  public void addOnDidFinishRenderingFrameListener(@NonNull OnDidFinishRenderingFrameListener listener) {
    mapChangeReceiver.addOnDidFinishRenderingFrameListener(listener);
  }

  /**
   * Set a callback that's invoked when the map has finished rendering a frame.
   *
   * @param listener The callback that's invoked when the map has finished rendering a frame
   */
  public void removeOnDidFinishRenderingFrameListener(@NonNull OnDidFinishRenderingFrameListener listener) {
    mapChangeReceiver.removeOnDidFinishRenderingFrameListener(listener);
  }

  /**
   * Set a callback that's invoked when the map will start rendering.
   *
   * @param listener The callback that's invoked when the map will start rendering
   */
  public void addOnWillStartRenderingMapListener(@NonNull OnWillStartRenderingMapListener listener) {
    mapChangeReceiver.addOnWillStartRenderingMapListener(listener);
  }

  /**
   * Set a callback that's invoked when the map will start rendering.
   *
   * @param listener The callback that's invoked when the map will start rendering
   */
  public void removeOnWillStartRenderingMapListener(@NonNull OnWillStartRenderingMapListener listener) {
    mapChangeReceiver.removeOnWillStartRenderingMapListener(listener);
  }

  /**
   * Set a callback that's invoked when the map has finished rendering.
   *
   * @param listener The callback that's invoked when the map has finished rendering
   */
  public void addOnDidFinishRenderingMapListener(@NonNull OnDidFinishRenderingMapListener listener) {
    mapChangeReceiver.addOnDidFinishRenderingMapListener(listener);
  }

  /**
   * Remove a callback that's invoked when the map has finished rendering.
   *
   * @param listener The callback that's invoked when the map has has finished rendering.
   */
  public void removeOnDidFinishRenderingMapListener(OnDidFinishRenderingMapListener listener) {
    mapChangeReceiver.removeOnDidFinishRenderingMapListener(listener);
  }

  /**
   * Set a callback that's invoked when the map has entered the idle state.
   *
   * @param listener The callback that's invoked when the map has entered the idle state.
   */
  public void addOnDidBecomeIdleListener(@NonNull OnDidBecomeIdleListener listener) {
    mapChangeReceiver.addOnDidBecomeIdleListener(listener);
  }

  /**
   * Remove a callback that's invoked when the map has entered the idle state.
   *
   * @param listener The callback that's invoked when the map has entered the idle state.
   */
  public void removeOnDidBecomeIdleListener(@NonNull OnDidBecomeIdleListener listener) {
    mapChangeReceiver.removeOnDidBecomeIdleListener(listener);
  }

  /**
   * Set a callback that's invoked when the style has finished loading.
   *
   * @param listener The callback that's invoked when the style has finished loading
   */
  public void addOnDidFinishLoadingStyleListener(@NonNull OnDidFinishLoadingStyleListener listener) {
    mapChangeReceiver.addOnDidFinishLoadingStyleListener(listener);
  }

  /**
   * Set a callback that's invoked when the style has finished loading.
   *
   * @param listener The callback that's invoked when the style has finished loading
   */
  public void removeOnDidFinishLoadingStyleListener(@NonNull OnDidFinishLoadingStyleListener listener) {
    mapChangeReceiver.removeOnDidFinishLoadingStyleListener(listener);
  }

  /**
   * Set a callback that's invoked when a map source has changed.
   *
   * @param listener The callback that's invoked when the source has changed
   */
  public void addOnSourceChangedListener(@NonNull OnSourceChangedListener listener) {
    mapChangeReceiver.addOnSourceChangedListener(listener);
  }

  /**
   * Set a callback that's invoked when a map source has changed.
   *
   * @param listener The callback that's invoked when the source has changed
   */
  public void removeOnSourceChangedListener(@NonNull OnSourceChangedListener listener) {
    mapChangeReceiver.removeOnSourceChangedListener(listener);
  }

  /**
   * Set a callback that's invoked when the id of an icon is missing.
   *
   * @param listener The callback that's invoked when the id of an icon is missing
   */
  public void addOnStyleImageMissingListener(@NonNull OnStyleImageMissingListener listener) {
    mapChangeReceiver.addOnStyleImageMissingListener(listener);
  }

  /**
   * Set a callback that's invoked when a map source has changed.
   *
   * @param listener The callback that's invoked when the source has changed
   */
  public void removeOnStyleImageMissingListener(@NonNull OnStyleImageMissingListener listener) {
    mapChangeReceiver.removeOnStyleImageMissingListener(listener);
  }

  /**
   * Set a callback that's invoked when map needs to release unused image resources.
   * <p>
   * A callback will be called only for unused images that were provided by the client via
   * {@link OnStyleImageMissingListener#onStyleImageMissing(String)} listener interface.
   * </p>
   * <p>
   * By default, platform will remove unused images from the style. By adding listener, default
   * behavior can be overridden and client can control whether to release unused resources.
   * </p>
   *
   * @param listener The callback that's invoked when map needs to release unused image resources
   */
  public void addOnCanRemoveUnusedStyleImageListener(@NonNull OnCanRemoveUnusedStyleImageListener listener) {
    mapChangeReceiver.addOnCanRemoveUnusedStyleImageListener(listener);
  }

  /**
   * Removes a callback that's invoked when map needs to release unused image resources.
   * <p>
   * When all listeners are removed, platform will fallback to default behavior, which is to remove
   * unused images from the style.
   * </p>
   *
   * @param listener The callback that's invoked when map needs to release unused image resources
   */
  public void removeOnCanRemoveUnusedStyleImageListener(@NonNull OnCanRemoveUnusedStyleImageListener listener) {
    mapChangeReceiver.removeOnCanRemoveUnusedStyleImageListener(listener);
  }

  /**
   * Set a callback that's invoked before a shader is compiled.
   *
   * @param listener The callback that's invoked before a shader is compiled
   */
  public void addOnPreCompileShaderListener(MapView.OnPreCompileShaderListener callback) {
    mapChangeReceiver.addOnPreCompileShaderListener(callback);
  }

  /**
   * Removes a callback that's invoked before a shader is compiled.
   *
   * @param listener The callback that's invoked before a shader is compiled
   */
  public void removeOnPreCompileShaderListener(MapView.OnPreCompileShaderListener callback) {
    mapChangeReceiver.removeOnPreCompileShaderListener(callback);
  }

  /**
   * Set a callback that's invoked after a shader is compiled.
   *
   * @param listener The callback that's invoked after a shader is compiled
   */
  public void addOnPostCompileShaderListener(MapView.OnPostCompileShaderListener callback) {
    mapChangeReceiver.addOnPostCompileShaderListener(callback);
  }

  /**
   * Removes a callback that's invoked after a shader is compiled.
   *
   * @param listener The callback that's invoked after a shader is compiled
   */
  public void removeOnPostCompileShaderListener(MapView.OnPostCompileShaderListener callback) {
    mapChangeReceiver.removeOnPostCompileShaderListener(callback);
  }

  /**
   * Set a callback that's invoked after a shader failed to compile.
   *
   * @param listener The callback that's invoked after a shader failes to compile
   */
  public void addOnShaderCompileFailedListener(MapView.OnShaderCompileFailedListener callback) {
    mapChangeReceiver.addOnShaderCompileFailedListener(callback);
  }

  /**
   * Removes a callback that's invoked after a shader failed to compile.
   *
   * @param listener The callback that's invoked after a shader failes to compile
   */
  public void removeOnShaderCompileFailedListener(MapView.OnShaderCompileFailedListener callback) {
    mapChangeReceiver.removeOnShaderCompileFailedListener(callback);
  }

  /**
   * Set a callback that's invoked after a range of glyphs are loaded.
   *
   * @param listener The callback that's invoked after a range of glyphs are loaded
   */
  public void addOnGlyphsLoadedListener(MapView.OnGlyphsLoadedListener callback) {
    mapChangeReceiver.addOnGlyphsLoadedListener(callback);
  }

  /**
   * Removes a callback that's invoked after a range of glyphs are loaded.
   *
   * @param listener The callback that's invoked after a range of glyphs are loaded
   */
  public void removeOnGlyphsLoadedListener(MapView.OnGlyphsLoadedListener callback) {
    mapChangeReceiver.removeOnGlyphsLoadedListener(callback);
  }

  /**
   * Set a callback that's invoked after a range of glyphs fail to load.
   *
   * @param listener The callback that's invoked after a range of glyphs fail to load
   */
  public void addOnGlyphsErrorListener(MapView.OnGlyphsErrorListener callback) {
    mapChangeReceiver.addOnGlyphsErrorListener(callback);
  }

  /**
   * Removes a callback that's invoked after a range of glyphs fail to load.
   *
   * @param listener The callback that's invoked after a range of glyphs fail to load
   */
  public void removeOnGlyphsErrorListener(MapView.OnGlyphsErrorListener callback) {
    mapChangeReceiver.removeOnGlyphsErrorListener(callback);
  }

  /**
   * Set a callback that's invoked after a range of glyphs are requested.
   *
   * @param listener The callback that's invoked after a range of glyphs are requested
   */
  public void addOnGlyphsRequestedListener(MapView.OnGlyphsRequestedListener callback) {
    mapChangeReceiver.addOnGlyphsRequestedListener(callback);
  }

  /**
   * Removes a callback that's invoked after a range of glyphs are requested.
   *
   * @param listener The callback that's invoked after a range of glyphs are requested
   */
  public void removeOnGlyphsRequestedListener(MapView.OnGlyphsRequestedListener callback) {
    mapChangeReceiver.removeOnGlyphsRequestedListener(callback);
  }

  /**
   * Set a callback that's invoked after a tile action occurs.
   *
   * @param listener The callback that's invoked after a tile action occurs
   */
  public void addOnTileActionListener(MapView.OnTileActionListener callback) {
    mapChangeReceiver.addOnTileActionListener(callback);
  }

  /**
   * Remove's a callback that's invoked after a tile action occurs.
   *
   * @param listener The callback that's invoked after a tile action occurs
   */
  public void removeOnTileActionListener(MapView.OnTileActionListener callback) {
    mapChangeReceiver.removeOnTileActionListener(callback);
  }

  /**
   * Set a callback that's invoked after a sprite is loaded.
   *
   * @param listener The callback that's invoked after a sprite is loaded
   */
  public void addOnSpriteLoadedListener(MapView.OnSpriteLoadedListener callback) {
    mapChangeReceiver.addOnSpriteLoadedListener(callback);
  }

  /**
   * Removes a callback that's invoked after a sprite is loaded.
   *
   * @param listener The callback that's invoked after a sprite is loaded
   */
  public void removeOnSpriteLoadedListener(MapView.OnSpriteLoadedListener callback) {
    mapChangeReceiver.removeOnSpriteLoadedListener(callback);
  }

  /**
   * Set a callback that's invoked after a sprite fails to laod.
   *
   * @param listener The callback that's invoked after a sprite fails to load
   */
  public void addOnSpriteErrorListener(MapView.OnSpriteErrorListener callback) {
    mapChangeReceiver.addOnSpriteErrorListener(callback);
  }

  /**
   * Removes a callback that's invoked after a sprite fails to laod.
   *
   * @param listener The callback that's invoked after a sprite fails to load
   */
  public void removeOnSpriteErrorListener(MapView.OnSpriteErrorListener callback) {
    mapChangeReceiver.removeOnSpriteErrorListener(callback);
  }

  /**
   * Set a callback that's invoked after a sprite is requested.
   *
   * @param listener The callback that's invoked after a sprite is requested
   */
  public void addOnSpriteRequestedListener(MapView.OnSpriteRequestedListener callback) {
    mapChangeReceiver.addOnSpriteRequestedListener(callback);
  }

  /**
   * Removes a callback that's invoked after a sprite is requested.
   *
   * @param listener The callback that's invoked after a sprite is requested
   */
  public void removeOnSpriteRequestedListener(MapView.OnSpriteRequestedListener callback) {
    mapChangeReceiver.removeOnSpriteRequestedListener(callback);
  }

  /**
   * Interface definition for a callback to be invoked when the camera will change.
   * <p>
   * {@link MapView#addOnCameraWillChangeListener(OnCameraWillChangeListener)}
   * </p>
   */
  public interface OnCameraWillChangeListener {

    /**
     * Called when the camera region will change.
     */
    void onCameraWillChange(boolean animated);
  }

  /**
   * Interface definition for a callback to be invoked when the camera is changing.
   * <p>
   * {@link MapView#addOnCameraIsChangingListener(OnCameraIsChangingListener)}
   * </p>
   */
  public interface OnCameraIsChangingListener {
    /**
     * Called when the camera is changing.
     */
    void onCameraIsChanging();
  }

  /**
   * Interface definition for a callback to be invoked when the map region did change.
   * <p>
   * {@link MapView#addOnCameraDidChangeListener(OnCameraDidChangeListener)}
   * </p>
   */
  public interface OnCameraDidChangeListener {
    /**
     * Called when the camera did change.
     */
    void onCameraDidChange(boolean animated);
  }

  /**
   * Interface definition for a callback to be invoked when the map will start loading.
   * <p>
   * {@link MapView#addOnWillStartLoadingMapListener(OnWillStartLoadingMapListener)}
   * </p>
   */
  public interface OnWillStartLoadingMapListener {
    /**
     * Called when the map will start loading.
     */
    void onWillStartLoadingMap();
  }

  /**
   * Interface definition for a callback to be invoked when the map finished loading.
   * <p>
   * {@link MapView#addOnDidFinishLoadingMapListener(OnDidFinishLoadingMapListener)}
   * </p>
   */
  public interface OnDidFinishLoadingMapListener {
    /**
     * Called when the map has finished loading.
     */
    void onDidFinishLoadingMap();
  }

  /**
   * Interface definition for a callback to be invoked when the map is changing.
   * <p>
   * {@link MapView#addOnDidFailLoadingMapListener(OnDidFailLoadingMapListener)}
   * </p>
   */
  public interface OnDidFailLoadingMapListener {
    /**
     * Called when the map failed to load.
     *
     * @param errorMessage The reason why the map failed to load
     */
    void onDidFailLoadingMap(String errorMessage);
  }

  /**
   * Interface definition for a callback to be invoked when the map will start rendering a frame.
   * <p>
   * {@link MapView#addOnWillStartRenderingFrameListener(OnWillStartRenderingFrameListener)}
   * </p>
   */
  public interface OnWillStartRenderingFrameListener {
    /**
     * Called when the map will start rendering a frame.
     */
    void onWillStartRenderingFrame();
  }

  /**
   * Interface definition for a callback to be invoked when the map finished rendering a frame.
   * <p>
   * {@link MapView#addOnDidFinishRenderingFrameListener(OnDidFinishRenderingFrameListener)}
   * </p>
   */
  public interface OnDidFinishRenderingFrameListener {
    /**
     * Called when the map has finished rendering a frame
     *
     * @param fully true if all frames have been rendered, false if partially rendered
     */
    void onDidFinishRenderingFrame(boolean fully, double frameEncodingTime, double frameRenderingTime);
  }

  /**
   * Interface definition for a callback to be invoked when the map will start rendering the map.
   * <p>
   * {@link MapView#addOnDidFailLoadingMapListener(OnDidFailLoadingMapListener)}
   * </p>
   */
  public interface OnWillStartRenderingMapListener {
    /**
     * Called when the map will start rendering.
     */
    void onWillStartRenderingMap();
  }

  /**
   * Interface definition for a callback to be invoked when the map is changing.
   * <p>
   * {@link MapView#addOnDidFinishRenderingMapListener(OnDidFinishRenderingMapListener)}
   * </p>
   */
  public interface OnDidFinishRenderingMapListener {
    /**
     * Called when the map has finished rendering.
     *
     * @param fully true if map is fully rendered, false if not fully rendered
     */
    void onDidFinishRenderingMap(boolean fully);
  }

  /**
   * Interface definition for a callback to be invoked when the map has entered the idle state.
   * <p>
   * Calling {@link MapLibreMap#snapshot(MapLibreMap.SnapshotReadyCallback)} from this callback
   * will result in recursive execution. Use {@link OnDidFinishRenderingFrameListener} instead.
   * </p>
   * <p>
   * {@link MapView#addOnDidBecomeIdleListener(OnDidBecomeIdleListener)}
   * </p>
   */
  public interface OnDidBecomeIdleListener {
    /**
     * Called when the map has entered the idle state.
     */
    void onDidBecomeIdle();
  }

  /**
   * Interface definition for a callback to be invoked when the map has loaded the style.
   * <p>
   * {@link MapView#addOnDidFailLoadingMapListener(OnDidFailLoadingMapListener)}
   * </p>
   */
  public interface OnDidFinishLoadingStyleListener {
    /**
     * Called when a style has finished loading.
     */
    void onDidFinishLoadingStyle();
  }

  /**
   * Interface definition for a callback to be invoked when a map source has changed.
   * <p>
   * {@link MapView#addOnDidFailLoadingMapListener(OnDidFailLoadingMapListener)}
   * </p>
   */
  public interface OnSourceChangedListener {
    /**
     * Called when a map source has changed.
     *
     * @param id the id of the source that has changed
     */
    void onSourceChangedListener(String id);
  }

  /**
   * Interface definition for a callback to be invoked with the id of a missing icon. The icon should be added
   * synchronously with {@link Style#addImage(String, Bitmap)} to be rendered on the current zoom level. When loading
   * icons asynchronously, you can load a placeholder image and replace it when you icon has loaded.
   * <p>
   * {@link MapView#addOnStyleImageMissingListener(OnStyleImageMissingListener)}
   * </p>
   */
  public interface OnStyleImageMissingListener {
    /**
     * Called when the map is missing an icon.The icon should be added synchronously with
     * {@link Style#addImage(String, Bitmap)} to be rendered on the current zoom level. When loading icons
     * asynchronously, you can load a placeholder image and replace it when you icon has loaded.
     *
     * @param id the id of the icon that is missing
     */
    void onStyleImageMissing(@NonNull String id);
  }

  /**
   * Interface definition for a callback to be invoked with an unused image identifier.
   * <p>
   * {@link MapView#addOnCanRemoveUnusedStyleImageListener(OnCanRemoveUnusedStyleImageListener)}
   * </p>
   */
  public interface OnCanRemoveUnusedStyleImageListener {
    /**
     * Called when the map needs to release unused image resources.
     *
     * @param id of an image that is not used by the map and can be removed from the style.
     * @return true if image can be removed, false otherwise.
     */
    boolean onCanRemoveUnusedStyleImage(@NonNull String id);
  }

  /**
   * Interface definition for a callback to be invoked before a shader is compiled.
   * <p>
   * {@link MapView#addOnPreCompileShaderListener(OnPreCompileShaderListener)}
   * </p>
   */
  public interface OnPreCompileShaderListener {
    /**
     * Called before a shader is compiled.
     *
     * @param id of a shader type enumeration. See `mbgl::shaders::BuiltIn` for a list
     * of possible values.
     * @param type of graphics backend the shader is being compiled for. See
     * `mbgl::gfx::Backend::Type` for a list of possible values.
     * @param additionalDefines that specify the permutaion of the shader.
     */
    void onPreCompileShader(int id, int type, String additionalDefines);
  }

  /**
   * Interface definition for a callback to be invoked after a shader is compiled.
   * <p>
   * {@link MapView#addOnPostCompileShaderListener(OnPostCompileShaderListener)}
   * </p>
   */
  public interface OnPostCompileShaderListener {
    /**
     * Called after a shader is compiled.
     *
     * @param id of a shader type enumeration. See `mbgl::shaders::BuiltIn` for a list
     * of possible values.
     * @param type of graphics backend the shader is being compiled for. See
     * `mbgl::gfx::Backend::Type` for a list of possible values.
     * @param additionalDefines that specify the permutation of the shader.
     */
    void onPostCompileShader(int id, int type, String additionalDefines);
  }

  /**
   * Interface definition for a callback to be invoked after a shader failed to compile.
   * <p>
   * {@link MapView#addOnShaderCompileFailedListener(OnShaderCompileFailedListener)}
   * </p>
   */
  public interface OnShaderCompileFailedListener {
    /**
     * Called when a shader fails to compile.
     *
     * @param id of a shader type enumeration. See `mbgl::shaders::BuiltIn` for a list
     * of possible values.
     * @param type of graphics backend the shader is being compiled for. See
     * `mbgl::gfx::Backend::Type` for a list of possible values.
     * @param additionalDefines that specify the permutation of the shader.
     */
    void onShaderCompileFailed(int id, int type, String additionalDefines);
  }

  /**
   * Interface definition for a callback to be invoked after a range of glyphs are loaded.
   * <p>
   * {@link MapView#addOnGlyphsLoadedListener(OnGlyphsLoadedListener)}
   * </p>
   */
  public interface OnGlyphsLoadedListener {
    /**
     * Called when a range of glyphs for a font stack are loaded.
     *
     * @param stack of font names.
     * @param rangeStart of glyph indices being loaded.
     * @param rangeEnd of glyph indices being loaded.
     */
    void onGlyphsLoaded(@NonNull String[] stack, int rangeStart, int rangeEnd);
  }

  /**
   * Interface definition for a callback to be invoked after a range of glyphs fail to load.
   * <p>
   * {@link MapView#addOnGlyphsErrorListener(OnGlyphsErrorListener)}
   * </p>
   */
  public interface OnGlyphsErrorListener {
    /**
     * Called when a range of glyphs for a font stack failed to load.
     *
     * @param stack of font names.
     * @param rangeStart of glyph indices that failed to load.
     * @param rangeEnd of glyph indices that failed to load.
     */
    void onGlyphsError(@NonNull String[] stack, int rangeStart, int rangeEnd);
  }

  /**
   * Interface definition for a callback to be invoked after a range of glyphs are requested.
   * <p>
   * {@link MapView#addOnGlyphsRequestedListener(OnGlyphsRequestedListener)}
   * </p>
   */
  public interface OnGlyphsRequestedListener {
    /**
     * Called when a range of glyphs for a font stack are requested.
     *
     * @param stack of font names.
     * @param rangeStart of glyph indices that are being requested.
     * @param rangeEnd of glyph indices that are being requested.
     */
    void onGlyphsRequested(@NonNull String[] stack, int rangeStart, int rangeEnd);
  }

  /**
   * Interface definition for a callback to be invoked after a tile action occurs.
   * <p>
   * {@link MapView#addOnTileActionListener(OnTileActionListener)}
   * </p>
   */
  public interface OnTileActionListener {
    /**
     * Called when a tile action occurs.
     *
     * @param op identifying the tile action that occurred.
     * @param x coordinate of the tile.
     * @param y coordinate of the tile.
     * @param z coordinate of the tile.
     * @param wrap coordinate of the tile.
     * @param overscaledZ coordinate of the tile.
     * @param sourceID of the tile.
     */
    void onTileAction(TileOperation op, int x, int y, int z, int wrap, int overscaledZ, String sourceID);
  }

  /**
   * Interface definition for a callback to be invoked after a sprite is requested.
   * <p>
   * {@link MapView#addOnSpriteLoadedListener(OnSpriteLoadedListener)}
   * </p>
   */
  public interface OnSpriteLoadedListener {
    /**
     * Called when a sprite is loaded.
     *
     * @param id of the sprite.
     * @param url of the sprite.
     */
    void onSpriteLoaded(@NonNull String id, @NonNull String url);
  }

  /**
   * Interface definition for a callback to be invoked after a sprite fails to load.
   * <p>
   * {@link MapView#addOnSpriteErrorListener(OnSpriteErrorListener)}
   * </p>
   */
  public interface OnSpriteErrorListener {
    /**
     * Called when a sprite fails to load.
     *
     * @param id of the sprite.
     * @param url of the sprite.
     */
    void onSpriteError(@NonNull String id, @NonNull String url);
  }

  /**
   * Interface definition for a callback to be invoked after a sprite is requested.
   * <p>
   * {@link MapView#addOnSpriteRequestedListener(OnSpriteRequestedListener)}
   * </p>
   */
  public interface OnSpriteRequestedListener {
    /**
     * Called when a sprite is requested.
     *
     * @param id of the sprite.
     * @param url of the sprite.
     */
    void onSpriteRequested(@NonNull String id, @NonNull String url);
  }

  /**
   * Sets a callback object which will be triggered when the {@link MapLibreMap} instance is ready to be used.
   *
   * @param callback The callback object that will be triggered when the map is ready to be used.
   */
  @UiThread
  public void getMapAsync(final @NonNull OnMapReadyCallback callback) {
    if (maplibreMap == null) {
      // Add callback to the list only if the style hasn't loaded, or the drawing surface isn't ready
      mapCallback.addOnMapReadyCallback(callback);
    } else {
      callback.onMapReady(maplibreMap);
    }
  }

  private boolean isGestureDetectorInitialized() {
    return mapGestureDetector != null;
  }

  private boolean isKeyDetectorInitialized() {
    return mapKeyListener != null;
  }

  @Nullable
  MapLibreMap getMapLibreMap() {
    return maplibreMap;
  }

  void setMapLibreMap(MapLibreMap maplibreMap) {
    this.maplibreMap = maplibreMap;
  }

  private class FocalPointInvalidator implements FocalPointChangeListener {

    private final List<FocalPointChangeListener> focalPointChangeListeners = new ArrayList<>();

    void addListener(FocalPointChangeListener focalPointChangeListener) {
      focalPointChangeListeners.add(focalPointChangeListener);
    }

    @Override
    public void onFocalPointChanged(PointF pointF) {
      mapGestureDetector.setFocalPoint(pointF);
      for (FocalPointChangeListener focalPointChangeListener : focalPointChangeListeners) {
        focalPointChangeListener.onFocalPointChanged(pointF);
      }
    }
  }

  /**
   * The initial render callback waits for rendering to happen before making the map visible for end-users.
   * We wait for the second DID_FINISH_RENDERING_FRAME map change event as the first will still show a black surface.
   */
  private class InitialRenderCallback implements OnDidFinishRenderingFrameListener {

    private int renderCount;

    InitialRenderCallback() {
      addOnDidFinishRenderingFrameListener(this);
    }

    @Override
    public void onDidFinishRenderingFrame(boolean fully, double frameEncodingTime, double frameRenderingTime) {
      if (maplibreMap != null && maplibreMap.getStyle() != null && maplibreMap.getStyle().isFullyLoaded()) {
        renderCount++;
        if (renderCount == 3) {
          MapView.this.setForeground(null);
          removeOnDidFinishRenderingFrameListener(this);
        }
      }
    }

    private void onDestroy() {
      removeOnDidFinishRenderingFrameListener(this);
    }
  }

  private class GesturesManagerInteractionListener implements MapLibreMap.OnGesturesManagerInteractionListener {

    @Override
    public void onAddMapClickListener(MapLibreMap.OnMapClickListener listener) {
      mapGestureDetector.addOnMapClickListener(listener);
    }

    @Override
    public void onRemoveMapClickListener(MapLibreMap.OnMapClickListener listener) {
      mapGestureDetector.removeOnMapClickListener(listener);
    }

    @Override
    public void onAddMapLongClickListener(MapLibreMap.OnMapLongClickListener listener) {
      mapGestureDetector.addOnMapLongClickListener(listener);
    }

    @Override
    public void onRemoveMapLongClickListener(MapLibreMap.OnMapLongClickListener listener) {
      mapGestureDetector.removeOnMapLongClickListener(listener);
    }

    @Override
    public void onAddFlingListener(MapLibreMap.OnFlingListener listener) {
      mapGestureDetector.addOnFlingListener(listener);
    }

    @Override
    public void onRemoveFlingListener(MapLibreMap.OnFlingListener listener) {
      mapGestureDetector.removeOnFlingListener(listener);
    }

    @Override
    public void onAddMoveListener(MapLibreMap.OnMoveListener listener) {
      mapGestureDetector.addOnMoveListener(listener);
    }

    @Override
    public void onRemoveMoveListener(MapLibreMap.OnMoveListener listener) {
      mapGestureDetector.removeOnMoveListener(listener);
    }

    @Override
    public void onAddRotateListener(MapLibreMap.OnRotateListener listener) {
      mapGestureDetector.addOnRotateListener(listener);
    }

    @Override
    public void onRemoveRotateListener(MapLibreMap.OnRotateListener listener) {
      mapGestureDetector.removeOnRotateListener(listener);
    }

    @Override
    public void onAddScaleListener(MapLibreMap.OnScaleListener listener) {
      mapGestureDetector.addOnScaleListener(listener);
    }

    @Override
    public void onRemoveScaleListener(MapLibreMap.OnScaleListener listener) {
      mapGestureDetector.removeOnScaleListener(listener);
    }

    @Override
    public void onAddShoveListener(MapLibreMap.OnShoveListener listener) {
      mapGestureDetector.addShoveListener(listener);
    }

    @Override
    public void onRemoveShoveListener(MapLibreMap.OnShoveListener listener) {
      mapGestureDetector.removeShoveListener(listener);
    }

    @Override
    public AndroidGesturesManager getGesturesManager() {
      return mapGestureDetector.getGesturesManager();
    }

    @Override
    public void setGesturesManager(AndroidGesturesManager gesturesManager, boolean attachDefaultListeners,
                                   boolean setDefaultMutuallyExclusives) {
      mapGestureDetector.setGesturesManager(
              getContext(), gesturesManager, attachDefaultListeners, setDefaultMutuallyExclusives);
    }

    @Override
    public void cancelAllVelocityAnimations() {
      mapGestureDetector.cancelAnimators();
    }
  }

  private class MapCallback implements OnDidFinishLoadingStyleListener,
          OnDidFinishRenderingFrameListener, OnDidFinishLoadingMapListener,
          OnCameraIsChangingListener, OnCameraDidChangeListener, OnDidFailLoadingMapListener {

    private final List<OnMapReadyCallback> onMapReadyCallbackList = new ArrayList<>();

    MapCallback() {
      addOnDidFinishLoadingStyleListener(this);
      addOnDidFinishRenderingFrameListener(this);
      addOnDidFinishLoadingMapListener(this);
      addOnCameraIsChangingListener(this);
      addOnCameraDidChangeListener(this);
      addOnDidFailLoadingMapListener(this);
    }

    void initialised() {
      maplibreMap.onPreMapReady();
      onMapReady();
      maplibreMap.onPostMapReady();
    }

    /**
     * Notify listeners, clear when done
     */
    private void onMapReady() {
      if (onMapReadyCallbackList.size() > 0) {
        Iterator<OnMapReadyCallback> iterator = onMapReadyCallbackList.iterator();
        while (iterator.hasNext()) {
          OnMapReadyCallback callback = iterator.next();
          if (callback != null) {
            // null checking required for #13279
            callback.onMapReady(maplibreMap);
          }
          iterator.remove();
        }
      }
    }

    void addOnMapReadyCallback(OnMapReadyCallback callback) {
      onMapReadyCallbackList.add(callback);
    }

    void onDestroy() {
      onMapReadyCallbackList.clear();
      removeOnDidFinishLoadingStyleListener(this);
      removeOnDidFinishRenderingFrameListener(this);
      removeOnDidFinishLoadingMapListener(this);
      removeOnCameraIsChangingListener(this);
      removeOnCameraDidChangeListener(this);
      removeOnDidFailLoadingMapListener(this);
    }

    @Override
    public void onDidFinishLoadingStyle() {
      if (maplibreMap != null) {
        maplibreMap.onFinishLoadingStyle();
      }
    }

    @Override
    public void onDidFailLoadingMap(String errorMessage) {
      if (maplibreMap != null) {
        maplibreMap.onFailLoadingStyle();
      }
    }

    @Override
    public void onDidFinishRenderingFrame(boolean fully, double frameEncodingTime, double frameRenderingTime) {
      if (maplibreMap != null) {
        maplibreMap.onUpdateFullyRendered();
      }
    }

    @Override
    public void onDidFinishLoadingMap() {
      if (maplibreMap != null) {
        maplibreMap.onUpdateRegionChange();
      }
    }

    @Override
    public void onCameraIsChanging() {
      if (maplibreMap != null) {
        maplibreMap.onUpdateRegionChange();
      }
    }

    @Override
    public void onCameraDidChange(boolean animated) {
      if (maplibreMap != null) {
        maplibreMap.onUpdateRegionChange();
      }
    }
  }

  /**
   * Click event hook for providing a custom attribution dialog manager.
   */
  private static class AttributionClickListener implements OnClickListener {

    @NonNull
    private final AttributionDialogManager defaultDialogManager;
    private UiSettings uiSettings;

    private AttributionClickListener(@NonNull Context context, @NonNull MapLibreMap maplibreMap) {
      this.defaultDialogManager = new AttributionDialogManager(context, maplibreMap);
      this.uiSettings = maplibreMap.getUiSettings();
    }

    @Override
    public void onClick(View v) {
      getDialogManager().onClick(v);
    }

    public void onStop() {
      getDialogManager().onStop();
    }

    private AttributionDialogManager getDialogManager() {
      AttributionDialogManager customDialogManager = uiSettings.getAttributionDialogManager();
      if (customDialogManager != null) {
        return uiSettings.getAttributionDialogManager();
      } else {
        return defaultDialogManager;
      }
    }
  }

  /**
   * Sets the strict mode that will throw the {@link org.maplibre.android.MapStrictModeException}
   * whenever the map would fail silently otherwise.
   *
   * @param strictModeEnabled true to enable the strict mode, false otherwise
   */
  public static void setMapStrictModeEnabled(boolean strictModeEnabled) {
    MapStrictMode.setStrictModeEnabled(strictModeEnabled);
  }
}
