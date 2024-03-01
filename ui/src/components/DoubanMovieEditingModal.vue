<script lang="ts" setup>
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { ref, computed, nextTick, watch} from "vue";
import type { DoubanMovie } from "@/types";
import apiClient from "@/utils/api-client";
import cloneDeep from "lodash.clonedeep";
import {toDatetimeLocal, toISOString} from "@/utils/date";

const props = withDefaults(
  defineProps<{
    visible: boolean;
    doubanMovie?: DoubanMovie;
  }>(),
  {
    visible: false,
    doubanMovie: undefined,
  }
);

const emit = defineEmits<{
  (event: "update:visible", value: boolean): void;
  (event: "close"): void;
}>();

const initialFormState: DoubanMovie = {
  metadata: {
    name: "",
    generateName: "douban-movie-",
  },
  faves:{
    remark :"",
    createTime: "",
    score: "",
    status: "done"
  },
  spec: {
    name: "",
    poster: "",
    link: "",
    id: null,
    score: "",
    year: "",
    type: "",
    pubdate: "",
    cardSubtitle: "",
    dataType: "halo",
    genres: []
  },
  kind: "DoubanMovie",
  apiVersion: "douban.moony.la/v1alpha1",
};

const formState = ref<DoubanMovie>(cloneDeep(initialFormState));
const saving = ref<boolean>(false);
const formVisible = ref(false);


const isUpdateMode = computed(() => {
  return !!formState.value.metadata.creationTimestamp;
});

const modalTitle = computed(() => {
  return isUpdateMode.value ? "编辑条目" : "新建条目";
});

const onVisibleChange = (visible: boolean) => {
  emit("update:visible", visible);
  if (!visible) {
    emit("close");
  }
};

const handleResetForm = () => {
  formState.value = cloneDeep(initialFormState);
};

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      formVisible.value = true;
    } else {
      setTimeout(() => {
        formVisible.value = false;
        handleResetForm();
      }, 200);
    }
  }
);

watch(
  () => props.doubanMovie,
  (doubanMovie) => {
    if (doubanMovie) {
      formState.value = cloneDeep(doubanMovie);
      formState.value.faves.createTime = formState.value.faves.createTime 
        ? toDatetimeLocal(formState.value.faves.createTime) : undefined
    }
  }
);

const annotationsFormRef = ref();

const handleSaveFriend = async () => {
  annotationsFormRef.value?.handleSubmit();
  await nextTick();

  const { customAnnotations, annotations, customFormInvalid, specFormInvalid } =
  annotationsFormRef.value || {};
  if (customFormInvalid || specFormInvalid) {
    return;
  }

  formState.value.metadata.annotations = {
    ...annotations,
    ...customAnnotations,
  };

  formState.value.faves.createTime = formState.value.faves.createTime 
    ? toISOString(formState.value.faves.createTime) : undefined

  try {
    saving.value = true;
    if (isUpdateMode.value) {
      await apiClient.put<DoubanMovie>(
        `/apis/douban.moony.la/v1alpha1/doubanmovies/${formState.value.metadata.name}`,
        formState.value
      );
    } else {
      await apiClient.post<DoubanMovie>(
        `/apis/douban.moony.la/v1alpha1/doubanmovies`,
        formState.value
      );
    }

    Toast.success("保存成功");

    onVisibleChange(false);
  } catch (e) {
    console.error(e);
  } finally {
    saving.value = false;
  }
};
</script>
<template>
  <VModal
    :title="modalTitle"
    :visible="visible"
    :width="650"
    @update:visible="onVisibleChange"
  >
    <template #actions>
      <slot name="append-actions" />
    </template>

    <FormKit
      v-if="formVisible"
      id="doubanMovie-form"
      name="doubanMovie-form"
      type="form"
      :config="{ validationVisibility: 'submit' }"
      @submit="handleSaveFriend"
    >
      <div class="md:grid md:grid-cols-4 md:gap-6">
        <div class="md:col-span-1">
          <div class="sticky top-0">
            <span class="text-base font-medium text-gray-900"> 常规 </span>
          </div>
        </div>
        <div class="mt-5 divide-y divide-gray-100 md:col-span-3 md:mt-0">
          <td v-if="isUpdateMode">
            <p><img :src="formState.spec.poster" width="100"></p>
            <p>{{formState.spec.name}} <span class="db--titletag">{{formState.spec.dataType == 'db' ? '豆瓣' : formState.spec.dataType == 'tmdb'  ? 'TMDB' : '手动添加'}}</span>
            </p>
            <p>{{formState.spec.cardSubtitle}}</p>
          </td>
          <FormKit
            v-if="formState.spec.dataType=='halo'"
            type="attachment"
            v-model="formState.spec.poster"
            name="poster"
            validation="required"
            label="封面"
          ></FormKit>
          <FormKit
            v-if="formState.spec.dataType=='halo'"
            type="text"
            v-model="formState.spec.name"
            name="name"
            validation="required"
            label="标题"
          ></FormKit>
          <FormKit
            type="text"
            v-model="formState.spec.link"
            name="link"
            validation="required"
            label="链接"
          ></FormKit>
          <FormKit
            v-if="formState.spec.dataType=='halo'"
            type="number"
            v-model="formState.spec.score"
            name="score"
            validation="required"
            label="评分"
            max="10"
            min="0"
          ></FormKit>
          <FormKit
            v-if="formState.spec.dataType=='halo'"
            :options="[
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
            label="类型"
            v-model="formState.spec.type"
            name="type"
            type="select"
          ></FormKit>
          <FormKit
            v-if="formState.spec.dataType=='halo'"
            type="textarea"
            v-model="formState.spec.cardSubtitle"
            name="cardSubtitle"
            label="描述"
            :rows="4"
            validation="required|length:0,300"
          ></FormKit>
          <FormKit
            type="datetime-local"
            v-model="formState.faves.createTime"
            name="createTime"
            validation="required"
            label="观看时间"
          ></FormKit>
          <FormKit
            :options="[
              { label: '已看', value: 'done' },
              { label: '想看', value: 'mark' },
              { label: '在看', value: 'doing' },
            ]"
            label="状态"
            v-model="formState.faves.status"
            name="status"
            type="select"
          ></FormKit>
          <FormKit
            type="textarea"
            v-model="formState.faves.remark"
            name="remark"
            label="我的短评"
            :rows="4"
            validation="length:0,300"
          ></FormKit>
          <FormKit
            type="number"
            v-model="formState.faves.score"
            name="score"
            label="我的评分"
            max="5"
            min="0"
          ></FormKit>
        </div>
      </div>
    </FormKit>

    <template #footer>
      <VSpace>
        <VButton
          :loading="saving"
          type="secondary"
          @click="$formkit.submit('doubanMovie-form')"
        >
          提交
        </VButton>
        <VButton @click="onVisibleChange(false)">取消</VButton>
      </VSpace>
    </template>
  </VModal>
</template>

<style scoped lang="scss">

.db--titletag {
  font-size: 13px;
  display: inline-block;
  color: #fff;
  background-color: green;
  border-radius: 2px;
  line-height: 1;
  padding: 2px 3px;
  margin-left: 4px;
}

divide-y td {
  margin-bottom: 9px;
  line-height: 1.3;
  padding-bottom: 1rem;
}

.divide-y td p {
  margin-bottom: 6px;
}

</style>
