#include <mbgl/annotation/annotation_manager.hpp>
#include <mbgl/layermanager/layer_manager.hpp>
#include <mbgl/map/camera.hpp>
#include <mbgl/map/map.hpp>
#include <mbgl/map/map_impl.hpp>
#include <mbgl/map/transform.hpp>
#include <mbgl/math/angles.hpp>
#include <mbgl/math/log2.hpp>
#include <mbgl/renderer/renderer_frontend.hpp>
#include <mbgl/renderer/renderer_observer.hpp>
#include <mbgl/renderer/update_parameters.hpp>
#include <mbgl/storage/file_source_manager.hpp>
#include <mbgl/storage/resource.hpp>
#include <mbgl/storage/response.hpp>
#include <mbgl/style/observer.hpp>
#include <mbgl/style/style_impl.hpp>
#include <mbgl/util/constants.hpp>
#include <mbgl/util/exception.hpp>
#include <mbgl/util/logging.hpp>
#include <mbgl/util/mapbox.hpp>
#include <mbgl/util/math.hpp>
#include <mbgl/util/tile_coordinate.hpp>
#include <mbgl/util/action_journal.hpp>

#include <utility>

namespace mbgl {

using namespace style;

Map::Map(RendererFrontend& frontend,
         MapObserver& observer,
         const MapOptions& mapOptions,
         const ResourceOptions& resourceOptions,
         const ClientOptions& clientOptions,
         const util::ActionJournalOptions& actionJournalOptions)
    : impl(std::make_unique<Impl>(frontend,
                                  observer,
                                  FileSourceManager::get()
                                      ? std::shared_ptr<FileSource>(FileSourceManager::get()->getFileSource(
                                            ResourceLoader, resourceOptions, clientOptions))
                                      : nullptr,
                                  mapOptions)) {
    if (actionJournalOptions.enabled()) {
        impl->actionJournal = std::make_unique<util::ActionJournal>(*this, actionJournalOptions);
    }
}

Map::Map(std::unique_ptr<Impl> impl_, const util::ActionJournalOptions& actionJournalOptions)
    : impl(std::move(impl_)) {
    if (actionJournalOptions.enabled()) {
        impl->actionJournal = std::make_unique<util::ActionJournal>(*this, actionJournalOptions);
    }
}

Map::~Map() {
    impl->actionJournal.reset();
}

void Map::renderStill(StillImageCallback callback) {
    if (!callback) {
        Log::Error(Event::General, "StillImageCallback not set");
        return;
    }

    if (impl->mode != MapMode::Static && impl->mode != MapMode::Tile) {
        callback(std::make_exception_ptr(util::MisuseException("Map is not in static or tile image render modes")));
        return;
    }

    if (impl->stillImageRequest) {
        callback(std::make_exception_ptr(util::MisuseException("Map is currently rendering an image")));
        return;
    }

    if (impl->style->impl->getLastError()) {
        callback(impl->style->impl->getLastError());
        return;
    }

    impl->stillImageRequest = std::make_unique<StillImageRequest>(std::move(callback));

    impl->onUpdate();
}

void Map::renderStill(const CameraOptions& camera, MapDebugOptions debugOptions, StillImageCallback callback) {
    impl->cameraMutated = true;
    impl->debugOptions = debugOptions;
    impl->transform.jumpTo(camera);
    renderStill(std::move(callback));
}

void Map::triggerRepaint() {
    impl->onUpdate();
}

// MARK: - Style

style::Style& Map::getStyle() {
    return *impl->style;
}

const style::Style& Map::getStyle() const {
    return *impl->style;
}

void Map::setStyle(std::unique_ptr<Style> style) {
    assert(style);
    impl->onStyleLoading();
    impl->style = std::move(style);
    if (LayerManager::annotationsEnabled) {
        impl->annotationManager.setStyle(*impl->style);
    }
}

// MARK: - Transitions

void Map::cancelTransitions() {
    impl->transform.cancelTransitions();
    impl->onUpdate();
}

void Map::setGestureInProgress(bool inProgress) {
    impl->transform.setGestureInProgress(inProgress);
    impl->onUpdate();
}

bool Map::isGestureInProgress() const {
    return impl->transform.isGestureInProgress();
}

bool Map::isRotating() const {
    return impl->transform.isRotating();
}

bool Map::isScaling() const {
    return impl->transform.isScaling();
}

bool Map::isPanning() const {
    return impl->transform.isPanning();
}

// MARK: -

CameraOptions Map::getCameraOptions(const std::optional<EdgeInsets>& padding) const {
    return impl->transform.getCameraOptions(padding);
}

void Map::jumpTo(const CameraOptions& camera) {
    impl->jumpTo(camera);
}

void Map::easeTo(const CameraOptions& camera, const AnimationOptions& animation) {
    impl->cameraMutated = true;
    impl->transform.easeTo(camera, animation);
    impl->onUpdate();
}

void Map::flyTo(const CameraOptions& camera, const AnimationOptions& animation) {
    impl->cameraMutated = true;
    impl->transform.flyTo(camera, animation);
    impl->onUpdate();
}

void Map::moveBy(const ScreenCoordinate& point, const AnimationOptions& animation) {
    impl->cameraMutated = true;
    impl->transform.moveBy(point, animation);
    impl->onUpdate();
}

void Map::pitchBy(double pitch, const AnimationOptions& animation) {
    easeTo(CameraOptions().withPitch(util::rad2deg(impl->transform.getPitch()) - pitch), animation);
}

void Map::scaleBy(double scale, const std::optional<ScreenCoordinate>& anchor, const AnimationOptions& animation) {
    const double zoom = impl->transform.getZoom() + impl->transform.getState().scaleZoom(scale);
    easeTo(CameraOptions().withZoom(zoom).withAnchor(anchor), animation);
}

void Map::rotateBy(const ScreenCoordinate& first, const ScreenCoordinate& second, const AnimationOptions& animation) {
    impl->cameraMutated = true;
    impl->transform.rotateBy(first, second, animation);
    impl->onUpdate();
}

CameraOptions Map::cameraForLatLngBounds(const LatLngBounds& bounds,
                                         const EdgeInsets& padding,
                                         const std::optional<double>& bearing,
                                         const std::optional<double>& pitch) const {
    return cameraForLatLngs(
        {
            bounds.northwest(),
            bounds.southwest(),
            bounds.southeast(),
            bounds.northeast(),
        },
        padding,
        bearing,
        pitch);
}

CameraOptions cameraForLatLngs(const std::vector<LatLng>& latLngs,
                               const Transform& transform,
                               const EdgeInsets& padding) {
    if (latLngs.empty()) {
        return {};
    }
    Size size = transform.getState().getSize();
    // Calculate the bounds of the possibly rotated shape with respect to the viewport.
    ScreenCoordinate nePixel = {-INFINITY, -INFINITY};
    ScreenCoordinate swPixel = {INFINITY, INFINITY};
    for (LatLng latLng : latLngs) {
        ScreenCoordinate pixel = transform.latLngToScreenCoordinate(latLng);
        swPixel.x = std::min(swPixel.x, pixel.x);
        nePixel.x = std::max(nePixel.x, pixel.x);
        swPixel.y = std::min(swPixel.y, pixel.y);
        nePixel.y = std::max(nePixel.y, pixel.y);
    }
    double width = nePixel.x - swPixel.x;
    double height = nePixel.y - swPixel.y;

    // Calculate the zoom level.
    double minScale = INFINITY;
    if (width > 0 || height > 0) {
        double scaleX = static_cast<double>(size.width) / width;
        double scaleY = static_cast<double>(size.height) / height;
        scaleX -= (padding.left() + padding.right()) / width;
        scaleY -= (padding.top() + padding.bottom()) / height;
        minScale = util::min(scaleX, scaleY);
    }

    double zoom = transform.getZoom();
    if (minScale > 0) {
        zoom = util::clamp(
            zoom + util::log2(minScale), transform.getState().getMinZoom(), transform.getState().getMaxZoom());
    } else {
        Log::Error(Event::General,
                   "Unable to calculate appropriate zoom level for bounds. Vertical "
                   "or horizontal padding is greater "
                   "than map's height or width.");
    }

    // Calculate the center point of a virtual bounds that is extended in all directions by padding.
    ScreenCoordinate centerPixel = nePixel + swPixel;
    centerPixel /= 2.0;

    return CameraOptions()
        .withCenter(transform.screenCoordinateToLatLng(centerPixel))
        .withPadding(padding)
        .withZoom(zoom);
}

CameraOptions Map::cameraForLatLngs(const std::vector<LatLng>& latLngs,
                                    const EdgeInsets& padding,
                                    const std::optional<double>& bearing,
                                    const std::optional<double>& pitch) const {
    if (!bearing && !pitch) {
        return mbgl::cameraForLatLngs(latLngs, impl->transform, padding);
    }

    Transform transform(impl->transform.getState());

    if (bearing || pitch) {
        transform.jumpTo(CameraOptions().withBearing(bearing).withPitch(pitch));
    }

    return mbgl::cameraForLatLngs(latLngs, transform, padding)
        .withBearing(util::rad2deg(-transform.getBearing()))
        .withPitch(util::rad2deg(transform.getPitch()));
}

CameraOptions Map::cameraForGeometry(const Geometry<double>& geometry,
                                     const EdgeInsets& padding,
                                     const std::optional<double>& bearing,
                                     const std::optional<double>& pitch) const {
    std::vector<LatLng> latLngs;
    forEachPoint(geometry, [&](const Point<double>& pt) { latLngs.emplace_back(pt.y, pt.x); });
    return cameraForLatLngs(latLngs, padding, bearing, pitch);
}

LatLngBounds Map::latLngBoundsForCamera(const CameraOptions& camera) const {
    Transform shallow{impl->transform.getState()};
    Size size = shallow.getState().getSize();

    shallow.jumpTo(camera);
    return LatLngBounds::hull(
        shallow.screenCoordinateToLatLng({}),
        shallow.screenCoordinateToLatLng({static_cast<double>(size.width), static_cast<double>(size.height)}));
}

LatLngBounds Map::latLngBoundsForCameraUnwrapped(const CameraOptions& camera) const {
    Transform shallow{impl->transform.getState()};
    Size size = shallow.getState().getSize();

    shallow.jumpTo(camera);
    LatLng nw = shallow.screenCoordinateToLatLng({});
    LatLng se = shallow.screenCoordinateToLatLng({static_cast<double>(size.width), static_cast<double>(size.height)});
    LatLng ne = shallow.screenCoordinateToLatLng({static_cast<double>(size.width), 0.0});
    LatLng sw = shallow.screenCoordinateToLatLng({0.0, static_cast<double>(size.height)});
    LatLng center = shallow.screenCoordinateToLatLng(
        {static_cast<double>(size.width) / 2, static_cast<double>(size.height) / 2});
    nw.unwrapForShortestPath(center);
    se.unwrapForShortestPath(center);
    ne.unwrapForShortestPath(center);
    sw.unwrapForShortestPath(center);
    LatLngBounds bounds = LatLngBounds::hull(nw, se);
    bounds.extend(ne);
    bounds.extend(sw);
    bounds.extend(center);
    return bounds;
}

// MARK: - Bounds

void Map::setBounds(const BoundOptions& options) {
    bool changeCamera = false;
    CameraOptions cameraOptions;

    if (options.bounds) {
        changeCamera = true;
        impl->transform.setLatLngBounds(*options.bounds);
    }

    if (options.minZoom) {
        impl->transform.setMinZoom(*options.minZoom);
        if (impl->transform.getZoom() < *options.minZoom) {
            changeCamera = true;
            cameraOptions.withZoom(options.minZoom);
        }
    }

    if (options.maxZoom) {
        impl->transform.setMaxZoom(*options.maxZoom);
        if (impl->transform.getZoom() > *options.maxZoom) {
            changeCamera = true;
            cameraOptions.withZoom(options.maxZoom);
        }
    }

    if (options.maxPitch) {
        impl->transform.setMaxPitch(*options.maxPitch);
        if (impl->transform.getPitch() > impl->transform.getState().getMaxPitch()) {
            changeCamera = true;
            cameraOptions.withPitch(options.maxPitch);
        }
    }

    if (options.minPitch) {
        impl->transform.setMinPitch(*options.minPitch);
        if (impl->transform.getPitch() < impl->transform.getState().getMinPitch()) {
            changeCamera = true;
            cameraOptions.withPitch(options.minPitch);
        }
    }

    if (changeCamera) {
        jumpTo(cameraOptions);
    }
}

BoundOptions Map::getBounds() const {
    return BoundOptions()
        .withLatLngBounds(impl->transform.getState().getLatLngBounds())
        .withMinZoom(impl->transform.getState().getMinZoom())
        .withMaxZoom(impl->transform.getState().getMaxZoom())
        .withMinPitch(util::rad2deg(impl->transform.getState().getMinPitch()))
        .withMaxPitch(util::rad2deg(impl->transform.getState().getMaxPitch()));
}

// MARK: - Map options

void Map::setSize(const Size size) {
    impl->transform.resize(size);
    impl->onUpdate();
}

void Map::setNorthOrientation(NorthOrientation orientation) {
    impl->transform.setNorthOrientation(orientation);
    impl->onUpdate();
}

void Map::setConstrainMode(mbgl::ConstrainMode mode) {
    impl->transform.setConstrainMode(mode);
    impl->onUpdate();
}

void Map::setViewportMode(mbgl::ViewportMode mode) {
    impl->transform.setViewportMode(mode);
    impl->onUpdate();
}

MapOptions Map::getMapOptions() const {
    return std::move(MapOptions()
                         .withMapMode(impl->mode)
                         .withConstrainMode(impl->transform.getConstrainMode())
                         .withViewportMode(impl->transform.getViewportMode())
                         .withCrossSourceCollisions(impl->crossSourceCollisions)
                         .withNorthOrientation(impl->transform.getNorthOrientation())
                         .withSize(impl->transform.getState().getSize())
                         .withPixelRatio(impl->pixelRatio));
}

// MARK: - Projection mode

void Map::setProjectionMode(const ProjectionMode& options) {
    impl->transform.setProjectionMode(options);
    impl->onUpdate();
}

ProjectionMode Map::getProjectionMode() const {
    return impl->transform.getProjectionMode();
}

// MARK: - Projection

ScreenCoordinate Map::pixelForLatLng(const LatLng& latLng) const {
    // If the center and point longitudes are not in the same side of the
    // antimeridian, we unwrap the point longitude so it would be seen if
    // e.g. the next antimeridian side is visible.
    LatLng unwrappedLatLng = latLng.wrapped();
    unwrappedLatLng.unwrapForShortestPath(impl->transform.getLatLng());
    return impl->transform.latLngToScreenCoordinate(unwrappedLatLng);
}

LatLng Map::latLngForPixel(const ScreenCoordinate& pixel) const {
    return impl->transform.screenCoordinateToLatLng(pixel);
}

std::vector<ScreenCoordinate> Map::pixelsForLatLngs(const std::vector<LatLng>& latLngs) const {
    std::vector<ScreenCoordinate> ret;
    ret.reserve(latLngs.size());
    for (const auto& latLng : latLngs) {
        ret.emplace_back(pixelForLatLng(latLng));
    }
    return ret;
}

std::vector<LatLng> Map::latLngsForPixels(const std::vector<ScreenCoordinate>& screenCoords) const {
    std::vector<LatLng> ret;
    ret.reserve(screenCoords.size());
    for (const auto& point : screenCoords) {
        ret.emplace_back(latLngForPixel(point));
    }
    return ret;
}

// MARK: - Transform

TransformState Map::getTransfromState() const {
    return impl->transform.getState();
}

// MARK: - Annotations

void Map::addAnnotationImage(std::unique_ptr<style::Image> image) {
    if (LayerManager::annotationsEnabled) {
        impl->annotationManager.addImage(std::move(image));
    }
}

void Map::removeAnnotationImage(const std::string& id) {
    if (LayerManager::annotationsEnabled) {
        impl->annotationManager.removeImage(id);
    }
}

double Map::getTopOffsetPixelsForAnnotationImage(const std::string& id) {
    if (LayerManager::annotationsEnabled) {
        return impl->annotationManager.getTopOffsetPixelsForImage(id);
    }
    return 0.0;
}

AnnotationID Map::addAnnotation(const Annotation& annotation) {
    if (LayerManager::annotationsEnabled) {
        auto result = impl->annotationManager.addAnnotation(annotation);
        impl->onUpdate();
        return result;
    }
    return 0;
}

void Map::updateAnnotation(AnnotationID id, const Annotation& annotation) {
    if (LayerManager::annotationsEnabled) {
        if (impl->annotationManager.updateAnnotation(id, annotation)) {
            impl->onUpdate();
        }
    }
}

void Map::removeAnnotation(AnnotationID annotation) {
    if (LayerManager::annotationsEnabled) {
        impl->annotationManager.removeAnnotation(annotation);
        impl->onUpdate();
    }
}

// MARK: - Toggles

void Map::setDebug(MapDebugOptions debugOptions) {
    impl->debugOptions = debugOptions;
    impl->onUpdate();
}

MapDebugOptions Map::getDebug() const {
    return impl->debugOptions;
}

void Map::setPrefetchZoomDelta(uint8_t delta) {
    impl->prefetchZoomDelta = delta;
}

uint8_t Map::getPrefetchZoomDelta() const {
    return impl->prefetchZoomDelta;
}

bool Map::isFullyLoaded() const {
    return impl->style->impl->isLoaded() && impl->rendererFullyLoaded;
}

void Map::dumpDebugLogs() const {
    Log::Info(Event::General,
              "----------------------------------------------------------------------"
              "----------");
    impl->style->impl->dumpDebugLogs();
    Log::Info(Event::General,
              "----------------------------------------------------------------------"
              "----------");
}

void Map::setFreeCameraOptions(const FreeCameraOptions& camera) {
    impl->transform.setFreeCameraOptions(camera);
    impl->cameraMutated = true;
    impl->onUpdate();
}

FreeCameraOptions Map::getFreeCameraOptions() const {
    return impl->transform.getFreeCameraOptions();
}

ClientOptions Map::getClientOptions() const {
    return impl->fileSource ? impl->fileSource->getClientOptions() : ClientOptions();
}

const std::unique_ptr<util::ActionJournal>& Map::getActionJournal() {
    return impl->actionJournal;
}

} // namespace mbgl
