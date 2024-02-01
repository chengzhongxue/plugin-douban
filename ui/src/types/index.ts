export interface Metadata {
  name: string;
  generateName?: string;
  labels?: {
    [key: string]: string;
  } | null;
  annotations?: {
    [key: string]: string;
  } | null;
  version?: number | null;
  creationTimestamp?: string | null;
  deletionTimestamp?: string | null;
}


export interface FriendDoubanMovieList {
  page: number;
  size: number;
  total: number;
  items: Array<DoubanMovie>;
  first: boolean;
  last: boolean;
  hasNext: boolean;
  hasPrevious: boolean;
  totalPages: number;
}

export interface DoubanMovie {
  spec: DoubanMovieSpec;
  faves: DoubanMovieFaves;
  apiVersion: string;
  kind: string;
  metadata: Metadata;
}

export interface DoubanMovieSpec {
  name: string;
  poster: string;
  link: string;
  doubanId?: number | null;
  doubanScore: string;
  year: string;
  type: string;
  pubdate: string;
  cardSubtitle: string;
  tmdbId?: number | null;
  tmdbType: string;
  genres: string[];
}

export interface DoubanMovieFaves {

  remark?: string;
  createTime?: string | null;
  score?: string;
  status?: string;
  
}

