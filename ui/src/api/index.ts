import { axiosInstance } from "@halo-dev/api-client";
import {
  ApiDoubanMoonyLaV1alpha1DoubanMovieApi,
  CronDoubanV1alpha1Api, 
  ConsoleApiDoubanMoonyLaV1alpha1DoubanMovieApi
} from './generated'

const doubanCoreApiClient = {
  cronDouban: new CronDoubanV1alpha1Api(undefined, "", axiosInstance),
};

const doubanApiClient = {
  doubanMovie: new ConsoleApiDoubanMoonyLaV1alpha1DoubanMovieApi(undefined, "", axiosInstance),
};

const doubanQueryApiClient = {
  doubanMovie: new ApiDoubanMoonyLaV1alpha1DoubanMovieApi(undefined, "", axiosInstance),
}


export { doubanCoreApiClient, doubanApiClient, doubanQueryApiClient };
