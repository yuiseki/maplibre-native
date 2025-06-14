cmake_minimum_required(VERSION 3.10)

project(MapLibreAndroid)

add_subdirectory(../../../../../
                 ../../../../../${ANDROID_ABI})

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

target_compile_options(mbgl-compiler-options INTERFACE -frtti)

add_library(maplibre SHARED
            ${PROJECT_SOURCE_DIR}/main.cpp
            ${PROJECT_SOURCE_DIR}/jni_native.cpp
            ${PROJECT_SOURCE_DIR}/jni_native.hpp
            ${PROJECT_SOURCE_DIR}/file_source.cpp
            ${PROJECT_SOURCE_DIR}/file_source.hpp
            ${PROJECT_SOURCE_DIR}/http_file_source.cpp
            ${PROJECT_SOURCE_DIR}/i18n/collator.cpp
            ${PROJECT_SOURCE_DIR}/i18n/collator_jni.hpp
            ${PROJECT_SOURCE_DIR}/i18n/number_format.cpp
            ${PROJECT_SOURCE_DIR}/i18n/number_format_jni.hpp
            ${PROJECT_SOURCE_DIR}/logger.cpp
            ${PROJECT_SOURCE_DIR}/logger.hpp
            ${PROJECT_SOURCE_DIR}/logging_android.cpp
            ${PROJECT_SOURCE_DIR}/text/local_glyph_rasterizer.cpp
            ${PROJECT_SOURCE_DIR}/text/local_glyph_rasterizer_jni.hpp
            ${PROJECT_SOURCE_DIR}/android_renderer_backend.cpp
            ${PROJECT_SOURCE_DIR}/android_renderer_backend.hpp
            ${PROJECT_SOURCE_DIR}/android_renderer_frontend.cpp
            ${PROJECT_SOURCE_DIR}/android_renderer_frontend.hpp
            ${PROJECT_SOURCE_DIR}/annotation/marker.cpp
            ${PROJECT_SOURCE_DIR}/annotation/marker.hpp
            ${PROJECT_SOURCE_DIR}/annotation/multi_point.hpp
            ${PROJECT_SOURCE_DIR}/annotation/polygon.cpp
            ${PROJECT_SOURCE_DIR}/annotation/polygon.hpp
            ${PROJECT_SOURCE_DIR}/annotation/polyline.cpp
            ${PROJECT_SOURCE_DIR}/annotation/polyline.hpp
            ${PROJECT_SOURCE_DIR}/asset_manager.hpp
            ${PROJECT_SOURCE_DIR}/asset_manager_file_source.cpp
            ${PROJECT_SOURCE_DIR}/asset_manager_file_source.hpp
            ${PROJECT_SOURCE_DIR}/connectivity_listener.cpp
            ${PROJECT_SOURCE_DIR}/connectivity_listener.hpp
            ${PROJECT_SOURCE_DIR}/conversion/collection.cpp
            ${PROJECT_SOURCE_DIR}/conversion/collection.hpp
            ${PROJECT_SOURCE_DIR}/conversion/color.cpp
            ${PROJECT_SOURCE_DIR}/conversion/color.hpp
            ${PROJECT_SOURCE_DIR}/conversion/constant.cpp
            ${PROJECT_SOURCE_DIR}/conversion/constant.hpp
            ${PROJECT_SOURCE_DIR}/conversion/conversion.hpp
            ${PROJECT_SOURCE_DIR}/geojson/feature.cpp
            ${PROJECT_SOURCE_DIR}/geojson/feature.hpp
            ${PROJECT_SOURCE_DIR}/geojson/feature_collection.cpp
            ${PROJECT_SOURCE_DIR}/geojson/feature_collection.hpp
            ${PROJECT_SOURCE_DIR}/geojson/geometry.cpp
            ${PROJECT_SOURCE_DIR}/geojson/geometry.hpp
            ${PROJECT_SOURCE_DIR}/geojson/geometry_collection.cpp
            ${PROJECT_SOURCE_DIR}/geojson/geometry_collection.hpp
            ${PROJECT_SOURCE_DIR}/geojson/line_string.cpp
            ${PROJECT_SOURCE_DIR}/geojson/line_string.hpp
            ${PROJECT_SOURCE_DIR}/geojson/multi_line_string.cpp
            ${PROJECT_SOURCE_DIR}/geojson/multi_line_string.hpp
            ${PROJECT_SOURCE_DIR}/geojson/multi_point.cpp
            ${PROJECT_SOURCE_DIR}/geojson/multi_point.hpp
            ${PROJECT_SOURCE_DIR}/geojson/multi_polygon.cpp
            ${PROJECT_SOURCE_DIR}/geojson/multi_polygon.hpp
            ${PROJECT_SOURCE_DIR}/geojson/point.cpp
            ${PROJECT_SOURCE_DIR}/geojson/point.hpp
            ${PROJECT_SOURCE_DIR}/geojson/polygon.cpp
            ${PROJECT_SOURCE_DIR}/geojson/polygon.hpp
            ${PROJECT_SOURCE_DIR}/geojson/util.hpp
            ${PROJECT_SOURCE_DIR}/geometry/lat_lng.cpp
            ${PROJECT_SOURCE_DIR}/geometry/lat_lng.hpp
            ${PROJECT_SOURCE_DIR}/geometry/lat_lng_bounds.cpp
            ${PROJECT_SOURCE_DIR}/geometry/lat_lng_bounds.hpp
            ${PROJECT_SOURCE_DIR}/geometry/lat_lng_quad.cpp
            ${PROJECT_SOURCE_DIR}/geometry/lat_lng_quad.hpp
            ${PROJECT_SOURCE_DIR}/geometry/projected_meters.cpp
            ${PROJECT_SOURCE_DIR}/geometry/projected_meters.hpp
            ${PROJECT_SOURCE_DIR}/graphics/pointf.cpp
            ${PROJECT_SOURCE_DIR}/graphics/pointf.hpp
            ${PROJECT_SOURCE_DIR}/graphics/rectf.cpp
            ${PROJECT_SOURCE_DIR}/graphics/rectf.hpp
            ${PROJECT_SOURCE_DIR}/gson/json_array.cpp
            ${PROJECT_SOURCE_DIR}/gson/json_array.hpp
            ${PROJECT_SOURCE_DIR}/gson/json_element.cpp
            ${PROJECT_SOURCE_DIR}/gson/json_element.hpp
            ${PROJECT_SOURCE_DIR}/gson/json_object.cpp
            ${PROJECT_SOURCE_DIR}/gson/json_object.hpp
            ${PROJECT_SOURCE_DIR}/gson/json_primitive.cpp
            ${PROJECT_SOURCE_DIR}/gson/json_primitive.hpp
            ${PROJECT_SOURCE_DIR}/java/util.cpp
            ${PROJECT_SOURCE_DIR}/java/util.hpp
            ${PROJECT_SOURCE_DIR}/java_types.cpp
            ${PROJECT_SOURCE_DIR}/java_types.hpp
            ${PROJECT_SOURCE_DIR}/java/util.cpp
            ${PROJECT_SOURCE_DIR}/java/util.hpp
            ${PROJECT_SOURCE_DIR}/java_types.cpp
            ${PROJECT_SOURCE_DIR}/java_types.hpp
            ${PROJECT_SOURCE_DIR}/maplibre.cpp
            ${PROJECT_SOURCE_DIR}/maplibre.hpp
            ${PROJECT_SOURCE_DIR}/map/camera_position.cpp
            ${PROJECT_SOURCE_DIR}/map/camera_position.hpp
            ${PROJECT_SOURCE_DIR}/map/image.cpp
            ${PROJECT_SOURCE_DIR}/map/image.hpp
            ${PROJECT_SOURCE_DIR}/map_renderer.cpp
            ${PROJECT_SOURCE_DIR}/map_renderer.hpp
            ${PROJECT_SOURCE_DIR}/map_renderer_runnable.cpp
            ${PROJECT_SOURCE_DIR}/map_renderer_runnable.hpp
            ${PROJECT_SOURCE_DIR}/native_map_view.cpp
            ${PROJECT_SOURCE_DIR}/native_map_view.hpp
            ${PROJECT_SOURCE_DIR}/native_map_options.cpp
            ${PROJECT_SOURCE_DIR}/native_map_options.hpp
            ${PROJECT_SOURCE_DIR}/offline/offline_manager.cpp
            ${PROJECT_SOURCE_DIR}/offline/offline_manager.hpp
            ${PROJECT_SOURCE_DIR}/offline/offline_region.cpp
            ${PROJECT_SOURCE_DIR}/offline/offline_region.hpp
            ${PROJECT_SOURCE_DIR}/offline/offline_region_definition.cpp
            ${PROJECT_SOURCE_DIR}/offline/offline_region_definition.hpp
            ${PROJECT_SOURCE_DIR}/offline/offline_region_error.cpp
            ${PROJECT_SOURCE_DIR}/offline/offline_region_error.hpp
            ${PROJECT_SOURCE_DIR}/offline/offline_region_status.cpp
            ${PROJECT_SOURCE_DIR}/offline/offline_region_status.hpp
            ${PROJECT_SOURCE_DIR}/snapshotter/map_snapshot.cpp
            ${PROJECT_SOURCE_DIR}/snapshotter/map_snapshot.hpp
            ${PROJECT_SOURCE_DIR}/snapshotter/map_snapshotter.cpp
            ${PROJECT_SOURCE_DIR}/snapshotter/map_snapshotter.hpp
            ${PROJECT_SOURCE_DIR}/style/android_conversion.hpp
            ${PROJECT_SOURCE_DIR}/style/conversion/filter.cpp
            ${PROJECT_SOURCE_DIR}/style/conversion/filter.hpp
            ${PROJECT_SOURCE_DIR}/style/conversion/position.cpp
            ${PROJECT_SOURCE_DIR}/style/conversion/position.hpp
            ${PROJECT_SOURCE_DIR}/style/conversion/property_expression.hpp
            ${PROJECT_SOURCE_DIR}/style/conversion/property_value.hpp
            ${PROJECT_SOURCE_DIR}/style/conversion/transition_options.cpp
            ${PROJECT_SOURCE_DIR}/style/conversion/transition_options.hpp
            ${PROJECT_SOURCE_DIR}/style/conversion/url_or_tileset.cpp
            ${PROJECT_SOURCE_DIR}/style/conversion/url_or_tileset.hpp
            ${PROJECT_SOURCE_DIR}/style/formatted.cpp
            ${PROJECT_SOURCE_DIR}/style/formatted.hpp
            ${PROJECT_SOURCE_DIR}/style/formatted_section.cpp
            ${PROJECT_SOURCE_DIR}/style/formatted_section.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/background_layer.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/background_layer.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/circle_layer.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/circle_layer.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/custom_layer.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/custom_layer.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/fill_extrusion_layer.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/fill_extrusion_layer.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/fill_layer.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/fill_layer.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/heatmap_layer.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/heatmap_layer.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/hillshade_layer.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/hillshade_layer.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/layer.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/layer.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/layer_manager.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/layer_manager.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/line_layer.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/line_layer.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/raster_layer.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/raster_layer.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/symbol_layer.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/symbol_layer.hpp
            ${PROJECT_SOURCE_DIR}/style/layers/location_indicator_layer.cpp
            ${PROJECT_SOURCE_DIR}/style/layers/location_indicator_layer.hpp
            ${PROJECT_SOURCE_DIR}/style/light.cpp
            ${PROJECT_SOURCE_DIR}/style/light.hpp
            ${PROJECT_SOURCE_DIR}/style/position.cpp
            ${PROJECT_SOURCE_DIR}/style/position.hpp
            ${PROJECT_SOURCE_DIR}/style/sources/custom_geometry_source.cpp
            ${PROJECT_SOURCE_DIR}/style/sources/custom_geometry_source.hpp
            ${PROJECT_SOURCE_DIR}/style/sources/geojson_source.cpp
            ${PROJECT_SOURCE_DIR}/style/sources/geojson_source.hpp
            ${PROJECT_SOURCE_DIR}/style/sources/image_source.cpp
            ${PROJECT_SOURCE_DIR}/style/sources/image_source.hpp
            ${PROJECT_SOURCE_DIR}/style/sources/raster_dem_source.cpp
            ${PROJECT_SOURCE_DIR}/style/sources/raster_dem_source.hpp
            ${PROJECT_SOURCE_DIR}/style/sources/raster_source.cpp
            ${PROJECT_SOURCE_DIR}/style/sources/raster_source.hpp
            ${PROJECT_SOURCE_DIR}/style/sources/source.cpp
            ${PROJECT_SOURCE_DIR}/style/sources/source.hpp
            ${PROJECT_SOURCE_DIR}/style/sources/unknown_source.cpp
            ${PROJECT_SOURCE_DIR}/style/sources/unknown_source.hpp
            ${PROJECT_SOURCE_DIR}/style/sources/vector_source.cpp
            ${PROJECT_SOURCE_DIR}/style/sources/vector_source.hpp
            ${PROJECT_SOURCE_DIR}/style/transition_options.cpp
            ${PROJECT_SOURCE_DIR}/style/transition_options.hpp
            ${PROJECT_SOURCE_DIR}/style/value.cpp
            ${PROJECT_SOURCE_DIR}/style/value.hpp
            ${PROJECT_SOURCE_DIR}/tile/tile_operation.cpp
            ${PROJECT_SOURCE_DIR}/util/default_style.cpp
            ${PROJECT_SOURCE_DIR}/util/tile_server_options.cpp
            ${PROJECT_SOURCE_DIR}/util/default_style.hpp
            ${PROJECT_SOURCE_DIR}/util/tile_server_options.hpp)

