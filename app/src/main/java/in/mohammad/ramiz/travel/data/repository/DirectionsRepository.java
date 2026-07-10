package in.mohammad.ramiz.travel.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import in.mohammad.ramiz.travel.core.Result;
import in.mohammad.ramiz.travel.domain.model.NavigationStep;
import in.mohammad.ramiz.travel.domain.model.RouteInfo;
import in.mohammad.ramiz.travel.util.PolylineUtil;
import com.mappls.sdk.geojson.Point;
import com.mappls.sdk.maps.geometry.LatLng;
import com.mappls.sdk.services.api.OnResponseCallback;
import com.mappls.sdk.services.api.directions.DirectionsCriteria;
import com.mappls.sdk.services.api.directions.MapplsDirectionManager;
import com.mappls.sdk.services.api.directions.MapplsDirections;
import com.mappls.sdk.services.api.directions.models.DirectionsResponse;
import com.mappls.sdk.services.api.directions.models.DirectionsRoute;
import com.mappls.sdk.services.api.directions.models.LegAnnotation;
import com.mappls.sdk.services.api.directions.models.LegStep;
import com.mappls.sdk.services.api.directions.models.MaxSpeed;
import com.mappls.sdk.services.api.directions.models.RouteLeg;
import com.mappls.sdk.services.api.directions.models.StepManeuver;

import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Routing via the Mappls (MapmyIndia) Directions API â€” auth handled by the
 * SDK's configuration files, no key in code. Driving uses route_eta
 * (live-traffic-adjusted duration); motorcycle uses the two-wheeler (biking)
 * profile. Turn instructions are synthesized from OSRM-style maneuvers
 * (type + modifier + road name). Keeps the last route in memory for fallback.
 */
@Singleton
public class DirectionsRepository {

    @Nullable
    private volatile RouteInfo lastRoute;

    public interface Callback {
        void onResult(Result<RouteInfo> result);
    }

    @Inject
    public DirectionsRepository() {
    }

    /**
     * @param motorcycle true -> Mappls two-wheeler profile; false -> driving with traffic ETA
     */
    public void getRoute(double originLat, double originLng,
                         double destLat, double destLng,
                         boolean motorcycle,
                         @NonNull Callback callback) {
        MapplsDirections.Builder builder = MapplsDirections.builder()
                .origin(Point.fromLngLat(originLng, originLat))
                .destination(Point.fromLngLat(destLng, destLat))
                .steps(true)
                .alternatives(false)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6);
        // Note: requesting the "maxspeed" annotation makes Mappls' route_eta /
        // route_adv endpoints reject the request, so we don't ask for it.
        // parseSpeedLimits() still reads limits if the response ever carries them;
        // otherwise the LIMIT tile shows "--".

        if (motorcycle) {
            // Two-wheeler routing is restricted to route_adv (the default resource).
            builder.profile(DirectionsCriteria.PROFILE_BIKING)
                    .resource(DirectionsCriteria.RESOURCE_ROUTE);
        } else {
            builder.profile(DirectionsCriteria.PROFILE_DRIVING)
                    .resource(DirectionsCriteria.RESOURCE_ROUTE_ETA);
        }

