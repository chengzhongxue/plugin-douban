<script lang="ts" setup>
import { Toast, VButton, VModal, VSpace } from "@halo-dev/components";
import { ref, computed, nextTick, watch, useTemplateRef} from "vue";
import cloneDeep from "lodash.clonedeep";
import {doubanCoreApiClient} from "@/api";
import type { DoubanMovie } from "@/api/generated";
import {toDatetimeLocal, toISOString} from "@/utils/date";

const props = withDefaults(
  defineProps<{
    doubanMovie?: DoubanMovie;
  }>(),
  {
    doubanMovie: undefined,
  }
);

const emit = defineEmits<{
  (event: "close"): void;
}>();

const initialFormState: DoubanMovie = {
  metadata: {
    name: "",
    generateName: "douban-movie-",
  },
  faves:{
    remark :"",
    createTime: undefined,
    score: "",
    status: "done"
  },
  spec: {
    name: "",
    poster: "",
    link: "",
    id: "",
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
const createTime = ref<string | undefined>(undefined);
const modal = useTemplateRef<InstanceType<typeof VModal> | null>("modal");

const isUpdateMode = computed(() => {
  return !!formState.value.metadata.creationTimestamp;
});

const modalTitle = computed(() => {
  return isUpdateMode.value ? "编辑条目" : "新建条目";
});


watch(
  () => props.doubanMovie,
  (doubanMovie) => {
    if (doubanMovie) {
      formState.value = cloneDeep(doubanMovie);
      createTime.value = toDatetimeLocal(formState.value.faves.createTime);
    }else {
      createTime.value = undefined;
    }
  },
  {
    immediate: true,
  }
);

watch(
  () => createTime.value,
  (value) => {
    formState.value.faves.createTime = value ? toISOString(value) : undefined;
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
  
  try {
    saving.value = true;
    if (isUpdateMode.value) {
      await doubanCoreApiClient.doubanMovie.updateDoubanMovie({
          name: formState.value.metadata.name,
          doubanMovie: formState.value
      });
    } else {
      await doubanCoreApiClient.doubanMovie.createDoubanMovie(
        {
          doubanMovie: formState.value
        });
    }

    Toast.success("保存成功");

    modal.value?.close();
  } catch (e) {
    console.error(e);
  } finally {
    saving.value = false;
  }
};
</script>
<template>
  <VModal
    ref="modal"
    :title="modalTitle"
    :width="650"
    @close="emit('close')"
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
      <div class=":uno: md:grid md:grid-cols-4 md:gap-6">
        <div class=":uno: md:col-span-1">
          <div class=":uno: sticky top-0">
            <span class=":uno: text-base font-medium text-gray-900"> 常规 </span>
          </div>
        </div>
        <div class=":uno: mt-5 divide-y divide-gray-100 md:col-span-3 md:mt-0">
          <td v-if="isUpdateMode">
            <p><img :src="formState.spec.poster" width="100"></p>
            <p>{{formState.spec.name}} <span class=":uno: db--titletag">{{formState.spec.dataType == 'db' ? '豆瓣' : formState.spec.dataType == 'tmdb'  ? 'TMDB' : '手动添加'}}</span>
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
            min="0000-01-01T00:00"
            max="9999-12-31T23:59"
            v-model="createTime"
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
        <VButton @click="modal?.close()">取消</VButton>
      </VSpace>
    </template>
  </VModal>
</template>

<style lang="scss">

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
