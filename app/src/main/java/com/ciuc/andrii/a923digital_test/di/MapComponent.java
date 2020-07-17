package com.ciuc.andrii.a923digital_test.di;

import com.ciuc.andrii.a923digital_test.di.module.NavigationManagerModule;
import com.ciuc.andrii.a923digital_test.di.module.RouteOptionsModule;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {RouteOptionsModule.class, NavigationManagerModule.class})
public interface MapComponent {
    RouteOptionsModule getRouteOptionsModule();
    NavigationManagerModule getNavigationManagerModule();
}