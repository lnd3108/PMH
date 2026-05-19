import { provideTaiga, tuiNotificationOptionsProvider } from '@taiga-ui/core';
import {
  ApplicationConfig,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './interceptors/auth.interceptor';
import { TUI_DEFAULT_LANGUAGE, TUI_VIETNAMESE_LANGUAGE, tuiLanguageSwitcher } from '@taiga-ui/i18n';

export const appConfig: ApplicationConfig = {
  providers: [
    {
      provide: TUI_DEFAULT_LANGUAGE,
      useValue: TUI_VIETNAMESE_LANGUAGE,
    },
    tuiLanguageSwitcher((language) => {
      switch (language) {
        case 'vietnamese':
          return import('@taiga-ui/i18n/languages/vietnamese');
        case 'english':
          return import('@taiga-ui/i18n/languages/english');
        default:
          return import('@taiga-ui/i18n/languages/vietnamese');
      }
    }),
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),

    provideHttpClient(withFetch(), withInterceptors([authInterceptor])),

    provideTaiga(),
    tuiNotificationOptionsProvider({
      block: 'end',
      inline: 'start',
      autoClose: 3000,
      closable: true,
      size: 's',
    }),
  ],
};
