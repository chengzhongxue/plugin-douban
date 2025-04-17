/// <reference types="vite/client" />

declare module "axios" {
  export interface AxiosRequestConfig {
    mute?: boolean;
  }
}

declare module "vue" {
  interface ComponentCustomProperties {
    $formkit: any;
  }
}