if(MLN_WITH_OPENGL)
    target_sources(
        maplibre PRIVATE
            ${PROJECT_SOURCE_DIR}/android_gl_renderer_backend.cpp
            ${PROJECT_SOURCE_DIR}/android_gl_renderer_backend.hpp
    )
    target_compile_definitions(
        maplibre
            PRIVATE MLN_RENDER_BACKEND_OPENGL=1
    )
endif()

if(MLN_WITH_VULKAN)
    target_sources(
        maplibre PRIVATE
            ${PROJECT_SOURCE_DIR}/android_vulkan_renderer_backend.cpp
            ${PROJECT_SOURCE_DIR}/android_vulkan_renderer_backend.hpp
    )
    target_compile_definitions(
        maplibre
            PRIVATE MLN_RENDER_BACKEND_VULKAN=1
    )
endif()

target_include_directories(
  maplibre
  PRIVATE ${PROJECT_SOURCE_DIR}
          ../../../../../src
          ../../../src
          ../../../../default/include)

target_compile_options(maplibre
                       PRIVATE $<$<CONFIG:Release>:-Oz>
                               $<$<CONFIG:Release>:-Qunused-arguments>
                               $<$<CONFIG:Release>:-flto>
                               $<$<CONFIG:Release>:-fvisibility=hidden>
                               $<$<CONFIG:Release>:-fvisibility-inlines-hidden>)

target_link_libraries(maplibre
                      PRIVATE $<$<CONFIG:Release>:-Oz>
                              $<$<CONFIG:Release>:-Wl,--icf=all>
                              $<$<CONFIG:Release>:-Wl,--gc-sections>
                              $<$<CONFIG:Release>:-flto>
                              $<$<CONFIG:Release>:-fuse-ld=gold>
                              MapLibreNative::Base::jni.hpp
                              mbgl-core
                              mbgl-vendor-unique_resource)

install(TARGETS maplibre LIBRARY DESTINATION lib)
