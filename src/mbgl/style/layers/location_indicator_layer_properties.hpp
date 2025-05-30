// clang-format off

// This file is generated. Edit scripts/generate-style-code.js, then run `make style-code`.

#pragma once

#include <mbgl/style/types.hpp>
#include <mbgl/style/layer_properties.hpp>
#include <mbgl/style/layers/location_indicator_layer.hpp>
#include <mbgl/style/layout_property.hpp>
#include <mbgl/style/paint_property.hpp>
#include <mbgl/style/properties.hpp>
#include <mbgl/shaders/attributes.hpp>
#include <mbgl/shaders/uniforms.hpp>

namespace mbgl {
namespace style {

struct BearingImage : LayoutProperty<expression::Image> {
    static constexpr const char *name() { return "bearing-image"; }
    static expression::Image defaultValue() { return {}; }
};

struct ShadowImage : LayoutProperty<expression::Image> {
    static constexpr const char *name() { return "shadow-image"; }
    static expression::Image defaultValue() { return {}; }
};

struct TopImage : LayoutProperty<expression::Image> {
    static constexpr const char *name() { return "top-image"; }
    static expression::Image defaultValue() { return {}; }
};

struct AccuracyRadius : PaintProperty<float> {
    static float defaultValue() { return 0.f; }
};

struct AccuracyRadiusBorderColor : PaintProperty<Color> {
    static Color defaultValue() { return Color::white(); }
};

struct AccuracyRadiusColor : PaintProperty<Color> {
    static Color defaultValue() { return Color::white(); }
};

struct Bearing : PaintProperty<Rotation> {
    static Rotation defaultValue() { return 0; }
};

struct BearingImageSize : PaintProperty<float> {
    static float defaultValue() { return 1.f; }
};

struct ImageTiltDisplacement : PaintProperty<float> {
    static float defaultValue() { return 0.f; }
};

struct Location : PaintProperty<std::array<double, 3>> {
    static std::array<double, 3> defaultValue() { return {{0.f, 0.f, 0.f}}; }
};

struct PerspectiveCompensation : PaintProperty<float> {
    static float defaultValue() { return 0.85f; }
};

struct ShadowImageSize : PaintProperty<float> {
    static float defaultValue() { return 1.f; }
};

struct TopImageSize : PaintProperty<float> {
    static float defaultValue() { return 1.f; }
};

class LocationIndicatorLayoutProperties : public Properties<
    BearingImage,
    ShadowImage,
    TopImage
> {};

class LocationIndicatorPaintProperties : public Properties<
    AccuracyRadius,
    AccuracyRadiusBorderColor,
    AccuracyRadiusColor,
    Bearing,
    BearingImageSize,
    ImageTiltDisplacement,
    Location,
    PerspectiveCompensation,
    ShadowImageSize,
    TopImageSize
> {};

class LocationIndicatorLayerProperties final : public LayerProperties {
public:
    explicit LocationIndicatorLayerProperties(Immutable<LocationIndicatorLayer::Impl>);
    LocationIndicatorLayerProperties(
        Immutable<LocationIndicatorLayer::Impl>,
        LocationIndicatorPaintProperties::PossiblyEvaluated);
    ~LocationIndicatorLayerProperties() override;

    unsigned long constantsMask() const override;

    expression::Dependency getDependencies() const noexcept override;

    const LocationIndicatorLayer::Impl& layerImpl() const noexcept;
    // Data members.
    LocationIndicatorPaintProperties::PossiblyEvaluated evaluated;
};

} // namespace style
} // namespace mbgl

// clang-format on
