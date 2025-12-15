/// <reference types="@rsbuild/core/types" />
/// <reference types="unplugin-icons/types/vue" />
export {};

declare module "*.vue" {
  import type { ComponentOptions } from "vue";
  const Component: ComponentOptions;
  export default Component;
}

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
