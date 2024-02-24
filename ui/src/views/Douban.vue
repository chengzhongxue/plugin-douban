<script lang="ts" setup>
import {
  VCard,
  IconRefreshLine,
  Dialog,
  VButton,
  VEmpty,
  VLoading,
  VPagination,
  VPageHeader,
  VDropdownItem,
  Toast,
  VSpace,
  IconAddCircle,
  IconCloseCircle} from "@halo-dev/components";
import {useQuery, useQueryClient} from "@tanstack/vue-query";
import {computed, ref, watch} from "vue";
import apiClient from "@/utils/api-client";
import {formatDatetime} from "@/utils/date";
import type {DoubanMovie, FriendDoubanMovieList} from "@/types";
import {useRouteQuery} from "@vueuse/router";
import DoubanMovieEditingModal from "../components/DoubanMovieEditingModal.vue";

const queryClient = useQueryClient();

const selecteDoubanMovie = ref<DoubanMovie | undefined>();
const selecteDoubanMovies = ref<string[]>([]);
const checkedAll = ref(false);
const selectedSort = useRouteQuery<string | undefined>("sort");
const selectedStatus = useRouteQuery<string | undefined>("status");
const selectedDataType = useRouteQuery<string | undefined>("dataType");
const selectedType = useRouteQuery<string | undefined>("type");


const page = ref(1);
const size = ref(20);
const keyword = ref("");
const searchText = ref("");
const total = ref(0);
const editingModal = ref(false);


watch(
  () => [
    selectedSort.value,
    selectedStatus.value,
    selectedDataType.value,
    selectedType.value,
    keyword.value,
  ],
  () => {
    page.value = 1;
  }
);

function handleClearFilters() {
  selectedSort.value = undefined;
  selectedStatus.value = undefined;
  selectedType.value = undefined;
  selectedDataType.value = undefined;
}

const hasFilters = computed(() => {
  return (
    selectedSort.value ||
    selectedStatus.value ||
    selectedType.value ||
    selectedDataType.value
  );
});

const {
  data: doubanMovies,
  isLoading,
  isFetching,
  refetch,
} = useQuery({
  queryKey: ["doubanMovies", page, size,selectedSort,selectedDataType,selectedStatus,selectedType,keyword],
  queryFn: async () => {
    
    const { data } = await apiClient.get<FriendDoubanMovieList>(
      "/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-douban/doubanmovies",
      {
        params: {
          page: page.value,
          size: size.value,
          sort: selectedSort.value,
          dataType: selectedDataType.value,
          status: selectedStatus.value,
          type : selectedType.value,
          keyword: keyword?.value
        },
      }
    );
    total.value = data.total;
    return data.items;
  },
  refetchInterval: (data) =>  {
    const deletingFriend = data?.filter(
      (douban) => !!douban.metadata.deletionTimestamp
    );
    return deletingFriend?.length ? 1000 : false;
  },
});


const handleCheckAllChange = (e: Event) => {
  const { checked } = e.target as HTMLInputElement;
  checkedAll.value = checked;
  if (checkedAll.value) {
    selecteDoubanMovies.value =
      doubanMovies.value?.map((doubanMovie) => {
        return doubanMovie.metadata.name;
      }) || [];
  } else {
    selecteDoubanMovies.value.length = 0;
  }
};

const handleDeleteInBatch = () => {
  Dialog.warning({
    title: "是否确认删除所选的条目？",
    description: "删除之后将无法恢复。",
    confirmType: "danger",
    onConfirm: async () => {
      try {
        const promises = selecteDoubanMovies.value.map((doubanMovie) => {
          return apiClient.delete(`/apis/douban.moony.la/v1alpha1/doubanmovies/${doubanMovie}`);
        });
        if (promises) {
          await Promise.all(promises);
        }
        selecteDoubanMovies.value.length = 0;
        checkedAll.value = false;

        Toast.success("删除成功");
      } catch (e) {
        console.error(e);
      } finally {
        queryClient.invalidateQueries({ queryKey: ["doubanMovies"] });
      }
    },
  });
};
function handleReset() {
  keyword.value = "";
  searchText.value = "";
}
function onKeywordChange() {
  keyword.value = searchText.value;
}

const synchronization = () => {
  Dialog.warning({
    title: "同步豆瓣数据",
    description: "点击按钮后，后台将进行同步豆瓣数据。",
    confirmType: "danger",
    confirmText: "确定",
    cancelText: "取消",
    onConfirm: async () => {
      try {
        await apiClient.post("/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-douban/douban/synchronizationDouban")
          .then((res: any) => {
            Toast.success("同步豆瓣数据成功");
          });
      } catch (e) {
        console.error("", e);
      }
    },
  });
}

