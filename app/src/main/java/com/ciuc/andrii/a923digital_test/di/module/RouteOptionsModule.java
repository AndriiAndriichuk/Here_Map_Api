package com.ciuc.andrii.a923digital_test.di.module;

import com.here.android.mpa.routing.RouteOptions;

import javax.inject.Inject;

import dagger.Module;
import dagger.Provides;

@Module
public class RouteOptionsModule {

    @Inject
    public RouteOptions routeOptions;

    @Inject
    public RouteOptionsModule(RouteOptions routeOptions) {
        this.routeOptions = routeOptions;
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setHighwaysAllowed(false);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routeOptions.setRouteCount(1);
    }

    @Provides
    public RouteOptions providesRouteOptions() {
        return routeOptions;
    }


}