        MapplsDirectionManager.newInstance(builder.build()).call(
                new OnResponseCallback<DirectionsResponse>() {
                    @Override
                    public void onSuccess(DirectionsResponse response) {
                        if (response == null || response.routes() == null
                                || response.routes().isEmpty()) {
                            callback.onResult(Result.error(
                                    new IOException("No route found"), lastRoute));
                            return;
                        }
                        RouteInfo route = parseRoute(response.routes().get(0));
                        lastRoute = route;
                        callback.onResult(Result.success(route));
                    }

                    @Override
                    public void onError(int code, String message) {
                        callback.onResult(Result.error(
                                new IOException("Directions error " + code + ": " + message),
                                lastRoute));
                    }
                });
    }

    @Nullable
    public RouteInfo getLastRoute() {
        return lastRoute;
    }

    // ------------------------------------------------------------------

    private RouteInfo parseRoute(DirectionsRoute dto) {
        RouteInfo route = new RouteInfo();
        route.summary = "";
        route.encodedPolyline = dto.geometry();
        route.path = dto.geometry() != null
                ? PolylineUtil.decode(dto.geometry(), 6)
                : new ArrayList<>();
        if (dto.distance() != null) route.distanceMeters = (int) Math.round(dto.distance());
        if (dto.duration() != null) route.durationSeconds = (int) Math.round(dto.duration());
        // route_eta already folds live traffic into the duration.
        route.durationInTrafficSeconds = route.durationSeconds;

        route.segmentSpeedLimitsKmh = parseSpeedLimits(dto);

        route.steps = new ArrayList<>();
        if (dto.legs() != null && !dto.legs().isEmpty()) {
            RouteLeg leg = dto.legs().get(0);
            if (leg.steps() != null) {
                for (LegStep s : leg.steps()) {
                    NavigationStep step = new NavigationStep();
                    step.instruction = buildInstruction(s);
                    StepManeuver m = s.maneuver();
                    step.maneuver = m != null
                            ? (m.type() != null ? m.type() : "")
                            + (m.modifier() != null ? "-" + m.modifier() : "")
                            : "";
                    step.distanceMeters = (int) Math.round(s.distance());
                    step.durationSeconds = (int) Math.round(s.duration());
                    if (m != null && m.location() != null) {
                        LatLng at = new LatLng(m.location().latitude(), m.location().longitude());
                        // The OSRM maneuver point is where the instruction applies:
                        // it starts this step and ends the previous one.
                        step.start = at;
                        if (!route.steps.isEmpty()) {
                            route.steps.get(route.steps.size() - 1).end = at;
                        }
                    }
                    route.steps.add(step);
                }
            }
        }
        // Every remaining open step end (incl. the last) closes at the destination.
        if (!route.path.isEmpty()) {
            LatLng dest = route.path.get(route.path.size() - 1);
            for (NavigationStep step : route.steps) {
                if (step.end == null) step.end = dest;
            }
        }
        return route;
    }

    /**
     * Flattens per-segment legal speed limits (km/h) from leg annotations.
     * Indexes align with the decoded geometry segments; empty when the
     * backend doesn't return maxspeed data.
     */
    private ArrayList<Double> parseSpeedLimits(DirectionsRoute dto) {
        ArrayList<Double> limits = new ArrayList<>();
        try {
            if (dto.legs() == null) return limits;
            for (RouteLeg leg : dto.legs()) {
                LegAnnotation annotation = leg.annotation();
                if (annotation == null || annotation.maxspeed() == null) continue;
                for (MaxSpeed max : annotation.maxspeed()) {
                    double kmh = 0;
                    if (max != null && max.speed() != null
                            && !Boolean.TRUE.equals(max.unknown())
                            && !Boolean.TRUE.equals(max.none())) {
                        kmh = max.speed();
                        if ("mph".equalsIgnoreCase(max.unit())) kmh *= 1.60934;
                    }
                    limits.add(kmh);
                }
            }
        } catch (Exception ignored) {
            // Annotation shape varies between resources; treat as unavailable.
            limits.clear();
        }
        return limits;
    }

    /** Human/voice-friendly text from an OSRM-style maneuver. */
    private String buildInstruction(LegStep step) {
        StepManeuver m = step.maneuver();
        String name = step.name() != null && !step.name().isEmpty() ? step.name() : null;
        if (m == null || m.type() == null) {
            return name != null ? "Continue on " + name : "Continue";
        }
        String modifier = m.modifier() != null ? m.modifier() : "";
        String onto = name != null ? " onto " + name : "";
        switch (m.type()) {
            case "depart":
                return "Head out" + (name != null ? " on " + name : "");
            case "arrive":
                return "You have arrived at your destination";
            case "turn":
            case "end of road":
                return capitalize("turn " + modifier + onto);
            case "fork":
                return capitalize("keep " + modifier + onto);
            case "merge":
                return capitalize("merge " + modifier + onto);
            case "on ramp":
                return "Take the ramp" + onto;
            case "off ramp":
                return capitalize("take the exit " + modifier + onto);
            case "roundabout":
            case "rotary":
                return "Enter the roundabout" + onto;
            case "exit roundabout":
                return "Exit the roundabout" + onto;
            case "new name":
            case "continue":
            default:
                if (modifier.contains("left") || modifier.contains("right")) {
                    return capitalize("continue " + modifier + onto);
                }
                return name != null ? "Continue on " + name : "Continue straight";
        }
    }

    private String capitalize(String s) {
        s = s.trim().replaceAll("\\s+", " ");
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

