import { axiosInstance } from "@halo-dev/api-client";
import { ApiDoubanMoonyLaV1alpha1DoubanMovieApi, DoubanMovieV1alpha1Api, CronDoubanV1alpha1Api } from "./generated";

const doubanCoreApiClient = {
  doubanMovie: new DoubanMovieV1alpha1Api(undefined, "", axiosInstance),
  cronDouban: new CronDoubanV1alpha1Api(undefined, "", axiosInstance),
};

const doubanApiClient = {
  doubanMovie: new ApiDoubanMoonyLaV1alpha1DoubanMovieApi(undefined, "", axiosInstance),
};


export { doubanCoreApiClient, doubanApiClient };
