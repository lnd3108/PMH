import { DOCUMENT } from '@angular/common';
import { mergeApplicationConfig, ApplicationConfig } from '@angular/core';
import { provideServerRendering, withRoutes } from '@angular/ssr';
import { WA_ANIMATION_FRAME, WA_WINDOW } from '@ng-web-apis/common';
import { EMPTY } from 'rxjs';
import { appConfig } from './app.config';
import { serverRoutes } from './app.routes.server';

const matchMedia = (query: string): MediaQueryList =>
  ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => undefined,
    removeListener: () => undefined,
    addEventListener: () => undefined,
    removeEventListener: () => undefined,
    dispatchEvent: () => false,
  }) as MediaQueryList;

const serverConfig: ApplicationConfig = {
  providers: [
    provideServerRendering(withRoutes(serverRoutes)),
    {
      provide: WA_ANIMATION_FRAME,
      useValue: EMPTY,
    },
    {
      provide: WA_WINDOW,
      deps: [DOCUMENT],
      useFactory: (document: Document): Window => {
        const win = document.defaultView as Window & {
          matchMedia?: Window['matchMedia'];
        };

        if (!win.matchMedia) {
          win.matchMedia = matchMedia;
        }

        win.requestAnimationFrame ??= (callback: FrameRequestCallback): number =>
          setTimeout(() => callback(Date.now()), 16) as unknown as number;
        win.cancelAnimationFrame ??= (id: number): void => {
          clearTimeout(id);
        };

        return win;
      },
    },
  ]
};

export const config = mergeApplicationConfig(appConfig, serverConfig);