const getStatusText = (status: String) => {
  if (status == 'done'){
    return '已看';
  }else if (status == 'mark'){
    return '想看';
  }else if (status == 'doing'){
    return '在看';
  }
};

function handleClear() {
  Dialog.warning({
    title: "清空记录",
    description: "确定要清空所有推送记录吗？此操作不可恢复。",
    async onConfirm() {
      try {
        await apiClient.delete("/apis/api.plugin.halo.run/v1alpha1/plugins/plugin-douban/douban/clear")
          .then((res: any) => {
            Toast.success("清空成功");
          });
      } catch (e) {
        console.error("", e);
      }
      refetch();
    },
  });
}

const handleOpenCreateModal = (doubanMovie: DoubanMovie) => {
  selecteDoubanMovie.value = doubanMovie;
  editingModal.value = true;
};

const onEditingModalClose = async () => {
  selecteDoubanMovie.value = undefined;
  refetch();
};

</script>

<template>
  <DoubanMovieEditingModal
    v-model:visible="editingModal"
    :doubanMovie="selecteDoubanMovie"
    @close="onEditingModalClose"
  >
  </DoubanMovieEditingModal>
  <VPageHeader title="豆瓣">
    <template #actions>
      <VSpace v-permission="['plugin:douban:manage']">
        <VButton :route="{ name: 'DoubanCron' }" size="sm">
          任务
        </VButton>
        <VButton
          type="secondary"
          @click="editingModal = true"
        >
          <template #icon>
            <IconAddCircle class="h-full w-full" />
          </template>
          新建
        </VButton>
        <VButton type="secondary" @click="synchronization">
          同步数据
        </VButton>
        <VButton type="danger" @click="handleClear()"> 清空 </VButton>
      </VSpace>
    </template>

  </VPageHeader>
  <div class="m-0 md:m-4">
    <VCard :body-class="['!p-0']">
      <template #header>
        <div class="block w-full bg-gray-50 px-4 py-3">
          <div class="relative flex flex-col flex-wrap items-start gap-4 sm:flex-row sm:items-center" >
            <div class="hidden items-center sm:flex" >
              <input
                v-model="checkedAll"
                type="checkbox"
                @change="handleCheckAllChange"
              />
            </div>
            <div class="flex w-full flex-1 items-center sm:w-auto" >
              <FormKit
                v-if="!selecteDoubanMovies.length"
                v-model="searchText"
                placeholder="输入关键词搜索"
                type="text"
                outer-class="!moments-p-0 moments-mr-2"
                @keyup.enter="onKeywordChange"
              >
                <template v-if="keyword" #suffix>
                  <div
                    class="group flex h-full cursor-pointer items-center bg-white px-2 transition-all hover:bg-gray-50"
                    @click="handleReset"
                  >
                    <IconCloseCircle
                      class="h-4 w-4 text-gray-500 group-hover:text-gray-700"
                    />
                  </div>
                </template>
              </FormKit>
              <VSpace v-else v-permission="['plugin:douban:manage']">
                <VButton type="danger" @click="handleDeleteInBatch">
                  删除
                </VButton>
              </VSpace>
            </div>
            <VSpace spacing="lg" class="flex-wrap">
              <FilterCleanButton
                v-if="hasFilters"
                @click="handleClearFilters"
              />
              <FilterDropdown
                v-model="selectedDataType"
                label="数据类型"
                :items="[
                    {
                      label: '全部',
                      value: undefined,
                    },
                    {
                      label: '豆瓣',
                      value: 'db',
                    },
                    {
                      label: 'TMDB',
                      value: 'tmdb',
                    },
                    {
                      label: '手动添加',
                      value: 'halo',
                    },
                  ]"
              />
              <FilterDropdown
                v-model="selectedStatus"
                label="状态"
                :items="[
                    {
                      label: '全部',
                      value: undefined,
                    },
                    {
                      label: '已看',
                      value: 'done',
                    },
                    {
                      label: '想看',
                      value: 'mark',
                    },
                    {
                      label: '在看',
                      value: 'doing',
                    },
                  ]"
              />
              <FilterDropdown
                v-model="selectedType"
                label="类型"
                :items="[
                    {
                      label: '全部',
                      value: undefined,
                    },
                    {
                      label: '电影',
                      value: 'movie',
                    },
                    {
                      label: '图书',
                      value: 'book',
                    },
                    {
                      label: '音乐',
                      value: 'music',
                    },
                    {
                      label: '游戏',
                      value: 'game',
                    },
                    {
                      label: '舞台剧',
                      value: 'drama',
                    },
                  ]"
              />
              <FilterDropdown
                v-model="selectedSort"
                label="排序"
                :items="[
                      {
                        label: '默认',
                      },
                      {
                        label: '较近创建',
                        value: 'createTime,desc',
                      },
                      {
                        label: '较早创建',
                        value: 'createTime,asc',
                      },
                    ]"
              />
              <div class="flex flex-row gap-2">
                <div
                  class="group cursor-pointer rounded p-1 hover:bg-gray-200"
                  @click="refetch()"
                >
                  <IconRefreshLine
                    v-tooltip="'刷新'"
                    :class="{ 'animate-spin text-gray-900': isFetching }"
                    class="h-4 w-4 text-gray-600 group-hover:text-gray-900"
                  />
                </div>
              </div>
            </VSpace>
          </div>
        </div>
      </template>
      <VLoading v-if="isLoading" />

      <Transition v-else-if="!doubanMovies?.length" appear name="fade">
        <VEmpty
          message="暂无订阅文章记录"
          title="暂无订阅文章记录"
        >
          <template #actions>
            <VSpace>
              <VButton @click="refetch()"> 刷新 </VButton>
            </VSpace>
          </template>
        </VEmpty>
      </Transition>

      <Transition v-else appear name="fade">
        <div class="w-full relative overflow-x-auto">
          <table class="w-full  text-sm text-left text-gray-500 widefat ">
            <thead class="text-xs text-gray-700 uppercase bg-gray-50">
            <tr>
              <th scope="col" class="px-4 py-3"><div class="w-max flex items-center"> </div></th>
              <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">标题 </div></th>
              <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">封面 </div></th>
              <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">来源 </div></th>
              <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">评分 </div></th>
              <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">描述 </div></th>
              <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">时间 </div></th>
              <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">状态 </div></th>
              <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">我的短评 </div></th>
              <th scope="col" class="px-4 py-3"><div class="w-max flex items-center">我的评分 </div></th>
              <th scope="col" class="px-4 py-3"><div class="w-max flex items-center"> </div></th>
            </tr>
            </thead>
            <tbody>
            <tr v-for="doubanMovie in doubanMovies" class="border-b last:border-none hover:bg-gray-100">
              <td class="px-4 py-4 ">
                <input
                  v-model="selecteDoubanMovies"
                  :value="doubanMovie.metadata.name"
                  class="h-4 w-4 rounded border-gray-300 text-indigo-600"
                  name="post-checkbox"
                  type="checkbox"
                />
              </td>
              <td class="px-4 py-4">{{doubanMovie.spec.name}}</td>
              <td class="px-4 py-4 poster">
                <img :src="doubanMovie.spec.poster"  referrerpolicy="no-referrer">
              </td>
              <td class="px-4 py-4 table-td">{{doubanMovie.spec.dataType == 'db' ? '豆瓣' : doubanMovie.spec.dataType == 'tmdb'  ? 'TMDB' : '手动添加'}}</td>
              <td class="px-4 py-4 table-td">{{doubanMovie.spec.score}}</td>
              <td class="px-4 py-4">{{doubanMovie.spec.cardSubtitle}}</td>
              <td class="px-4 py-4 table-td">{{formatDatetime(doubanMovie.faves.createTime)}}</td>
              <td class="px-4 py-4 table-td">{{getStatusText(doubanMovie.faves.status)}}</td>
              <td class="px-4 py-4">{{doubanMovie.faves.remark}}</td>
              <td class="px-4 py-4">{{doubanMovie.faves.score}}</td>
              <td class="px-4 py-4 table-td">
                <VDropdownItem v-permission="['plugin:douban:manage']" @click="handleOpenCreateModal(doubanMovie)">
                  编辑
                </VDropdownItem>
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </Transition>

      <template #footer>
        <VPagination
          v-model:page="page"
          v-model:size="size"
          :total="total"
          :size-options="[20, 30, 50, 100]"
        />
      </template>
    </VCard>
  </div>


</template>

<style scoped lang="scss">

.widefat * {
  word-wrap: break-word;
}
.widefat td {
  vertical-align: top;
}

.widefat .poster{
  width: 180px;
}

.table-td {
  text-align: left !important;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}


</style>
