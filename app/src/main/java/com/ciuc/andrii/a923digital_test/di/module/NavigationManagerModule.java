package com.ciuc.andrii.a923digital_test.di.module;

import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteOptions;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import dagger.Module;
import dagger.Provides;

@Module
public class NavigationManagerModule {

    @Inject
    public NavigationManager navigationManager;

    //todo Navigation listeners
    private NavigationManager.NewInstructionEventListener instructListener = new NavigationManager.NewInstructionEventListener() {
        public void onNewInstructionEvent() {
            // Interpret and present the Maneuver object as it contains
            // turn by turn navigation instructions for the user.
            navigationManager.getNextManeuver();
        }
    };

    private NavigationManager.PositionListener positionListener = new NavigationManager.PositionListener() {
        public void onPositionUpdated(GeoPosition loc) {
            // the position we get in this callback can be used
            // to reposition the map and change orientation.

            // also remaining time and distance can be
            // fetched from navigation manager
            navigationManager.getTta(Route.TrafficPenaltyMode.DISABLED, true);
            navigationManager.getDestinationDistance();
        }
    };


    @Inject
    public NavigationManagerModule(NavigationManager navigationManager) {
        this.navigationManager = navigationManager;

        // start listening for navigation events
        navigationManager.addNewInstructionEventListener(
                new WeakReference<>(instructListener));

        // start listening for position events
        navigationManager.addPositionListener(
                new WeakReference<>(positionListener));
    }

    @Provides
    public NavigationManager providesNavigationManager() {
        return navigationManager;
    }


}
